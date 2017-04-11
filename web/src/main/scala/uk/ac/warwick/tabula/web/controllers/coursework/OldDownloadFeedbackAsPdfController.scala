package uk.ac.warwick.tabula.web.controllers.coursework

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{CurrentUser, PermissionDeniedException}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.coursework.DownloadFeedbackAsPdfCommand
import uk.ac.warwick.tabula.commands.profiles.PhotosWarwickMemberPhotoUrlGeneratorComponent
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.pdf.FreemarkerXHTMLPDFGeneratorComponent
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.FeedbackService
import uk.ac.warwick.tabula.web.views.{AutowiredTextRendererComponent, PDFView}
import uk.ac.warwick.userlookup.User

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/module/{module}/{assignment}/{student}/feedback.pdf"))
class OldDownloadFeedbackAsPdfController extends OldCourseworkController {

	type DownloadFeedbackAsPdfCommand = Appliable[Feedback]
	var feedbackService: FeedbackService = Wire[FeedbackService]

	@ModelAttribute def command(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable student: User): DownloadFeedbackAsPdfCommand = {

		// We send a permission denied explicitly (this would normally be a 404 for feedback not found) because PDF handling is silly in Chrome et al
		if (!user.loggedIn) {
			throw new PermissionDeniedException(user, Permissions.AssignmentFeedback.Read, assignment)
		}

		DownloadFeedbackAsPdfCommand(module, assignment, mandatory(feedbackService.getAssignmentFeedbackByUsercode(assignment, student.getUserId)))
	}

	@RequestMapping
	def viewAsPdf(command: DownloadFeedbackAsPdfCommand, @PathVariable student: User): PDFView with FreemarkerXHTMLPDFGeneratorComponent with AutowiredTextRendererComponent with PhotosWarwickMemberPhotoUrlGeneratorComponent = {
		new PDFView(
			"feedback.pdf",
			DownloadFeedbackAsPdfCommand.feedbackDownloadTemple,
			Map(
				"feedback" -> command.apply(),
				"studentId" -> CurrentUser.studentIdentifier(student)
			)
		) with FreemarkerXHTMLPDFGeneratorComponent with AutowiredTextRendererComponent with PhotosWarwickMemberPhotoUrlGeneratorComponent
	}

}
