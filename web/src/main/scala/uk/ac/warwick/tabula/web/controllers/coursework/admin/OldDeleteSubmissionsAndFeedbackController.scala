package uk.ac.warwick.tabula.web.controllers.coursework.admin
import scala.collection.JavaConversions._
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import javax.validation.Valid

import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.commands.coursework.assignments.DeleteSubmissionsAndFeedbackCommand
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/submissionsandfeedback/delete"))
class OldDeleteSubmissionsAndFeedback extends OldCourseworkController {

	@ModelAttribute
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		new DeleteSubmissionsAndFeedbackCommand(module, assignment)

	validatesSelf[DeleteSubmissionsAndFeedbackCommand]

	def formView(assignment: Assignment): Mav =
		Mav(s"$urlPrefix/admin/assignments/submissionsandfeedback/delete",
			"assignment" -> assignment)
			.crumbs(
					Breadcrumbs.Department(assignment.module.adminDepartment),
					Breadcrumbs.Module(assignment.module))

	def RedirectBack(assignment: Assignment) = Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))

	@RequestMapping(method = Array(GET))
	def get(form: DeleteSubmissionsAndFeedbackCommand) = RedirectBack(form.assignment)

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(form: DeleteSubmissionsAndFeedbackCommand, errors: Errors): Mav = {
		form.prevalidate(errors)
		formView(form.assignment)
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(
			@Valid form: DeleteSubmissionsAndFeedbackCommand,
			errors: Errors): Mav = {
		transactional() {
			if (errors.hasErrors) {
				formView(form.assignment)
			} else {
				form.apply()
				RedirectBack(form.assignment)
			}
		}
	}
}
