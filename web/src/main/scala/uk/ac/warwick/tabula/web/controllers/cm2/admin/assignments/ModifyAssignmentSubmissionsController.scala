package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.{ModifyAssignmentSubmissionsCommand, _}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.{CourseworkBreadcrumbs, CourseworkController}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/new/{assignment}/submissions"))
class ModifyAssignmentSubmissionsController extends CourseworkController {

	type ModifyAssignmentSubmissionsCommand = Appliable[Assignment] with ModifyAssignmentSubmissionsCommandState with PopulateOnForm


	@ModelAttribute("ManageAssignmentMappingParameters")
	def params = ManageAssignmentMappingParameters


	@ModelAttribute("command") def command(@PathVariable assignment: Assignment): ModifyAssignmentSubmissionsCommand =
		ModifyAssignmentSubmissionsCommand(mandatory(assignment))


	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("assignment") assignment: Assignment,
		@ModelAttribute("command") cmd: ModifyAssignmentSubmissionsCommand
	): Mav = {
		cmd.populate()
		showForm(cmd)
	}


	def showForm(form: ModifyAssignmentSubmissionsCommand): Mav = {
		val module = form.module
		Mav(s"$urlPrefix/admin/assignments/assignment_submissions_details",
			"module" -> module
		).crumbs(CourseworkBreadcrumbs.Assignment.AssignmentManagement())
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddOptions, "action!=refresh", "action!=update"))
	def submitAndAddSubmissions(@ModelAttribute("command") cmd: ModifyAssignmentSubmissionsCommand, errors: Errors): Mav =
	submit(cmd, errors, Routes.admin.assignment.createAddOptions)

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddSubmissions, "action!=refresh", "action!=update"))
	def saveAndExit(@ModelAttribute("command") cmd: ModifyAssignmentSubmissionsCommand, errors: Errors): Mav = {
		submit(cmd, errors, { _ => Routes.home })
	}


	private def submit(cmd: ModifyAssignmentSubmissionsCommand, errors: Errors, route: Assignment => String) = {
		if (errors.hasErrors) showForm(cmd)
		else {
			val assignment = cmd.apply()
			RedirectForce(route(assignment))
		}
	}
}