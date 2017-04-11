package uk.ac.warwick.tabula.commands.coursework

import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.{AssignmentFeedback, ExamFeedback, Feedback}
import uk.ac.warwick.tabula.commands.{Description, Describable, CommandInternal, ComposableCommand}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions

object ViewOnlineFeedbackCommand {
	def apply(feedback: Feedback) =
		new ViewOnlineFeedbackCommandInternal(feedback)
			with ComposableCommand[Feedback]
			with ViewOnlineFeedbackCommandDescription
			with ViewOnlineFeedbackCommandPermissions
}

trait ViewOnlineFeedbackCommandState {
	def feedback: Feedback
}

class ViewOnlineFeedbackCommandInternal(val feedback: Feedback) extends CommandInternal[Feedback] with ViewOnlineFeedbackCommandState {
	def applyInternal(): Feedback = feedback
}

trait ViewOnlineFeedbackCommandDescription extends Describable[Feedback] {
	self: ViewOnlineFeedbackCommandState =>

	override lazy val eventName = "ViewOnlineFeedback"

	def describe(d: Description): Unit = HibernateHelpers.initialiseAndUnproxy(feedback) match {
		case assignmentFeedback: AssignmentFeedback =>
			d.assignment(assignmentFeedback.assignment).properties("student" -> feedback.studentIdentifier)
		case examFeedback: ExamFeedback =>
			d.exam(examFeedback.exam).properties("student" -> feedback.studentIdentifier)
	}
}

trait ViewOnlineFeedbackCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewOnlineFeedbackCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentFeedback.Read, mandatory(feedback))
	}
}