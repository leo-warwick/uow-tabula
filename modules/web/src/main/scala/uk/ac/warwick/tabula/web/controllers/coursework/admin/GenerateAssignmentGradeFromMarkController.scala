package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.coursework.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.web.controllers.AbstractGenerateGradeFromMarkController

@Controller
@RequestMapping(Array("/coursework/admin/module/{module}/assignments/{assignment}/generate-grade"))
class GenerateAssignmentGradeFromMarkController extends AbstractGenerateGradeFromMarkController[Assignment] {

	@ModelAttribute("command")
	override def command(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		GenerateGradesFromMarkCommand(mandatory(module), mandatory(assignment))

}
