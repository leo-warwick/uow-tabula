package uk.ac.warwick.tabula.web.controllers.coursework.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.coursework.assignments.SharedAssignmentPropertiesForm
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, Department}
import uk.ac.warwick.tabula.services.turnitinlti.TurnitinLtiService
import uk.ac.warwick.tabula.web.Mav

/**
 * When setting up a batch of assignments using AddAssignmentsController, we need
 * to open a dialog for entering assignment settings. This controller shows that
 * form, does validation and then the resulting form fields are injected into the
 * original HTML page.
 */
@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/department/{department}/shared-options"))
class OldAssignmentSharedOptionsController extends OldCourseworkController {

	@RequestMapping(method = Array(GET))
	def showForm(@ModelAttribute form: SharedAssignmentPropertiesForm, errors: Errors, @PathVariable department: Department): Mav = {
		mav(form, department)
	}

	@RequestMapping(method = Array(POST))
	def submitForm(@Valid @ModelAttribute form: SharedAssignmentPropertiesForm, errors: Errors, @PathVariable department: Department): Mav = {
		mav(form, department).addObjects(
			"submitted" -> true,
			"hasErrors" -> errors.hasErrors)
	}

	def mav(form: SharedAssignmentPropertiesForm, @PathVariable department: Department): Mav = {
		Mav(s"$urlPrefix/admin/assignments/shared_options",
			"department" -> department,
			"maxWordCount" -> Assignment.MaximumWordCount,
			"turnitinFileSizeLimit" -> TurnitinLtiService.maxFileSizeInMegabytes
		).noLayout()
	}

	@ModelAttribute
	def model(department: Department) = new SharedAssignmentPropertiesForm()

}
