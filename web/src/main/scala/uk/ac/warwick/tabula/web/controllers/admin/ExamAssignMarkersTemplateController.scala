package uk.ac.warwick.tabula.web.controllers.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.assignments.AssignMarkersTemplateCommand
import uk.ac.warwick.tabula.data.model.Exam
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.views.ExcelView

@Controller
@RequestMapping(value=Array("/exams/exams/admin/module/{module}/{academicYear}/exams/{exam}/assign-markers/template"))
class ExamAssignMarkersTemplateController extends ExamsController {

	@ModelAttribute("command")
	def command(@PathVariable exam: Exam) = AssignMarkersTemplateCommand(exam)

	@RequestMapping
	def getTemplate(@Valid @ModelAttribute("command") cmd: Appliable[ExcelView]): ExcelView = {
		cmd.apply()
	}

}
