package uk.ac.warwick.tabula.coursework.commands.feedback

import org.joda.time.DateTime
import uk.ac.warwick.tabula.{ItemNotFoundException, CurrentUser}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.notifications.coursework.FeedbackAdjustmentNotification
import uk.ac.warwick.tabula.data.model.{Notification, Submission, Assignment, Feedback}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringZipServiceComponent, AutowiringFeedbackServiceComponent, ZipServiceComponent, FeedbackServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import org.springframework.validation.Errors
import uk.ac.warwick.userlookup.User

object FeedbackAdjustmentCommand {

	final val REASON_SIZE_LIMIT = 600

	def apply(assignment: Assignment, student:User, submitter: CurrentUser) =
		new FeedbackAdjustmentCommandInternal(assignment, student, submitter)
			with ComposableCommand[Feedback]
			with FeedbackAdjustmentCommandPermissions
			with FeedbackAdjustmentCommandDescription
			with FeedbackAdjustmentCommandValidation
			with FeedbackAdjustmentNotifier
			with AutowiringFeedbackServiceComponent
			with AutowiringZipServiceComponent
}

class FeedbackAdjustmentCommandInternal(val assignment: Assignment, val student:User, val submitter: CurrentUser)
	extends CommandInternal[Feedback] with FeedbackAdjustmentCommandState with SubmissionState {

	self: FeedbackServiceComponent with ZipServiceComponent =>

	val submission = assignment.findSubmission(student.getWarwickId)
	val feedback = assignment.findFeedback(student.getWarwickId)
		.getOrElse(throw new ItemNotFoundException("Can't adjust for non-existent feedback"))
	copyFrom(feedback)

	def applyInternal() = {
		copyTo(feedback)

		// if we are updating existing feedback then invalidate any cached feedback zips
		if(feedback.id != null) {
			zipService.invalidateIndividualFeedbackZip(feedback)
			zipService.invalidateFeedbackZip(assignment)
		}

		feedback.updatedDate = DateTime.now
		feedbackService.saveOrUpdate(feedback)
		feedback
	}

	def copyFrom(feedback: Feedback) {
		// mark and grade
		if (assignment.collectMarks) {
			actualMark = feedback.actualMark.map(_.toString).orNull
			actualGrade = feedback.actualGrade.orNull
			adjustedMark = feedback.adjustedMark.map(_.toString).orNull
			adjustedGrade = feedback.adjustedGrade.getOrElse("")
			reason = feedback.adjustmentReason
			comments = feedback.adjustmentComments
		}
	}

	def copyTo(feedback: Feedback) {
		// save mark and grade
		if (assignment.collectMarks) {
			feedback.adjustedMark = adjustedMark.maybeText.map(_.toInt)
			feedback.adjustedGrade = adjustedGrade.maybeText
			feedback.adjustmentReason = reason
			feedback.adjustmentComments = comments
		}
	}

}

trait FeedbackAdjustmentCommandValidation extends SelfValidating {
	self: FeedbackAdjustmentCommandState =>
	def validate(errors: Errors) {
		if (!reason.hasText)
			errors.rejectValue("reason", "feedback.adjustment.reason.empty")
		else if(reason.length > FeedbackAdjustmentCommand.REASON_SIZE_LIMIT)
			errors.rejectValue("reason", "feedback.adjustment.reason.tooBig")
		if (!comments.hasText) errors.rejectValue("comments", "feedback.adjustment.comments.empty")
		// validate mark (must be int between 0 and 100)
		if (adjustedMark.hasText) {
			try {
				val asInt = adjustedMark.toInt
				if (asInt < 0 || asInt > 100) {
					errors.rejectValue("adjustedMark", "actualMark.range")
				}
			} catch {
				case _ @ (_: NumberFormatException | _: IllegalArgumentException) =>
					errors.rejectValue("adjustedMark", "actualMark.format")
			}
		}
	}
}

trait FeedbackAdjustmentCommandState {
	val assignment: Assignment
	val student: User
	val feedback: Feedback
	val submission: Option[Submission]

	var adjustedMark: String = _
	var adjustedGrade: String = _

	var actualMark: String = _
	var actualGrade: String = _
	
	var reason: String = _
	var comments: String = _

	val submitter: CurrentUser
}

trait FeedbackAdjustmentCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: FeedbackAdjustmentCommandState =>
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Feedback.Update, mandatory(assignment))
	}
}

trait FeedbackAdjustmentCommandDescription extends Describable[Feedback] {
	self: FeedbackAdjustmentCommandState =>
	def describe(d: Description) {
		d.assignment(assignment)
		d.studentIds(Seq(student.getUserId))
		d.property("adjustmentReason", comments)
		d.property("adjustmentComments", comments)
	}
}

trait FeedbackAdjustmentNotifier extends Notifies[Feedback, Feedback] {
	self: FeedbackAdjustmentCommandState =>

	def emit(feedback: Feedback) = {
		if (assignment.hasWorkflow) {
			Seq(Notification.init(new FeedbackAdjustmentNotification, submitter.apparentUser, feedback, feedback.assignment))
		} else {
			Nil
		}
	}

}