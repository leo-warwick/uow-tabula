package uk.ac.warwick.tabula.web.controllers.exams.exams.admin


import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.{InitBinder, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.exams.{AddExamCommand, ExamState, ModifiesExamMembership}
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController

@Controller
@RequestMapping(value = Array("/exams/exams/admin/module/{module}/{academicYear}/exams/new"))
class AddExamController extends ExamsController {

	type AddExamCommand = Appliable[Exam] with ExamState with ModifiesExamMembership with PopulateOnForm

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		 @PathVariable module: Module,
		 @PathVariable academicYear : AcademicYear) = AddExamCommand(mandatory(module), mandatory(academicYear))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@ModelAttribute("command") cmd: AddExamCommand): Mav = {
		cmd.populate()
		cmd.afterBind()
		render(cmd)
	}

	private def render(cmd: AddExamCommand) = {
		Mav("exams/exams/admin/new",
			"availableUpstreamGroups" -> cmd.availableUpstreamGroups,
			"linkedUpstreamAssessmentGroups" -> cmd.linkedUpstreamAssessmentGroups,
			"assessmentGroups" -> cmd.assessmentGroups,
			"department" -> cmd.module.adminDepartment
		).crumbs(
			Breadcrumbs.Exams.Home,
			Breadcrumbs.Exams.Department(cmd.module.adminDepartment, cmd.academicYear),
			Breadcrumbs.Exams.Module(cmd.module, cmd.academicYear)
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: AddExamCommand,
		errors: Errors
	): Mav = {
		cmd.afterBind()
		if (errors.hasErrors) {
			render(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.Exams.admin.module(cmd.module, cmd.academicYear))
		}
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}

}
