package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.coursework.assignments.AddFeedbackCommand
import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/feedback/new"))
class OldAddFeedbackController extends OldCourseworkController {

	@ModelAttribute
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment, user: CurrentUser) =
		new AddFeedbackCommand(module, assignment, user.apparentUser, user)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute form: AddFeedbackCommand): Mav = {
		Mav(s"$urlPrefix/admin/assignments/feedback/form",
			"department" -> form.module.adminDepartment,
			"module" -> form.module,
			"assignment" -> form.assignment)
			.crumbs(Breadcrumbs.Department(form.module.adminDepartment), Breadcrumbs.Module(form.module))
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid form: AddFeedbackCommand, errors: Errors): Mav = {
		transactional() {
			form.preExtractValidation(errors)
			form.postExtractValidation(errors)
			if (errors.hasErrors) {
				showForm(form)
			} else {
				form.apply()
				Mav("redirect:" + Routes.admin.module(form.module))
			}
		}
	}

}