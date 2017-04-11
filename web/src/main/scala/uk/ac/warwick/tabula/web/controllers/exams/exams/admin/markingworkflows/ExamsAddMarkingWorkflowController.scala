package uk.ac.warwick.tabula.web.controllers.exams.exams.admin.markingworkflows

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.coursework.markingworkflows.{OldAddMarkingWorkflowCommand, MarkingWorkflowCommandState}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Department, MarkingWorkflow}
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController

@Controller
@RequestMapping(value=Array("/exams/exams/admin/department/{department}/markingworkflows/add"))
class ExamsAddMarkingWorkflowController extends ExamsController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def cmd(@PathVariable department: Department) = OldAddMarkingWorkflowCommand(department)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[MarkingWorkflow] with MarkingWorkflowCommandState): Mav = {
		Mav("exams/exams/admin/markingworkflows/add", "isExams" -> true)
	}

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[MarkingWorkflow] with MarkingWorkflowCommandState, errors: Errors): Mav = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.Exams.admin.markingWorkflow.list(cmd.department))
		}
	}

}
