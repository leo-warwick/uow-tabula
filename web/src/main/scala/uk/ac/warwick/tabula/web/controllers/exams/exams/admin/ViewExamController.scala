package uk.ac.warwick.tabula.web.controllers.exams.exams.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.exams.exams.{ViewExamCommand, ViewExamCommandResult}
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController

@Controller
@RequestMapping(Array("/exams/exams/admin/module/{module}/{academicYear}/exams/{exam}"))
class ViewExamController extends ExamsController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable academicYear: AcademicYear, @PathVariable exam: Exam) =
		ViewExamCommand(mandatory(module), mandatory(academicYear), mandatory(exam))

	@RequestMapping(method = Array(HEAD, GET))
	def home(
		@ModelAttribute("command") cmd: Appliable[ViewExamCommandResult],
		@PathVariable module: Module,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		val result = cmd.apply()
		Mav("exams/exams/admin/view",
			"students" -> result.students,
			"seatNumberMap" -> result.seatNumberMap,
			"feedbackMap" -> result.feedbackMap,
			"sitsStatusMap" -> result.sitsStatusMap
		).crumbs(
			Breadcrumbs.Exams.Department(module.adminDepartment, academicYear),
			Breadcrumbs.Exams.Module(module, academicYear)
		)
	}

}
