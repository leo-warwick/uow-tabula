package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.coursework.feedback.SubmissionState
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, Submission}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

case class FeedbackAuditData(submission: Option[Submission], feedback: Option[Feedback])

object FeedbackAuditCommand {
	def apply(assignment: Assignment, student: User) =
		new FeedbackAuditCommandInternal(assignment, student)
			with ComposableCommand[FeedbackAuditData]
			with ReadOnly
			with FeedbackAuditCommandPermissions
			with FeedbackAuditCommandDescription
			with AutowiringSubmissionServiceComponent
			with AutowiringFeedbackServiceComponent
			with AutowiringProfileServiceComponent
			with SubmissionState
}

class FeedbackAuditCommandInternal(val assignment: Assignment, val student: User) extends CommandInternal[FeedbackAuditData]
	with FeedbackAuditCommandState {

	self : SubmissionServiceComponent with FeedbackServiceComponent =>

	lazy val submission: Option[Submission] = submissionService.getSubmissionByUsercode(assignment, student.getUserId)

	def applyInternal(): FeedbackAuditData = {
		val feedback = feedbackService.getAssignmentFeedbackByUsercode(assignment, student.getUserId)
		FeedbackAuditData(submission, feedback)
	}
}

trait FeedbackAuditCommandState {
	val assignment: Assignment
	val student: User
}

trait FeedbackAuditCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: FeedbackAuditCommandState =>
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Submission.Read, mandatory(assignment))
		p.PermissionCheck(Permissions.AssignmentFeedback.Read, mandatory(assignment))
	}
}

trait FeedbackAuditCommandDescription extends Describable[FeedbackAuditData] {
	self: FeedbackAuditCommandState =>
	def describe(d: Description) {
		d.studentIds(Option(student.getWarwickId).toSeq)
		d.studentUsercodes(student.getUserId)
		d.assignment(assignment)
	}
}