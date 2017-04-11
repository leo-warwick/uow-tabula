package uk.ac.warwick.tabula.commands.cm2.feedback

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object DownloadFeedbackAsPdfCommand {

	final val feedbackDownloadTemple = "/WEB-INF/freemarker/coursework/admin/assignments/markerfeedback/feedback-download.ftl"

	def apply(module: Module, assignment: Assignment, feedback: Feedback, student: Option[Member]) =
		new DownloadFeedbackAsPdfCommandInternal(module, assignment, feedback, student)
			with ComposableCommand[Feedback]
			with DownloadFeedbackAsPdfPermissions
			with DownloadFeedbackAsPdfAudit
}

class DownloadFeedbackAsPdfCommandInternal(val module: Module, val assignment: Assignment, val feedback: Feedback, val student: Option[Member])
	extends CommandInternal[Feedback] with DownloadFeedbackAsPdfState {
	override def applyInternal(): Feedback = feedback
}

trait DownloadFeedbackAsPdfPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DownloadFeedbackAsPdfState =>

	def permissionsCheck(p: PermissionsChecking) {
		notDeleted(assignment)
		mustBeLinked(assignment, module)

		student match {
			case Some(student: StudentMember) => 	p.PermissionCheckAny(
				Seq(CheckablePermission(Permissions.AssignmentFeedback.Read, feedback),
					CheckablePermission(Permissions.AssignmentFeedback.Read, student))
			)
			case _ => p.PermissionCheckAny(Seq(CheckablePermission(Permissions.AssignmentFeedback.Read, feedback)))
		}
	}
}

trait DownloadFeedbackAsPdfAudit extends Describable[Feedback] {
	self: DownloadFeedbackAsPdfState =>

	def describe(d: Description) {
		d.feedback(feedback)
	}
}

trait DownloadFeedbackAsPdfState {
	val module: Module
	val assignment: Assignment
	val feedback: Feedback
	val student: Option[Member]
}
