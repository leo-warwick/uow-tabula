package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.{EditAssignmentDetailsCommand, _}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkBreadcrumbs

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/edit"))
class EditAssignmentDetailsController extends AbstractAssignmentController {

	type EditAssignmentDetailsCommand =  Appliable[Assignment] with EditAssignmentDetailsCommandState with PopulateOnForm
	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def createAssignmentDetailsCommand(@PathVariable assignment: Assignment) =
		EditAssignmentDetailsCommand(mandatory(assignment))

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: EditAssignmentDetailsCommand): Mav = {
		cmd.populate()
		showForm(cmd)
	}


	def showForm(cmd: EditAssignmentDetailsCommand): Mav = {
		val module = cmd.module
		Mav(s"$urlPrefix/admin/assignments/edit_assignment_details",
			"department" -> module.adminDepartment,
			"module" -> module
		).crumbs(CourseworkBreadcrumbs.Assignment.AssignmentManagement())
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.editAndAddFeedback, "action!=refresh", "action!=update, action=submit"))
	def submitAndAddFeedback(@Valid @ModelAttribute("command") cmd: EditAssignmentDetailsCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
		submit(cmd, errors, Routes.admin.assignment.createOrEditFeedback(assignment, editMode))

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.editAndEditDetails, "action!=refresh", "action!=update"))
	def saveAndExit(@ModelAttribute("command") cmd: EditAssignmentDetailsCommand, errors: Errors): Mav = {
		submit(cmd, errors, Routes.home)
	}

	private def submit(cmd: EditAssignmentDetailsCommand, errors: Errors, path: String) = {
		if (errors.hasErrors) showForm(cmd)
		else {
			cmd.apply()
			RedirectForce(path)
		}
	}
}
