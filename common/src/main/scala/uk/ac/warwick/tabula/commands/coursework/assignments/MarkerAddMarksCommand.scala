package uk.ac.warwick.tabula.commands.coursework.assignments

import org.joda.time.DateTime
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.MarkingState.{InProgress, ReleasedForMarking}
import uk.ac.warwick.tabula.data.model.{Assignment, Module, _}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.coursework.docconversion.{AutowiringMarksExtractorComponent, MarkItem}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import collection.JavaConverters._

object MarkerAddMarksCommand {
	def apply(module: Module, assignment: Assignment, marker: User, submitter: CurrentUser, firstMarker: Boolean, gradeGenerator: GeneratesGradesFromMarks) =
		new MarkerAddMarksCommandInternal(module, assignment, marker, submitter, firstMarker, gradeGenerator)
			with AutowiringFeedbackServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringMarksExtractorComponent
			with ComposableCommand[List[MarkerFeedback]]
			with MarkerAddMarksDescription
			with MarkerAddMarksPermissions
			with MarkerAddMarksCommandValidation
			with MarkerAddMarksCommandState
			with PostExtractValidation
			with AddMarksCommandBindListener
}

class MarkerAddMarksCommandInternal(val module: Module, val assignment: Assignment, val marker: User, val submitter: CurrentUser, val firstMarker: Boolean, val gradeGenerator: GeneratesGradesFromMarks)
	extends CommandInternal[List[MarkerFeedback]] with CanProxy {

	self: MarkerAddMarksCommandState with FeedbackServiceComponent  =>

	override def applyInternal(): List[MarkerFeedback] = transactional() {

		def saveFeedback(user: User, actualMark: String, actualGrade: String) = {
			// For markers a parent feedback should _always_ exist (created when released for marking)
			val parentFeedback = assignment.feedbacks.asScala.find(_.usercode == user.getUserId).get

			// get marker feedback if it already exists - if not create one
			val markerFeedback:MarkerFeedback = firstMarker match {
				case true => parentFeedback.getFirstMarkerFeedback.getOrElse {
					val mf = new MarkerFeedback(parentFeedback)
					parentFeedback.firstMarkerFeedback = mf
					mf
				}
				case false => parentFeedback.getSecondMarkerFeedback.getOrElse {
					val mf = new MarkerFeedback(parentFeedback)
					parentFeedback.secondMarkerFeedback = mf
					mf
				}
			}

			markerFeedback.mark = StringUtils.hasText(actualMark) match {
				case true => Some(actualMark.toInt)
				case false => None
			}

			markerFeedback.grade = Option(actualGrade)

			if (markerFeedback.state == ReleasedForMarking) {
				markerFeedback.state = InProgress
			}

			parentFeedback.updatedDate = DateTime.now

			feedbackService.saveOrUpdate(parentFeedback)
			feedbackService.save(markerFeedback)
			markerFeedback
		}

		// persist valid marks
		val markList = marks.asScala.filter(_.isValid).map { (mark) => saveFeedback(mark.user, mark.actualMark, mark.actualGrade) }
		markList.toList
	}

}

trait MarkerAddMarksCommandValidation extends ValidatesMarkItem {

	self: MarkerAddMarksCommandState with UserLookupComponent =>

	override def checkMarkUpdated(mark: MarkItem) {
		// Warn if marks for this student are already uploaded
		assessment.feedbacks.asScala.find(feedback => feedback.usercode == mark.user.getUserId) match {
			case Some(feedback) =>
				if (assessment.isFirstMarker(marker)
					&& feedback.firstMarkerFeedback != null
					&& (feedback.firstMarkerFeedback.hasMark || feedback.firstMarkerFeedback.hasGrade)
				)
					mark.isModified = true
				else if (assessment.isSecondMarker(marker)
					&& feedback.secondMarkerFeedback != null
					&& (feedback.secondMarkerFeedback.hasMark || feedback.secondMarkerFeedback.hasGrade)
				)
					mark.isModified = true
			case None =>
		}
	}

	override def checkMarker(mark: MarkItem, errors: Errors, alreadyHasErrors: Boolean): Boolean = {
		var hasErrors = alreadyHasErrors
		assessment.feedbacks.asScala.find(feedback => feedback.usercode == mark.user.getUserId) match {
			case Some(feedback) =>
				val assignedMarker = firstMarker match {
					case true => assignment.getStudentsFirstMarker(feedback.usercode)
					case false => assignment.getStudentsSecondMarker(feedback.usercode)
				}

				if (!assignedMarker.contains(marker)) {
					errors.rejectValue("universityId", "uniNumber.wrong.marker")
					hasErrors = true
				}
			case None =>
				errors.rejectValue("universityId", "uniNumber.wrong.marker")
				hasErrors = true
		}
		hasErrors
	}
}

trait MarkerAddMarksDescription extends Describable[List[MarkerFeedback]] {

	self: MarkerAddMarksCommandState =>

	override lazy val eventName = "MarkerAddMarks"

	override def describe(d: Description) {
		d.assignment(assessment)
	}
}

trait MarkerAddMarksPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: MarkerAddMarksCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assessment, module)
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assessment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assessment)
		}
	}

}

trait MarkerAddMarksCommandState extends AddMarksCommandState {
	def marker: User
	def submitter: CurrentUser
	def assignment: Assignment
	override def assessment: Assignment = assignment
	def firstMarker: Boolean
}
