package uk.ac.warwick.tabula.web.controllers.cm2.admin.markingworkflows

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.cm2.markingworkflows.{EditMarkingWorkflowCommand, EditMarkingWorkflowState}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.web.{Mav, Routes}


@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/{academicYear}/markingworkflows/{workflow}/edit"))
class EditMarkingWorkflowController extends CM2MarkingWorkflowController {

	type EditMarkingWorkflowCommand = Appliable[CM2MarkingWorkflow] with EditMarkingWorkflowState

	validatesSelf[SelfValidating]

	@ModelAttribute("editMarkingWorkflowCommand")
	def command(
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable workflow: CM2MarkingWorkflow
	) = EditMarkingWorkflowCommand(mandatory(department), mandatory(academicYear), mandatory(workflow))

	@RequestMapping(method=Array(HEAD,GET))
	def showForm(
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable workflow: CM2MarkingWorkflow,
		@ModelAttribute("editMarkingWorkflowCommand") cmd: EditMarkingWorkflowCommand,
		errors: Errors): Mav = {
		commonCrumbs(
			Mav(s"$urlPrefix/admin/workflows/edit_workflow", Map(
				"workflow" -> workflow,
				"canDeleteMarkers" -> workflow.canDeleteMarkers
			)),
			department,
			academicYear
		)
	}

	@RequestMapping(method=Array(POST))
	def submitForm(
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable workflow: CM2MarkingWorkflow,
		@Valid @ModelAttribute("editMarkingWorkflowCommand") cmd: EditMarkingWorkflowCommand,
		errors: Errors
	): Mav = {
		if (errors.hasErrors) {
			showForm(department, academicYear, workflow, cmd, errors)
		} else {
			cmd.apply()
			Redirect(Routes.cm2.admin.workflows(department, academicYear))
		}
	}

}