package uk.ac.warwick.tabula.web.controllers.coursework.admin.markingworkflows

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands.coursework.markingworkflows.{OldDeleteMarkingWorkflowCommand, DeleteMarkingWorkflowCommandState}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.web.controllers.coursework.admin.markingworkflows.OldDeleteMarkingWorkflowController.DeleteMarkingWorkflowCommand

object OldDeleteMarkingWorkflowController {
	type DeleteMarkingWorkflowCommand = Appliable[Unit]
		with SelfValidating with DeleteMarkingWorkflowCommandState
}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/department/{department}/markingworkflows/delete/{markingworkflow}"))
class OldDeleteMarkingWorkflowController extends OldCourseworkController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def cmd(@PathVariable department: Department, @PathVariable markingworkflow: MarkingWorkflow): DeleteMarkingWorkflowCommand =
		OldDeleteMarkingWorkflowCommand(department, markingworkflow)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: DeleteMarkingWorkflowCommand): Mav = {
		Mav(s"$urlPrefix/admin/markingworkflows/delete").noLayoutIf(ajax)
	}

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: DeleteMarkingWorkflowCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.admin.markingWorkflow.list(cmd.department))
		}
	}

}