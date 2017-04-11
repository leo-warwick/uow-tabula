package uk.ac.warwick.tabula.web.controllers.coursework.admin

import java.io.StringWriter
import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.coursework.assignments.SubmissionAndFeedbackCommand
import uk.ac.warwick.tabula.commands.coursework.assignments.SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.coursework.{CourseworkFilter, CourseworkFilters}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{CSVView, ExcelView, XmlView}
import uk.ac.warwick.util.csv.GoodCsvDocument
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor

import scala.xml.Elem

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}"))
class OldSubmissionAndFeedbackController extends OldCourseworkController {

	var features: Features = Wire[Features]

	validatesSelf[SelfValidating]

	@ModelAttribute("submissionAndFeedbackCommand")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment): SubmissionAndFeedbackCommand.CommandType =
		SubmissionAndFeedbackCommand(module, assignment)

	@ModelAttribute("allFilters")
	def allFilters(@PathVariable assignment: Assignment): Seq[CourseworkFilter with Product with Serializable] =
		CourseworkFilters.AllFilters.filter(_.applies(assignment))

	@RequestMapping(Array("/list"))
	def list(@Valid @ModelAttribute("submissionAndFeedbackCommand") command: SubmissionAndFeedbackCommand.CommandType, errors: Errors, @PathVariable module: Module, @PathVariable assignment: Assignment): Mav = {
		module.adminDepartment.assignmentInfoView match {
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
	def summary(@Valid @ModelAttribute("submissionAndFeedbackCommand") command: SubmissionAndFeedbackCommand.CommandType, errors: Errors, @PathVariable module: Module, @PathVariable assignment: Assignment): Mav = {
		if (!features.assignmentProgressTable) Redirect(Routes.admin.assignment.submissionsandfeedback.table(assignment))
		else {
			if (errors.hasErrors) {
				Mav(s"$urlPrefix/admin/assignments/submissionsandfeedback/progress")
					.crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module), Breadcrumbs.Current(s"Assignment progress for ${assignment.name}"))
			} else {
				val results = command.apply()

				Mav(s"$urlPrefix/admin/assignments/submissionsandfeedback/progress",
					resultMap(results)
				).crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module), Breadcrumbs.Current(s"Assignment progress for ${assignment.name}"))
			}
		}
	}

	@RequestMapping(Array("/table"))
	def table(@Valid @ModelAttribute("submissionAndFeedbackCommand") command: SubmissionAndFeedbackCommand.CommandType, errors: Errors, @PathVariable module: Module, @PathVariable assignment: Assignment): Mav = {
		if (errors.hasErrors) {
			Mav(s"$urlPrefix/admin/assignments/submissionsandfeedback/list")
				.crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module), Breadcrumbs.Current(s"Assignment table for ${assignment.name}"))
		} else {
			val results = command.apply()

			Mav(s"$urlPrefix/admin/assignments/submissionsandfeedback/list",
				resultMap(results)
			).crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module), Breadcrumbs.Current(s"Assignment table for ${assignment.name}"))
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

	@RequestMapping(Array("/export.csv"))
	def csv(@Valid @ModelAttribute("submissionAndFeedbackCommand") command: SubmissionAndFeedbackCommand.CommandType, @PathVariable module: Module, @PathVariable assignment: Assignment): CSVView = {
		val results = command.apply()

		val items = results.students

		val writer = new StringWriter
		val csvBuilder = new CSVBuilder(items, assignment, module)
		val doc = new GoodCsvDocument(csvBuilder, null)

		doc.setHeaderLine(true)
		csvBuilder.headers foreach (header => doc.addHeaderField(header))
		items foreach (item => doc.addLine(item))
		doc.write(writer)

		new CSVView(module.code + "-" + assignment.id + ".csv", writer.toString)
	}

	@RequestMapping(Array("/export.xml"))
	def xml(@Valid @ModelAttribute("submissionAndFeedbackCommand") command: SubmissionAndFeedbackCommand.CommandType, @PathVariable module: Module, @PathVariable assignment: Assignment): XmlView = {
		val results = command.apply()

		val items = results.students

		new XmlView(new XMLBuilder(items, assignment, module).toXML, Some(module.code + "-" + assignment.id + ".xml"))
	}

	@RequestMapping(Array("/export.xlsx"))
	def xlsx(@Valid @ModelAttribute("submissionAndFeedbackCommand") command: SubmissionAndFeedbackCommand.CommandType, @PathVariable module: Module, @PathVariable assignment: Assignment): ExcelView = {
		val results = command.apply()

		val items = results.students

		val workbook = new ExcelBuilder(items, assignment, module).toXLSX

		new ExcelView(assignment.name + ".xlsx", workbook)
	}

	override def binding[A](binder: WebDataBinder, cmd: A) {
		binder.registerCustomEditor(classOf[CourseworkFilter], new AbstractPropertyEditor[CourseworkFilter] {
			override def fromString(name: String): CourseworkFilter = CourseworkFilters.of(name)
			override def toString(filter: CourseworkFilter): String = filter.getName
		})
	}

}