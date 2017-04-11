package uk.ac.warwick.tabula.web.controllers.exams.exams.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.exams.exams.ReleaseExamForMarkingCommand
import uk.ac.warwick.tabula.data.model.{Exam, Feedback, Module}
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(value = Array("/exams/exams/admin/module/{module}/{academicYear}/exams/{exam}/release-for-marking"))
class ReleaseExamForMarkingController extends ExamsController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable exam: Exam) =
		ReleaseExamForMarkingCommand (
			mandatory(module),
			mandatory(exam),
			user
		)

	@RequestMapping(method = Array(GET))
	def form(@PathVariable module: Module, @PathVariable exam: Exam): Mav = {
		Mav("exams/exams/admin/release").crumbs(
			Breadcrumbs.Exams.Home,
			Breadcrumbs.Exams.Department(module.adminDepartment, exam.academicYear),
			Breadcrumbs.Exams.Module(module, exam.academicYear)
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("command") cmd: Appliable[Seq[Feedback]], @PathVariable exam: Exam): Mav = {
		cmd.apply()
		Redirect(Routes.Exams.admin.module(exam.module, exam.academicYear))
	}

}
