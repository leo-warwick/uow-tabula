package uk.ac.warwick.tabula.commands.coursework.feedback

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.MarkingState.{MarkingCompleted, Rejected}
import uk.ac.warwick.tabula.helpers.StringUtils._
import scala.collection.JavaConverters._

object OnlineFeedbackCommand {
	def apply(module: Module, assignment: Assignment, submitter: CurrentUser) =
		new OnlineFeedbackCommand(module, assignment, submitter)
			with ComposableCommand[Seq[StudentFeedbackGraph]]
			with OnlineFeedbackPermissions
			with AutowiringSubmissionServiceComponent
			with AutowiringFeedbackServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringAssessmentMembershipServiceComponent
			with Unaudited
			with ReadOnly
}

abstract class OnlineFeedbackCommand(val module: Module, val assignment: Assignment, val submitter: CurrentUser)
	extends CommandInternal[Seq[StudentFeedbackGraph]] with Appliable[Seq[StudentFeedbackGraph]] with OnlineFeedbackState {

	self: SubmissionServiceComponent with FeedbackServiceComponent with UserLookupComponent with AssessmentMembershipServiceComponent =>

	val marker: User = submitter.apparentUser

	def applyInternal(): Seq[StudentFeedbackGraph] = {

		val usercodes = assignment.getUsercodesWithSubmissionOrFeedback.filter(_.hasText).toSeq
		val studentsWithSubmissionOrFeedback = userLookup.getUsersByUserIds(usercodes)
			.values
			.filter(_.isFoundUser)
			.toSeq
			.sortBy(u => s"${u.getWarwickId}${u.getUserId}")

	  val studentsWithSubmissionOrFeedbackUsercodes = studentsWithSubmissionOrFeedback.map(_.getUserId)

		val unsubmittedStudents = assessmentMembershipService.determineMembershipUsers(assignment)
				.filterNot { u => studentsWithSubmissionOrFeedbackUsercodes.contains(u.getUserId) }

		val students = studentsWithSubmissionOrFeedback ++ unsubmittedStudents
		students.map { student =>
			val hasSubmission = submissionService.getSubmissionByUsercode(assignment, student.getUserId).isDefined
			val feedback = feedbackService.getAssignmentFeedbackByUsercode(assignment, student.getUserId)
			val (hasFeedback, hasPublishedFeedback) = feedback match {
				case Some(f) => (true, f.released.booleanValue)
				case _ => (false, false)
			}
			new StudentFeedbackGraph(student, hasSubmission, hasFeedback, hasPublishedFeedback, false, false)
		}
	}

}


object OnlineMarkerFeedbackCommand {
	def apply(module: Module, assignment: Assignment, marker: User, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
		new OnlineMarkerFeedbackCommand(module, assignment, marker, submitter, gradeGenerator)
			with ComposableCommand[Seq[StudentFeedbackGraph]]
			with OnlineFeedbackPermissions
			with AutowiringUserLookupComponent
			with AutowiringSubmissionServiceComponent
			with AutowiringFeedbackServiceComponent
			with Unaudited
			with ReadOnly
}

abstract class OnlineMarkerFeedbackCommand(
	val module: Module,
	val assignment: Assignment,
	val marker: User,
	val submitter: CurrentUser,
	val gradeGenerator: GeneratesGradesFromMarks
)	extends CommandInternal[Seq[StudentFeedbackGraph]] with Appliable[Seq[StudentFeedbackGraph]] with OnlineFeedbackState {

	self: SubmissionServiceComponent with FeedbackServiceComponent with UserLookupComponent =>

	def applyInternal(): Seq[StudentFeedbackGraph] = {

		val students = Option(assignment.markingWorkflow).map(_.getMarkersStudents(assignment, marker)).getOrElse(Nil)

		students.filter(s => assignment.isReleasedForMarking(s.getUserId)).map { student =>

			val hasSubmission = assignment.submissions.asScala.exists(_.usercode == student.getUserId)
			val feedback = feedbackService.getAssignmentFeedbackByUsercode(assignment, student.getUserId)
			// get all the feedbacks for this user and pick the most recent
			val markerFeedback = assignment.getAllMarkerFeedbacks(student.getUserId, marker).headOption

			val hasUncompletedFeedback = markerFeedback.exists(_.hasContent)
			// the current feedback for the marker is completed or if the parent feedback isn't a placeholder then marking is completed
			val hasCompletedFeedback = markerFeedback.exists(_.state == MarkingCompleted)
			val hasRejectedFeedback = markerFeedback.exists(_.state == Rejected)

			val hasPublishedFeedback = feedback match {
				case Some(f) => f.released.booleanValue
				case None => false
			}

			new StudentFeedbackGraph(
				student,
				hasSubmission,
				hasUncompletedFeedback,
				hasPublishedFeedback,
				hasCompletedFeedback,
				hasRejectedFeedback
			)
		}
	}
}


trait OnlineFeedbackPermissions extends RequiresPermissionsChecking {

	self: OnlineFeedbackState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.AssignmentFeedback.Read, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}
}

trait OnlineFeedbackState {
	val assignment: Assignment
	val module: Module
	val marker: User
	val submitter: CurrentUser
}


case class StudentFeedbackGraph(
	student: User,
	hasSubmission: Boolean,
	hasUncompletedFeedback: Boolean,
	hasPublishedFeedback: Boolean,
	hasCompletedFeedback: Boolean,
	hasRejectedFeedback: Boolean
)