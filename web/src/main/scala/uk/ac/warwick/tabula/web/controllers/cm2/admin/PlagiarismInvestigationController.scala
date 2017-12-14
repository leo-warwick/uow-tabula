package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.assignments.{PlagiarismInvestigationCommand, PlagiarismInvestigationCommandValidation}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.{AdminSelectionAction, CourseworkController}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}/submissionsandfeedback/mark-plagiarised"))
class PlagiarismInvestigationController extends CourseworkController with AdminSelectionAction {

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment) = PlagiarismInvestigationCommand(mandatory(assignment), user.apparentUser)

	validatesSelf[PlagiarismInvestigationCommandValidation]

	def formView(assignment: Assignment): Mav =
		Mav("cm2/admin/assignments/submissionsandfeedback/mark-plagiarised",
			"assignment" -> assignment
		).crumbsList(Breadcrumbs.assignment(assignment))

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(@PathVariable assignment: Assignment, @ModelAttribute("command") form: Appliable[Unit], errors: Errors): Mav =
		formView(assignment)

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(@PathVariable assignment: Assignment, @Valid @ModelAttribute("command") form: Appliable[Unit], errors: Errors): Mav =
		if (errors.hasErrors) {
			formView(assignment)
		} else {
			form.apply()
			RedirectBack(assignment)
		}
}
