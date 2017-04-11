package uk.ac.warwick.tabula.web.controllers.cm2.admin.markingworkflows

import javax.validation.Valid

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.cm2.markingworkflows.{ReplaceMarkerCommand, ReplaceMarkerState}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.web.{Mav, Routes}


@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/{academicYear}/markingworkflows/{workflow}/replace"))
class ReplaceMarkerController extends CM2MarkingWorkflowController {

	var messageSource: MessageSource = Wire.auto[MessageSource]

	type ReplaceMarkerCommand = Appliable[CM2MarkingWorkflow] with ReplaceMarkerState

	validatesSelf[SelfValidating]

	@ModelAttribute("replaceMarkerCommand")
	def command(@PathVariable department: Department, @PathVariable workflow: CM2MarkingWorkflow) =
		ReplaceMarkerCommand(mandatory(department), mandatory(workflow))

	@RequestMapping(method=Array(HEAD,GET))
	def showForm(
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable workflow: CM2MarkingWorkflow,
		@ModelAttribute("replaceMarkerCommand") cmd: ReplaceMarkerCommand,
		errors: Errors): Mav = {
		commonCrumbs(
			Mav(s"$urlPrefix/admin/workflows/replace_marker", Map(
				"workflow" -> workflow
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
		@Valid @ModelAttribute("replaceMarkerCommand") cmd: ReplaceMarkerCommand,
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
