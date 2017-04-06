package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.cm2.assignments.SubmissionAndFeedbackCommand
import uk.ac.warwick.tabula.commands.cm2.assignments.SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.cm2.{Cm2Filter, Cm2Filters}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.{CourseworkBreadcrumbs, CourseworkController}
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}"))
class SubmissionAndFeedbackController extends CourseworkController {

	var features: Features = Wire[Features]

	validatesSelf[SelfValidating]

	@ModelAttribute("submissionAndFeedbackCommand")
	def command(@PathVariable assignment: Assignment): SubmissionAndFeedbackCommand.CommandType =
		SubmissionAndFeedbackCommand(assignment)

	@ModelAttribute("allFilters")
	def allFilters(@PathVariable assignment: Assignment): Seq[Cm2Filter with Product with Serializable] =
		Cm2Filters.AllFilters.filter(_.applies(assignment))

	@RequestMapping(Array("/list"))
	def list(@Valid @ModelAttribute("submissionAndFeedbackCommand") command: SubmissionAndFeedbackCommand.CommandType, errors: Errors, @PathVariable assignment: Assignment): Mav = {
		assignment.module.adminDepartment.assignmentInfoView match {
			case Assignment.Settings.InfoViewType.Summary =>
				Redirect(Routes.admin.assignment.submissionsandfeedback.summary(assignment))
			case Assignment.Settings.InfoViewType.Table =>
				Redirect(Routes.admin.assignment.submissionsandfeedback.table(assignment))
			case _ => // default
				if (features.assignmentProgressTableByDefault)
					Redirect(Routes.admin.assignment.submissionsandfeedback.summary(assignment))
				else
					Redirect(Routes.admin.assignment.submissionsandfeedback.table(assignment))
		}
	}

	@RequestMapping(Array("/summary"))
	def summary(@Valid @ModelAttribute("submissionAndFeedbackCommand") command: SubmissionAndFeedbackCommand.CommandType, errors: Errors, @PathVariable assignment: Assignment): Mav = {
		if (!features.assignmentProgressTable) Redirect(Routes.admin.assignment.submissionsandfeedback.table(assignment))
		else {
			if (errors.hasErrors) {

				Mav(s"$urlPrefix/admin/assignments/submissionsandfeedback/progress",
					"module" -> assignment.module,
					"department" -> assignment.module.adminDepartment
				).crumbs(CourseworkBreadcrumbs.SubmissionsAndFeedback.SubmissionsAndFeedbackManagement(assignment))

			} else {

				val results = command.apply()

				Mav(s"$urlPrefix/admin/assignments/submissionsandfeedback/progress",
					"module" -> assignment.module,
					"department" -> assignment.module.adminDepartment,
					"results" ->	resultMap(results)
				).crumbs(CourseworkBreadcrumbs.SubmissionsAndFeedback.SubmissionsAndFeedbackManagement(assignment))
			}
	}
	}

	@RequestMapping(Array("/table"))
	def table(@Valid @ModelAttribute("submissionAndFeedbackCommand") command: SubmissionAndFeedbackCommand.CommandType, errors: Errors, @PathVariable assignment: Assignment): Mav = {
		if (errors.hasErrors) {

			Mav(s"$urlPrefix/admin/assignments/submissionsandfeedback/list",
				"department" -> assignment.module.adminDepartment
			).crumbs(CourseworkBreadcrumbs.SubmissionsAndFeedback.SubmissionsAndFeedbackManagement(assignment))

		} else {

			val results = command.apply()
			Mav(s"$urlPrefix/admin/assignments/submissionsandfeedback/list",
				"department" -> assignment.module.adminDepartment,
				"results" ->	resultMap(results)
			).crumbs(CourseworkBreadcrumbs.SubmissionsAndFeedback.SubmissionsAndFeedbackManagement(assignment))

		}
	}

	def resultMap(results: SubmissionAndFeedbackResults): Map[String, Any] = {
		Map("students" -> results.students,
			"whoDownloaded" -> results.whoDownloaded,
			"stillToDownload" -> results.stillToDownload,
			"hasPublishedFeedback" -> results.hasPublishedFeedback,
			"hasOriginalityReport" -> results.hasOriginalityReport,
			"mustReleaseForMarking" -> results.mustReleaseForMarking)
	}

	override def binding[A](binder: WebDataBinder, cmd: A) {
		binder.registerCustomEditor(classOf[Cm2Filter], new AbstractPropertyEditor[Cm2Filter] {
			override def fromString(name: String): Cm2Filter = Cm2Filters.of(name)
			override def toString(filter: Cm2Filter): String = filter.getName
		})
	}
}
