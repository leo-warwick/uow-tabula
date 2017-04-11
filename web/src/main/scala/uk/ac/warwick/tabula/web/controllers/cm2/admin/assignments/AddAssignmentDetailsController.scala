package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.{AssignmentDetailsCommandState, CreateAssignmentDetailsCommand, CreateAssignmentDetailsCommandInternal}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.controllers.cm2.{CourseworkBreadcrumbs, CourseworkController}
import uk.ac.warwick.tabula.web.{Breadcrumbs, Mav}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/{module}/assignments/new"))
class AddAssignmentDetailsController extends CourseworkController {

	type CreateAssignmentDetailsCommand = CreateAssignmentDetailsCommandInternal with Appliable[Assignment] with AssignmentDetailsCommandState
	validatesSelf[SelfValidating]

	@ModelAttribute("ManageAssignmentMappingParameters")
	def params = ManageAssignmentMappingParameters

	@ModelAttribute("command")
	def createAssignmentDetailsCommand(@PathVariable module: Module) =
		CreateAssignmentDetailsCommand(mandatory(module))

	@RequestMapping
	def form(@ModelAttribute("command") form: CreateAssignmentDetailsCommand): Mav = {
		form.prefillFromRecentAssignment()
		showForm(form)
	}


	def showForm(form: CreateAssignmentDetailsCommand): Mav = {
		val module = form.module

		Mav(s"$urlPrefix/admin/assignments/new_assignment_details",
			"department" -> module.adminDepartment,
			"module" -> module,
			"academicYear" -> form.academicYear
		).crumbs(CourseworkBreadcrumbs.Assignment.AssignmentManagement())
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddFeedback, "action!=refresh", "action!=update, action=submit"))
	def submitAndAddFeedback(@Valid @ModelAttribute("command") cmd: CreateAssignmentDetailsCommand, errors: Errors): Mav =
		submit(cmd, errors, Routes.admin.assignment.createAddFeedback)

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddDetails, "action!=refresh", "action!=update"))
	def saveAndExit(@ModelAttribute("command") cmd: CreateAssignmentDetailsCommand, errors: Errors): Mav = {
		submit(cmd, errors, { _ => Routes.home })
	}

	private def submit(cmd: CreateAssignmentDetailsCommand, errors: Errors, route: Assignment => String) = {
		if (errors.hasErrors) form(cmd)
		else {
			val assignment = cmd.apply()
			RedirectForce(route(assignment))
		}
	}
}
