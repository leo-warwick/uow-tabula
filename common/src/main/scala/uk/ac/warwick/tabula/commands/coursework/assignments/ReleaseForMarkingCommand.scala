package uk.ac.warwick.tabula.commands.coursework.assignments

import org.joda.time.DateTime
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.coursework.{FeedbackReleasedNotifier, ReleasedState}
import uk.ac.warwick.tabula.data.model.notifications.coursework.ReleaseToMarkerNotification
import uk.ac.warwick.tabula.data.model.{Module, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import collection.JavaConverters._
import scala.collection.mutable

object ReleaseForMarkingCommand {
	def apply(module: Module, assignment: Assignment, user: User) =
		new ReleaseForMarkingCommand(module, assignment, user)
			with ComposableCommand[List[Feedback]]
			with ReleaseForMarkingCommandPermissions
			with ReleaseForMarkingCommandDescription
			with FirstMarkerReleaseNotifier
			with AutowiringAssessmentServiceComponent
			with AutowiringStateServiceComponent
			with AutowiringFeedbackServiceComponent
			with AutowiringUserLookupComponent
}


abstract class ReleaseForMarkingCommand(val module: Module, val assignment: Assignment, val user: User)
	extends CommandInternal[List[Feedback]] with Appliable[List[Feedback]] with ReleaseForMarkingState with ReleasedState with BindListener
	with SelfValidating with UserAware {

	self: AssessmentServiceComponent with StateServiceComponent with FeedbackServiceComponent with UserLookupComponent =>

	// we must go via the marking workflow directly to determine if the student has a marker - not all workflows use the markerMap on assignment
	def studentsWithKnownMarkers: Seq[String] = students.asScala.filter(assignment.markingWorkflow.studentHasMarker(assignment, _))
	def unreleasableSubmissions: Seq[String] = (studentsWithoutKnownMarkers ++ studentsAlreadyReleased).distinct

	def studentsWithoutKnownMarkers:Seq[String] = students.asScala -- studentsWithKnownMarkers
	def studentsAlreadyReleased: mutable.Buffer[String] = invalidFeedback.asScala.map(f => f.usercode)

	override def applyInternal(): List[AssignmentFeedback] = {

		val studentUsers = userLookup.getUsersByUserIds(students.asScala)

		// get the parent feedback or create one if none exist
		val feedbacks = studentsWithKnownMarkers.toBuffer.map { usercode: String =>
			val student = studentUsers(usercode)
			val parentFeedback = assignment.feedbacks.asScala.find(_.usercode == usercode).getOrElse({
				val newFeedback = new AssignmentFeedback
				newFeedback.assignment = assignment
				newFeedback.uploaderId = user.getUserId
				newFeedback.usercode = student.getUserId
				newFeedback._universityId = student.getWarwickId
				newFeedback.released = false
				newFeedback.createdDate = DateTime.now
				feedbackService.saveOrUpdate(newFeedback)
				newFeedback
			})
			parentFeedback.updatedDate = DateTime.now
			parentFeedback
		}

		val feedbackToUpdate: Seq[AssignmentFeedback] = feedbacks -- invalidFeedback.asScala

		newReleasedFeedback.clear()
		newReleasedFeedback.addAll(feedbackToUpdate.map(f => {
			val markerFeedback = new MarkerFeedback(f)
			f.firstMarkerFeedback = markerFeedback
			stateService.updateState(markerFeedback, MarkingState.ReleasedForMarking)
			markerFeedback
		}).asJava)

		feedbacksUpdated = feedbackToUpdate.size
		feedbackToUpdate.toList
	}

	override def onBind(result: BindingResult) {
		invalidFeedback.addAll((for {
			usercode <- students.asScala
			parentFeedback <- assignment.feedbacks.asScala.find(_.usercode == usercode)
			if parentFeedback.firstMarkerFeedback != null
		} yield parentFeedback).asJava)
	}

	override def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "submission.release.for.marking.confirm")
	}

}

trait ReleaseForMarkingState {
	import uk.ac.warwick.tabula.JavaImports._

	val assignment: Assignment
	val module: Module

	var students: JList[String] = JArrayList()
	var confirm: Boolean = false
	var invalidFeedback: JList[AssignmentFeedback] = JArrayList()

	var feedbacksUpdated = 0
}

trait ReleaseForMarkingCommandPermissions extends RequiresPermissionsChecking {
	self: ReleaseForMarkingState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Submission.ReleaseForMarking, assignment)
	}
}

trait ReleaseForMarkingCommandDescription extends Describable[List[Feedback]] {

	self: ReleaseForMarkingState =>

	override def describe(d: Description){
		d.assignment(assignment)
			.property("students" -> students)
	}

	override def describeResult(d: Description){
		d.assignment(assignment)
			.property("submissionCount" -> feedbacksUpdated)
	}
}

trait FirstMarkerReleaseNotifier extends FeedbackReleasedNotifier[List[Feedback]] {
	this: ReleaseForMarkingState with ReleasedState with UserAware with UserLookupComponent with Logging =>
	def blankNotification = new ReleaseToMarkerNotification(1)
}