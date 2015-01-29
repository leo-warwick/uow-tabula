package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.web.views.AutowiredTextRendererComponent
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.services.FeedbackService
import uk.ac.warwick.tabula.web.views.PDFView
import uk.ac.warwick.tabula.pdf.FreemarkerXHTMLPDFGeneratorComponent
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.commands._
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.data.model.Feedback
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(value = Array("/module/{module}/{assignment}/{student}/feedback.pdf"))
class DownloadFeedbackAsPdfController extends CourseworkController {

	type DownloadFeedbackAsPdfCommand = Appliable[Feedback]
	var feedbackService = Wire[FeedbackService]

	@ModelAttribute def command(
		@PathVariable("module") module: Module,
		@PathVariable("assignment") assignment: Assignment,
		@PathVariable("student") student: User): DownloadFeedbackAsPdfCommand = {

		// We send a permission denied explicitly (this would normally be a 404 for feedback not found) because PDF handling is silly in Chrome et al
		if (!user.loggedIn) {
			throw new PermissionDeniedException(user, Permissions.Feedback.Read, assignment)
		}

		DownloadFeedbackAsPdfCommand(module, assignment, mandatory(feedbackService.getFeedbackByUniId(assignment, student.getWarwickId)))
	}

	@RequestMapping
	def viewAsPdf(command: DownloadFeedbackAsPdfCommand, @PathVariable("student") student: User) = {
		new PDFView(
			"feedback.pdf",
			"/WEB-INF/freemarker/admin/assignments/markerfeedback/feedback-download.ftl",
			Map("feedback" -> command.apply(), "user"-> student)
		) with FreemarkerXHTMLPDFGeneratorComponent with AutowiredTextRendererComponent
	}

}

object DownloadFeedbackAsPdfCommand {
	def apply(module: Module, assignment: Assignment, feedback: Feedback) =
		new DownloadFeedbackAsPdfCommandInternal(module, assignment, feedback)
			with ComposableCommand[Feedback]
			with DownloadFeedbackAsPdfPermissions
			with DownloadFeedbackAsPdfAudit
}

class DownloadFeedbackAsPdfCommandInternal(val module: Module, val assignment: Assignment, val feedback: Feedback)
	extends CommandInternal[Feedback] with DownloadFeedbackAsPdfState {
	override def applyInternal() = feedback
}

trait DownloadFeedbackAsPdfPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DownloadFeedbackAsPdfState =>
	
	def permissionsCheck(p: PermissionsChecking) {
		notDeleted(assignment)
		mustBeLinked(assignment, module)
		
		p.PermissionCheck(Permissions.Feedback.Read, feedback)
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
}
