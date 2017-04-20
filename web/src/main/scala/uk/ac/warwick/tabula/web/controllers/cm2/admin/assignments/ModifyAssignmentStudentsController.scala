package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.{ModifyAssignmentStudentsCommand, _}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkBreadcrumbs

abstract class AbstractAssignmentStudentsController extends AbstractAssignmentController {

	type ModifyAssignmentStudentsCommand = Appliable[Assignment] with ModifyAssignmentStudentsCommandState with ModifiesAssignmentMembership with PopulateOnForm

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def modifyAssignmentFeedbackCommand(@PathVariable assignment: Assignment) =
		ModifyAssignmentFeedbackCommand(mandatory(assignment))

	def showForm(form: ModifyAssignmentStudentsCommand, mode: String): Mav = {
		val module = form.module
		Mav(s"$urlPrefix/admin/assignments/assignment_student_details",
			"department" -> module.adminDepartment,
			"module" -> module,
			"linkedUpstreamAssessmentGroups" -> form.linkedUpstreamAssessmentGroups,
			"availableUpstreamGroups" -> form.availableUpstreamGroups,
			"assessmentGroups" -> form.assessmentGroups,
			"academicYear" -> form.assignment.academicYear,
			"mode" -> mode
		).crumbs(CourseworkBreadcrumbs.Assignment.AssignmentManagement())
	}

	def submit(cmd: ModifyAssignmentStudentsCommand, errors: Errors, path: String, mode: String) = {
		cmd.afterBind()
		if (errors.hasErrors) showForm(cmd, mode)
		else {
			cmd.apply()
			RedirectForce(path)
		}
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}
}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}"))
class ModifyAssignmentStudentsController extends AbstractAssignmentStudentsController {

	@ModelAttribute("command") def command(@PathVariable assignment: Assignment): ModifyAssignmentStudentsCommand =
	ModifyAssignmentStudentsCommand(mandatory(assignment))

	@RequestMapping(method = Array(GET), value = Array("/new/students"))
	def form(
		@PathVariable("assignment") assignment: Assignment,
		@ModelAttribute("command") cmd: ModifyAssignmentStudentsCommand
	): Mav =
		getStudents(cmd, createMode)

	@RequestMapping(method = Array(GET), value = Array("/edit/students"))
	def formEdit(
		@PathVariable("assignment") assignment: Assignment,
		@ModelAttribute("command") cmd: ModifyAssignmentStudentsCommand
	): Mav =
		getStudents(cmd, editMode)


	private def getStudents(cmd: ModifyAssignmentStudentsCommand, mode: String): Mav = {
		cmd.afterBind()
		cmd.populate()
		showForm(cmd, mode)
	}

	@RequestMapping(method = Array(POST), value = Array("/new/students"), params = Array(ManageAssignmentMappingParameters.createAndAddMarkers, "action!=refresh", "action!=update"))
	def submitAndAddFeedback(@Valid @ModelAttribute("command") cmd: ModifyAssignmentStudentsCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
		submit(cmd, errors, Routes.admin.assignment.createOrEditMarkers(assignment, createMode), createMode)

	@RequestMapping(method = Array(POST), value = Array("/new/students"), params = Array(ManageAssignmentMappingParameters.createAndAddStudents, "action!=refresh", "action!=update"))
	def saveAndExit(@Valid @ModelAttribute("command") cmd: ModifyAssignmentStudentsCommand, errors: Errors): Mav = {
		submit(cmd, errors, Routes.home, createMode)
	}

	@RequestMapping(method = Array(POST), value = Array("/edit/students"), params = Array(ManageAssignmentMappingParameters.editAndAddMarkers, "action!=refresh", "action!=update"))
	def submitAndAddFeedbackForEdit(@Valid @ModelAttribute("command") cmd: ModifyAssignmentStudentsCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
		submit(cmd, errors, Routes.admin.assignment.createOrEditMarkers(assignment, editMode), editMode)


	@RequestMapping(method = Array(POST), value = Array("/edit/students"), params = Array(ManageAssignmentMappingParameters.editAndAddStudents, "action!=refresh", "action!=update"))
	def saveAndExitForEdit(@Valid @ModelAttribute("command") cmd: ModifyAssignmentStudentsCommand, errors: Errors): Mav = {
		submit(cmd, errors, Routes.home, editMode)
	}

}