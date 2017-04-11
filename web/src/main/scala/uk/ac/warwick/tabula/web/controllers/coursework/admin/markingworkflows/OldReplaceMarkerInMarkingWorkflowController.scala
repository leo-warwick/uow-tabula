package uk.ac.warwick.tabula.web.controllers.coursework.admin.markingworkflows

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.coursework.markingworkflows.OldReplaceMarkerInMarkingWorkflowCommand
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Department, MarkingWorkflow}
import uk.ac.warwick.tabula.coursework.web.{Routes => CourseworkRoutes}
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/department/{department}/markingworkflows/edit/{markingWorkflow}/replace"))
class OldReplaceMarkerInMarkingWorkflowController extends OldCourseworkController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def cmd(@PathVariable department: Department, @PathVariable markingWorkflow: MarkingWorkflow) =
		OldReplaceMarkerInMarkingWorkflowCommand(department, markingWorkflow)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[MarkingWorkflow], @PathVariable department: Department): Mav = {
		Mav(s"$urlPrefix/admin/markingworkflows/replace").crumbs(Breadcrumbs.Department(department))
	}

	@RequestMapping(method=Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[MarkingWorkflow], errors: Errors,
		@PathVariable department: Department,
		@PathVariable markingWorkflow: MarkingWorkflow
	): Mav = {
		if (errors.hasErrors) {
			form(cmd, department)
		} else {
			cmd.apply()
			Redirect(CourseworkRoutes.admin.markingWorkflow.edit(markingWorkflow))
		}
	}

}
