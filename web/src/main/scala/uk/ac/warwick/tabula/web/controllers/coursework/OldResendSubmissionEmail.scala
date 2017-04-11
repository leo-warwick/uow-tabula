package uk.ac.warwick.tabula.web.controllers.coursework

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.commands.coursework.assignments.SendSubmissionReceiptCommand
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.spring.Wire
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.SubmissionService

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/module/{module}/{assignment}/resend-receipt"))
class OldResendSubmissionEmail extends OldCourseworkController {

	var submissionService: SubmissionService = Wire.auto[SubmissionService]

	hideDeletedItems

	@ModelAttribute def command(@PathVariable module: Module, @PathVariable assignment: Assignment, user: CurrentUser) =
		new SendSubmissionReceiptCommand(
			module, assignment,
			mandatory(submissionService.getSubmissionByUsercode(assignment, user.userId).filter(_.submitted)),
			user)

	@RequestMapping(method = Array(GET, HEAD))
	def nope(form: SendSubmissionReceiptCommand) = Redirect(Routes.assignment(mandatory(form.assignment)))

	@RequestMapping(method = Array(POST))
	def sendEmail(form: SendSubmissionReceiptCommand): Mav = {
		val sent = form.apply()

		Mav(s"$urlPrefix/submit/receipt",
			"submission" -> form.submission,
			"module" -> form.module,
			"assignment" -> form.assignment,
			"sent" -> sent,
			"hasEmail" -> user.email.hasText)

	}

}
