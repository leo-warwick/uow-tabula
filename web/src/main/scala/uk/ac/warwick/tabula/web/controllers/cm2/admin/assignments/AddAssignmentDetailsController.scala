package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.{CreateAssignmentDetailsCommand, CreateAssignmentDetailsCommandInternal, CreateAssignmentDetailsCommandState}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkBreadcrumbs


@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/{module}/assignments/new"))
class AddAssignmentDetailsController extends AbstractAssignmentController {

	type CreateAssignmentDetailsCommand = CreateAssignmentDetailsCommandInternal with Appliable[Assignment] with CreateAssignmentDetailsCommandState
	validatesSelf[SelfValidating]

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
			"module" -> module
		).crumbs(CourseworkBreadcrumbs.Assignment.AssignmentManagement())
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddFeedback, "action!=refresh", "action!=update, action=submit"))
	def submitAndAddFeedback(@Valid @ModelAttribute("command") cmd: CreateAssignmentDetailsCommand, errors: Errors): Mav = {
		if (errors.hasErrors) showForm(cmd)
		else {
			val assignment = cmd.apply()
			RedirectForce(Routes.admin.assignment.createOrEditFeedback(assignment, createMode))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddDetails, "action!=refresh", "action!=update"))
	def saveAndExit(@Valid @ModelAttribute("command") cmd: CreateAssignmentDetailsCommand, errors: Errors): Mav = {
		if (errors.hasErrors) showForm(cmd)
		else {
			cmd.apply()
			RedirectForce(Routes.home)
		}
	}

}
