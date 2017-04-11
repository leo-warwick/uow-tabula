package uk.ac.warwick.tabula.commands.coursework.feedback

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.data.model.{Assignment, AssignmentFeedback, Feedback}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.{AutowiringFeedbackServiceComponent, FeedbackServiceComponent}
import uk.ac.warwick.tabula.permissions.Permissions

object FeedbackSummaryCommand {
	def apply(assignment: Assignment, student: User) =
		new FeedbackSummaryCommandInternal(assignment, student)
			with ComposableCommand[Option[Feedback]]
			with FeedbackSummaryCommandPermissions
			with FeedbackSummaryCommandDescription
			with AutowiringFeedbackServiceComponent
			with ReadOnly
}

class FeedbackSummaryCommandInternal(val assignment: Assignment, val student: User)
	extends CommandInternal[Option[Feedback]] with FeedbackSummaryCommandState {

	this : FeedbackServiceComponent =>

	def applyInternal(): Option[AssignmentFeedback] =
		feedbackService.getAssignmentFeedbackByUsercode(assignment, student.getUserId)
}

trait FeedbackSummaryCommandState {
	val student: User
	val assignment: Assignment
}

trait FeedbackSummaryCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: FeedbackSummaryCommandState =>
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentFeedback.Read, assignment)
	}
}

trait FeedbackSummaryCommandDescription extends Describable[Option[Feedback]] {
	self: FeedbackSummaryCommandState =>
	def describe(d: Description) {
		d.assignment(assignment)
		d.studentIds(Option(student.getWarwickId).toSeq)
		d.studentUsercodes(student.getUserId)
	}
}
