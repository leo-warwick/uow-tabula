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
import uk.ac.warwick.tabula.web.controllers.cm2.{CourseworkBreadcrumbs, CourseworkController}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/new/{assignment}/options"))
class ModifyAssignmentOptionsController extends CourseworkController {

	type ModifyAssignmentOptionsCommand = Appliable[Assignment] with ModifyAssignmentOptionsCommandState with PopulateOnForm

	validatesSelf[SelfValidating]

	@ModelAttribute("ManageAssignmentMappingParameters")
	def params = ManageAssignmentMappingParameters

	@ModelAttribute("command") def command(@PathVariable assignment: Assignment): ModifyAssignmentOptionsCommand =
		ModifyAssignmentOptionsCommand(mandatory(assignment))

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("assignment") assignment: Assignment,
		@ModelAttribute("command") cmd: ModifyAssignmentOptionsCommand
	): Mav = {
		cmd.populate()
		showForm(cmd)
	}

	def showForm(form: ModifyAssignmentOptionsCommand): Mav = {
		val module = form.module
		Mav(s"$urlPrefix/admin/assignments/assignment_options_details",
			"module" -> module,
			"turnitinFileSizeLimit" -> TurnitinLtiService.maxFileSizeInMegabytes
		).crumbs(CourseworkBreadcrumbs.Assignment.AssignmentManagement())
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.reviewAssignment, "action!=refresh", "action!=update"))
	def submitAndAddOptions(@Valid @ModelAttribute("command") cmd: ModifyAssignmentOptionsCommand, errors: Errors): Mav =
		submit(cmd, errors, Routes.admin.assignment.reviewAssignment)

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddOptions, "action!=refresh", "action!=update"))
	def saveAndExit(@Valid @ModelAttribute("command") cmd: ModifyAssignmentOptionsCommand, errors: Errors): Mav = {
		submit(cmd, errors, { _ => Routes.home })
	}

	private def submit(cmd: ModifyAssignmentOptionsCommand, errors: Errors, route: Assignment => String) = {
		if (errors.hasErrors) showForm(cmd)
		else {
			val assignment = cmd.apply()
			RedirectForce(route(assignment))
		}
	}
}