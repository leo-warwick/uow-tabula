package uk.ac.warwick.tabula.web.controllers.coursework.admin.markingworkflows

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands.coursework.markingworkflows.OldAddMarkingWorkflowCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.{Routes => CourseworkRoutes}
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.markingworkflows.MarkingWorkflowCommandState

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/department/{department}/markingworkflows/add"))
class OldAddMarkingWorkflowController extends OldCourseworkController {

	// tell @Valid annotation how to validate
	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def cmd(@PathVariable department: Department) = OldAddMarkingWorkflowCommand(department)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[MarkingWorkflow] with MarkingWorkflowCommandState): Mav = {
		Mav(s"$urlPrefix/admin/markingworkflows/add", "isExams" -> false).crumbs(Breadcrumbs.Department(cmd.department))
	}

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[MarkingWorkflow] with MarkingWorkflowCommandState, errors: Errors): Mav = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(CourseworkRoutes.admin.markingWorkflow.list(cmd.department))
		}
	}

}