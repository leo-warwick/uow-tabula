package uk.ac.warwick.tabula.web.controllers.exams.exams.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.coursework.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.commands.exams.exams.{BulkAdjustmentCommand, BulkAdjustmentTemplateCommand}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Exam, Mark, Module}
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.helpers.SpreadsheetHelpers
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.views.ExcelView

@Controller
@RequestMapping(Array("/exams/exams/admin/module/{module}/{academicYear}/exams/{exam}/feedback/bulk-adjustment"))
class BulkAdjustmentController extends ExamsController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable exam: Exam) =
		BulkAdjustmentCommand(
			mandatory(exam),
			GenerateGradesFromMarkCommand(mandatory(module), mandatory(exam)),
			SpreadsheetHelpers,
			user
		)

	@RequestMapping(method = Array(GET, HEAD))
	def form: Mav = {
		Mav("exams/exams/admin/adjustments/bulk/form",
			"StudentIdHeader" -> BulkAdjustmentCommand.StudentIdHeader,
			"MarkHeader" -> BulkAdjustmentCommand.MarkHeader,
			"GradeHeader" -> BulkAdjustmentCommand.GradeHeader
		)
	}

	@RequestMapping(method = Array(POST))
	def upload(@Valid @ModelAttribute("command") cmd: Appliable[Seq[Mark]], errors: Errors): Mav = {
		if (errors.hasFieldErrors("file"))
			form
		else
			Mav("exams/exams/admin/adjustments/bulk/preview")
	}

	@RequestMapping(method = Array(POST), params = Array("confirmStep=true"))
	def confirm(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[Mark]], errors: Errors,
		@PathVariable exam: Exam
	): Mav = {
		if (errors.hasFieldErrors("defaultReason") || errors.hasFieldErrors("defaultComment")) {
			upload(cmd, errors)
		} else {
			cmd.apply()
			Redirect(Routes.Exams.admin.exam(exam))
		}
	}

}

@Controller
@RequestMapping(Array("/exams/exams/admin/module/{module}/{academicYear}/exams/{exam}/feedback/bulk-adjustment/template"))
class BulkAdjustmentTemplateController extends ExamsController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable exam: Exam): Appliable[ExcelView] =
		BulkAdjustmentTemplateCommand(mandatory(exam))

	@RequestMapping(method = Array(GET, HEAD))
	def home(@ModelAttribute("command") cmd: Appliable[ExcelView]): ExcelView = {
		cmd.apply()
	}

}
