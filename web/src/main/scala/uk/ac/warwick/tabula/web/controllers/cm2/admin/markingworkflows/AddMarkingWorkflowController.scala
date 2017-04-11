package uk.ac.warwick.tabula.web.controllers.cm2.admin.markingworkflows

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.cm2.markingworkflows.{AddMarkingWorkflowCommand, AddMarkingWorkflowState}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.markingworkflow.{CM2MarkingWorkflow, MarkingWorkflowType}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.{Mav, Routes}


@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/{academicYear}/markingworkflows/add"))
class AddMarkingWorkflowController extends CM2MarkingWorkflowController {

	type AddMarkingWorkflowCommand = Appliable[CM2MarkingWorkflow] with AddMarkingWorkflowState

	validatesSelf[SelfValidating]

	@ModelAttribute("addMarkingWorkflowCommand")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		AddMarkingWorkflowCommand(mandatory(department), mandatory(academicYear), isResuable = true)

	@RequestMapping(method=Array(HEAD,GET))
	def showForm(
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@ModelAttribute("addMarkingWorkflowCommand") cmd: AddMarkingWorkflowCommand,
		errors: Errors): Mav = {
		commonCrumbs(
			Mav(s"$urlPrefix/admin/workflows/add_workflow", Map(
				"availableWorkflows" -> MarkingWorkflowType.values.sorted,
				"canDeleteMarkers" -> true
			)),
			department,
			academicYear
		)
	}

	@RequestMapping(method=Array(POST))
	def submitForm(
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@Valid @ModelAttribute("addMarkingWorkflowCommand") cmd: AddMarkingWorkflowCommand,
		errors: Errors
	): Mav = {
		if (errors.hasErrors) {
			showForm(department, academicYear, cmd, errors)
		} else {
			cmd.apply()
			Redirect(Routes.cm2.admin.workflows(department, academicYear))
		}
	}

}
