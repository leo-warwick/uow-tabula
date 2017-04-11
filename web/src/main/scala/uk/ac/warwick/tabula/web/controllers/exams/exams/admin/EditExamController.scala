package uk.ac.warwick.tabula.web.controllers.exams.exams.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.{InitBinder, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.exams.exams.{EditExamCommand, EditExamCommandState, ModifiesExamMembership, PopulateEditExamCommand}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating, UpstreamGroup, UpstreamGroupPropertyEditor}
import uk.ac.warwick.tabula.data.model.Exam
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController

@Controller
@RequestMapping(value = Array("/exams/exams/admin/module/{module}/{academicYear}/exams/{exam}/edit"))
class EditExamController extends ExamsController {

	type EditExamCommand = Appliable[Exam] with EditExamCommandState with ModifiesExamMembership with PopulateEditExamCommand

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable exam : Exam) = EditExamCommand(mandatory(exam))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@ModelAttribute("command") cmd: EditExamCommand): Mav = {
		cmd.populateGroups(cmd.exam)
		cmd.afterBind()

		render(cmd)
	}

	private def render(cmd: EditExamCommand) = {
		Mav("exams/exams/admin/edit",
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
		@Valid @ModelAttribute("command") cmd: EditExamCommand,
		errors: Errors
	): Mav = {
			cmd.afterBind()
			if (errors.hasErrors) {
				render(cmd)
			} else {
				cmd.apply()
				Redirect(Routes.Exams.admin.module(cmd.exam.module, cmd.exam.academicYear))
			}
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}
}
