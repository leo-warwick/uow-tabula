package uk.ac.warwick.tabula.web.controllers.coursework.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands.coursework.assignments._
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.turnitinlti.TurnitinLtiService
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/delete"))
class OldDeleteAssignmentController extends OldCourseworkController {

	validatesSelf[DeleteAssignmentCommand]

	@ModelAttribute
	def formObject(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		new DeleteAssignmentCommand(module, mandatory(assignment))

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def showForm(form: DeleteAssignmentCommand): Mav = {
		val (module, assignment) = (form.module, form.assignment)

		Mav(s"$urlPrefix/admin/assignments/delete",
			"department" -> module.adminDepartment,
			"module" -> module,
			"assignment" -> assignment,
			"maxWordCount" -> Assignment.MaximumWordCount,
			"turnitinFileSizeLimit" -> TurnitinLtiService.maxFileSizeInMegabytes
		).crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module))
	}

	@RequestMapping(method = Array(RequestMethod.POST))
	def submit(@Valid form: DeleteAssignmentCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			showForm(form)
		} else {
			form.apply()
			Redirect(Routes.admin.module(form.module))
		}

	}

}
