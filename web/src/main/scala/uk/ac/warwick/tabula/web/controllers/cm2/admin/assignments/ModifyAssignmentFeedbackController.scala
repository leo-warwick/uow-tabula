package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.{ModifyAssignmentFeedbackCommand, ModifyAssignmentFeedbackCommandState}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.{CourseworkBreadcrumbs, CourseworkController}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/new/{assignment}/feedback"))
class ModifyAssignmentFeedbackController extends CourseworkController {

	type ModifyAssignmentFeedbackCommand = Appliable[Assignment] with ModifyAssignmentFeedbackCommandState with PopulateOnForm

	@ModelAttribute("ManageAssignmentMappingParameters")
	def params = ManageAssignmentMappingParameters

	@ModelAttribute("command")
	def modifyAssignmentFeedbackCommand(@PathVariable assignment: Assignment) =
		ModifyAssignmentFeedbackCommand(mandatory(assignment))

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("assignment") assignment: Assignment,
		@ModelAttribute("command") cmd: ModifyAssignmentFeedbackCommand
	): Mav = {
		cmd.populate()
		showForm(cmd)
	}

	def showForm(form: ModifyAssignmentFeedbackCommand): Mav = {
		val module = form.module
		Mav(s"$urlPrefix/admin/assignments/assignment_feedback",
			"module" -> module,
			"department" -> module.adminDepartment
		).crumbs(CourseworkBreadcrumbs.Assignment.AssignmentManagement())
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddFeedback, "action!=refresh", "action!=update"))
	def saveAndExit(@ModelAttribute("command") cmd: ModifyAssignmentFeedbackCommand, errors: Errors): Mav = {
		submit(cmd, errors, { _ => Routes.home })
	}


	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddStudents, "action!=refresh", "action!=update, action=submit"))
	def submitAndAddStudents(@Valid @ModelAttribute("command") cmd: ModifyAssignmentFeedbackCommand, errors: Errors): Mav =
		submit(cmd, errors, Routes.admin.assignment.createAddStudents)

	private def submit(cmd: ModifyAssignmentFeedbackCommand, errors: Errors, route: Assignment => String) = {
		if (errors.hasErrors) {
			showForm(cmd)
		} else {
			val assignment = cmd.apply()
			RedirectForce(route(assignment))
		}
	}
}