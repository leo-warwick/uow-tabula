package uk.ac.warwick.tabula.web.controllers.coursework.admin.markingworkflows

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.markingworkflows.{ListMarkingWorkflowCommand, ListMarkingWorkflowCommandResult}
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/department/{department}/markingworkflows"))
class OldListMarkingWorkflowController extends OldCourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable department: Department) = ListMarkingWorkflowCommand(department, isExam = false)

	@RequestMapping
	def list(@ModelAttribute("command") cmd: Appliable[Seq[ListMarkingWorkflowCommandResult]], @PathVariable department: Department): Mav = {
		Mav(s"$urlPrefix/admin/markingworkflows/list",
		    "markingWorkflowInfo" -> cmd.apply(),
				"isExams" -> false
		).crumbs(Breadcrumbs.Department(department))
	}

}