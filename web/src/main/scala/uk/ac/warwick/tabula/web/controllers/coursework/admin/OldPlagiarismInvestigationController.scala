package uk.ac.warwick.tabula.web.controllers.coursework.admin

import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.model.Assignment
import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.commands.coursework.assignments.{PlagiarismInvestigationCommand, PlagiarismInvestigationCommandValidation}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/submissionsandfeedback/mark-plagiarised"))
class OldPlagiarismInvestigationController extends OldCourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment) = PlagiarismInvestigationCommand(assignment, user.apparentUser)

	validatesSelf[PlagiarismInvestigationCommandValidation]

	def formView(assignment: Assignment): Mav =
		Mav(s"$urlPrefix/admin/assignments/submissionsandfeedback/mark-plagiarised",
				"assignment" -> assignment
		).crumbs(Breadcrumbs.Department(assignment.module.adminDepartment), Breadcrumbs.Module(assignment.module))

	def RedirectBack(assignment: Assignment) = Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))

	// shouldn't ever be called as a GET - if it is, just redirect back to the submission list
	@RequestMapping(method = Array(GET))
	def get(@PathVariable assignment: Assignment) = RedirectBack(assignment)

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(
			@PathVariable module: Module,
			@PathVariable assignment: Assignment,
			@ModelAttribute("command") form: Appliable[Unit], errors: Errors): Mav = {
		formView(assignment)
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(
			@PathVariable module: Module,
			@PathVariable assignment: Assignment,
			@Valid @ModelAttribute("command") form: Appliable[Unit], errors: Errors): Mav = {
		if (errors.hasErrors) {
			formView(assignment)
		} else {
			form.apply()
			RedirectBack(assignment)
		}
	}
}
