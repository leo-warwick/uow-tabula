package uk.ac.warwick.tabula.web.controllers.cm2

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.cm2.assignments.SendSubmissionReceiptCommand
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.SubmissionService
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/submission/{assignment}/resend-receipt"))
class ResendSubmissionEmailController extends CourseworkController {

	var submissionService: SubmissionService = Wire.auto[SubmissionService]

	hideDeletedItems

	@ModelAttribute def command(@PathVariable assignment: Assignment, user: CurrentUser) =
		new SendSubmissionReceiptCommand(
			assignment,
			mandatory(submissionService.getSubmissionByUsercode(assignment, user.userId).filter(_.submitted)),
			user)

	@RequestMapping(method = Array(GET, HEAD))
	def nope(form: SendSubmissionReceiptCommand) = Redirect(Routes.assignment(mandatory(form.assignment)))

	@RequestMapping(method = Array(POST))
	def sendEmail(form: SendSubmissionReceiptCommand): Mav = {
		val sent = form.apply()

		Mav(s"$urlPrefix/submit/receipt",
			"submission" -> form.submission,
			"module" -> form.assignment.module,
			"assignment" -> form.assignment,
			"sent" -> sent,
			"hasEmail" -> user.email.hasText)

	}

}
