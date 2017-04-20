package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.{ModifyAssignmentOptionsCommand, _}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.turnitinlti.TurnitinLtiService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkBreadcrumbs


abstract class AbstractAssignmentOptionsController extends AbstractAssignmentController {

	type ModifyAssignmentOptionsCommand = Appliable[Assignment] with ModifyAssignmentOptionsCommandState with PopulateOnForm

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def modifyAssignmentFeedbackCommand(@PathVariable assignment: Assignment) =
		ModifyAssignmentOptionsCommand(mandatory(assignment))

	def showForm(form: ModifyAssignmentOptionsCommand, mode: String): Mav = {
		val module = form.module
		Mav(s"$urlPrefix/admin/assignments/assignment_options_details",
			"module" -> module,
			"mode" -> mode,
			"turnitinFileSizeLimit" -> TurnitinLtiService.maxFileSizeInMegabytes
		).crumbs(CourseworkBreadcrumbs.Assignment.AssignmentManagement())
	}

	def submit(cmd: ModifyAssignmentOptionsCommand, errors: Errors, path: String, mode: String) = {
		if (errors.hasErrors) {
			showForm(cmd, mode)
		} else {
			cmd.apply()
			Redirect(path)
		}
	}

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}"))
class ModifyAssignmentOptionsController extends AbstractAssignmentOptionsController {

	@ModelAttribute("command") def command(@PathVariable assignment: Assignment): ModifyAssignmentOptionsCommand =
		ModifyAssignmentOptionsCommand(mandatory(assignment))


	@RequestMapping(method = Array(GET), value = Array("/new/options"))
	def form(
		@PathVariable("assignment") assignment: Assignment,
		@ModelAttribute("command") cmd: ModifyAssignmentOptionsCommand
	): Mav = {
		cmd.populate()
		showForm(cmd, createMode)
	}

	@RequestMapping(method = Array(GET), value = Array("/edit/options"))
	def formEdit(
		@PathVariable("assignment") assignment: Assignment,
		@ModelAttribute("command") cmd: ModifyAssignmentOptionsCommand
	): Mav = {
		cmd.populate()
		showForm(cmd, editMode)
	}

	@RequestMapping(method = Array(POST), value = Array("/new/options"), params = Array(ManageAssignmentMappingParameters.reviewAssignment, "action!=refresh", "action!=update"))
	def submitAndAddOptions(@Valid @ModelAttribute("command") cmd: ModifyAssignmentOptionsCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
		submit(cmd, errors, Routes.admin.assignment.reviewAssignment(assignment), createMode)

	@RequestMapping(method = Array(POST), value = Array("/new/options"), params = Array(ManageAssignmentMappingParameters.createAndAddOptions, "action!=refresh", "action!=update"))
	def saveAndExit(@Valid @ModelAttribute("command") cmd: ModifyAssignmentOptionsCommand, errors: Errors): Mav =
		submit(cmd, errors, Routes.home, createMode)

	@RequestMapping(method = Array(POST), value = Array("/edit/options"), params = Array(ManageAssignmentMappingParameters.reviewAssignment, "action!=refresh", "action!=update"))
	def submitAndAddOptionsForEdit(@Valid @ModelAttribute("command") cmd: ModifyAssignmentOptionsCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
		submit(cmd, errors, Routes.admin.assignment.reviewAssignment(assignment), editMode)

	@RequestMapping(method = Array(POST), value = Array("/edit/options"), params = Array(ManageAssignmentMappingParameters.editAndAddOptions, "action!=refresh", "action!=update"))
	def saveAndExitForEdit(@Valid @ModelAttribute("command") cmd: ModifyAssignmentOptionsCommand, errors: Errors): Mav =
		submit(cmd, errors, Routes.home, editMode)

}