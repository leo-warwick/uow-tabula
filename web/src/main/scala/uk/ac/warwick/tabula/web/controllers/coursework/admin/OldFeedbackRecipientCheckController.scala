package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.coursework.feedback.FeedbackRecipientCheckCommand
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Assignment

/**
 * For calling by AJAX. Returns a report of the email addresses that would be
 * the recipients for any published feedback, noting ones that appear empty or
 * invalid.
 */
@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/check-recipients"))
class OldFeedbackRecipientCheckController extends OldCourseworkController {

	@ModelAttribute def command(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		new FeedbackRecipientCheckCommand(module, assignment)

	@RequestMapping()
	def confirmation(command: FeedbackRecipientCheckCommand, errors: Errors): Mav = {
		val report = command.apply()
		Mav(s"$urlPrefix/admin/assignments/publish/checkrecipients",
			"assignment" -> command.assignment,
			"report" -> report).noLayout()
	}

}