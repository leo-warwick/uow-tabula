package uk.ac.warwick.tabula.web.controllers.exams.exams.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.coursework.feedback._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController

@Controller
@RequestMapping(Array("/exams/exams/admin/module/{module}/{academicYear}/exams/{exam}/feedback/adjustments"))
class ExamFeedbackAdjustmentsListController extends ExamsController {

	type FeedbackAdjustmentListCommand = Appliable[Seq[StudentInfo]]

	@ModelAttribute("listCommand")
	def listCommand(@PathVariable exam: Exam): FeedbackAdjustmentListCommand =
		FeedbackAdjustmentListCommand(mandatory(exam))

	@RequestMapping
	def list(
		@PathVariable exam: Exam,
		@ModelAttribute("listCommand") listCommand: FeedbackAdjustmentListCommand
	): Mav = {
		val (studentInfo, noFeedbackStudentInfo) = listCommand.apply().partition { _.feedback.isDefined }

		Mav("exams/exams/admin/adjustments/list",
			"studentInfo" -> studentInfo,
			"noFeedbackStudentInfo" -> noFeedbackStudentInfo,
			"isGradeValidation" -> exam.module.adminDepartment.assignmentGradeValidation
		).crumbs(
			Breadcrumbs.Exams.Home,
			Breadcrumbs.Exams.Department(exam.module.adminDepartment, exam.academicYear),
			Breadcrumbs.Exams.Module(exam.module, exam.academicYear)
		)
	}
}

@Controller
@RequestMapping(Array("/exams/exams/admin/module/{module}/{academicYear}/exams/{exam}/feedback/adjustments/{student}"))
class ExamFeedbackAdjustmentsController extends ExamsController with AutowiringProfileServiceComponent {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def formCommand(@PathVariable module: Module, @PathVariable exam: Exam, @PathVariable student: User, submitter: CurrentUser) =
		FeedbackAdjustmentCommand(mandatory(exam), student, submitter, GenerateGradesFromMarkCommand(mandatory(module), mandatory(exam)))

	@RequestMapping(method=Array(GET))
	def showForm(@PathVariable exam: Exam): Mav = {
		Mav("exams/exams/admin/adjustments/student",
			"isGradeValidation" -> exam.module.adminDepartment.assignmentGradeValidation
		).noLayout()
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Feedback] with FeedbackAdjustmentCommandState,
		errors: Errors,
		@PathVariable exam: Exam
	): Mav = {
		if (errors.hasErrors) {
			showForm(exam)
		} else {
			cmd.apply()
			Mav("ajax_success").noLayout()
		}
	}

}