package uk.ac.warwick.tabula.web.controllers.exams.exams.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.coursework.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.web.controllers.AbstractGenerateGradeFromMarkController

@Controller
@RequestMapping(Array("/exams/exams/admin/module/{module}/exams/{exam}/generate-grade"))
class GenerateExamGradeFromMarkController extends AbstractGenerateGradeFromMarkController[Exam] {

	@ModelAttribute("command")
	override def command(@PathVariable module: Module, @PathVariable exam: Exam) =
		GenerateGradesFromMarkCommand(mandatory(module), mandatory(exam))

}
