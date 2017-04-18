package uk.ac.warwick.tabula.web.controllers.cm2.admin

import java.io.StringWriter
import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.cm2.assignments.SubmissionAndFeedbackCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.cm2.WorkflowItems
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.web.views.{CSVView, ExcelView, XmlView}
import uk.ac.warwick.util.csv.GoodCsvDocument



@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}"))
class SubmissionAndFeedbackExportController extends CourseworkController {

	@ModelAttribute("submissionAndFeedbackCommand")
	def command(@PathVariable assignment: Assignment): SubmissionAndFeedbackCommand.CommandType =
		SubmissionAndFeedbackCommand(assignment)


	@RequestMapping(Array("/export.csv"))
	def csv(@Valid @ModelAttribute("submissionAndFeedbackCommand") command: SubmissionAndFeedbackCommand.CommandType, module: Module, @PathVariable assignment: Assignment): CSVView = {
		val results = command.apply()

		val items = results.students
		val workflowItems: Seq[WorkflowItems] = for(elem <- items) yield elem.coursework

		val writer = new StringWriter
		val csvBuilder = new CSVBuilder(workflowItems, assignment, module)
		val doc = new GoodCsvDocument(csvBuilder, null)

		doc.setHeaderLine(true)
		csvBuilder.headers foreach (header => doc.addHeaderField(header))
		workflowItems foreach (item => doc.addLine(item))
		doc.write(writer)

		new CSVView(module.code + "-" + assignment.id + ".csv", writer.toString)
	}

	@RequestMapping(Array("/export.xml"))
	def xml(@Valid @ModelAttribute("submissionAndFeedbackCommand") command: SubmissionAndFeedbackCommand.CommandType, @PathVariable assignment: Assignment): XmlView = {
		val results = command.apply()

		val items = results.students
		val workflowItems: Seq[WorkflowItems] = for(elem <- items) yield elem.coursework

		new XmlView(new XMLBuilder(workflowItems, assignment, assignment.module).toXML, Some(assignment.module.code + "-" + assignment.id + ".xml"))
	}

	@RequestMapping(Array("/export.xlsx"))
	def xlsx(@Valid @ModelAttribute("submissionAndFeedbackCommand") command: SubmissionAndFeedbackCommand.CommandType, module: Module, @PathVariable assignment: Assignment): ExcelView = {
		val results = command.apply()

		val items = results.students
		val workflowItems: Seq[WorkflowItems] = for(elem <- items) yield elem.coursework

		val workbook = new ExcelBuilder(workflowItems, assignment, module).toXLSX

		new ExcelView(assignment.name + ".xlsx", workbook)
	}

}
