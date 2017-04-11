package uk.ac.warwick.tabula.web.controllers.coursework.admin.assignments

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.assignments.{ArchiveAssignmentCommand, ArchiveAssignmentCommandInternal}
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/archive"))
class OldArchiveAssignmentController extends OldCourseworkController {

	@ModelAttribute("command") def model(module: Module, assignment: Assignment) =
		ArchiveAssignmentCommand(module, assignment)

	@RequestMapping(method = Array(GET, HEAD))
	def confirmation(@ModelAttribute("command") cmd: ArchiveAssignmentCommandInternal): Mav = {
		Mav(s"$urlPrefix/admin/assignments/archive").noLayoutIf(ajax)
	}

	@RequestMapping(method = Array(POST))
	def apply(@ModelAttribute("command") cmd: Appliable[Assignment]): Mav = {
		cmd.apply()
		Mav("ajax_success").noLayoutIf(ajax) // should be AJAX, otherwise you'll just get a terse success response.
	}

}