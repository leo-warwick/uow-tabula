package uk.ac.warwick.tabula.web.controllers.coursework.admin

import java.io.StringWriter

import scala.collection.JavaConversions.asScalaSet
import scala.collection.JavaConversions.seqAsJavaList
import org.joda.time.ReadableInstant
import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.helpers.DateTimeOrdering.orderedDateTime
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.commands.coursework.assignments.ListSubmissionsCommand
import uk.ac.warwick.tabula.commands.coursework.assignments.ListSubmissionsCommand.SubmissionListItem
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.views.CSVView
import uk.ac.warwick.util.csv.CSVLineWriter
import uk.ac.warwick.util.csv.GoodCsvDocument

import scala.collection.immutable.ListMap
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue

import scala.collection.AbstractSeq
import scala.xml.{Elem, Node}

/**
 * Download submissions metadata.
 */
@Profile(Array("cm1Enabled")) @Controller
class OldSubmissionsInfoController extends OldCourseworkController {

	val isoFormatter = DateFormats.IsoDateTime
	val csvFormatter = DateFormats.CSVDateTime

	def isoFormat(i: ReadableInstant): String = isoFormatter print i
	def csvFormat(i: ReadableInstant): String = csvFormatter print i

	var checkIndex = true

	@ModelAttribute("command") def command(@PathVariable module: Module, @PathVariable assignment: Assignment): ListSubmissionsCommand.CommandType =
		ListSubmissionsCommand(module, assignment)

	@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/submissions.xml"), method = Array(GET, HEAD))
	def xml(@ModelAttribute("command") command: ListSubmissionsCommand.CommandType, @PathVariable assignment: Assignment): Elem = {
		command.checkIndex = checkIndex

		val items = command.apply().sortBy { _.submission.submittedDate }.reverse

		<submissions>
			{ assignmentElement(assignment) }
			{ items map submissionElement }
		</submissions>
	}

	def assignmentElement(assignment: Assignment): Elem =
		<assignment id={ assignment.id } open-date={ isoFormat(assignment.openDate) } close-date={ isoFormat(assignment.closeDate) }/>

	def submissionElement(item: SubmissionListItem): Elem =
		<submission
				id={ item.submission.id }
				submission-time={ isoFormat(item.submission.submittedDate) }
				university-id={ item.submission.universityId.orNull }
				usercode={ item.submission.usercode }
				downloaded={ item.downloaded.toString }>
			{ item.submission.values map fieldElement(item) }
		</submission>

	def fieldElement(item: SubmissionListItem)(value: SavedFormValue): AbstractSeq[Node] with Seq[Node] =
		if (value.hasAttachments)
			<field name={ value.name }>
				{
					value.attachments map { file =>
						<file name={ file.name } zip-path={ item.submission.zipFileName(file) }/>
					}
				}
			</field>
		else if (value.value != null)
			<field name={ value.name } value={ value.value }/>
		else
			Nil //empty Node seq, no element


	@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/submissions.csv"), method = Array(GET, HEAD))
	def csv(@ModelAttribute("command") command: ListSubmissionsCommand.CommandType): CSVView = {
		command.checkIndex = checkIndex

		val items = command.apply().sortBy { _.submission.submittedDate }.reverse
		val writer = new StringWriter
		val csvBuilder = new SubmissionsCSVBuilder(items)
		val doc = new GoodCsvDocument(csvBuilder, null)

		doc.setHeaderLine(true)
		csvBuilder.headers foreach (header => doc.addHeaderField(header))
		items foreach (item => doc.addLine(item))
		doc.write(writer)

		new CSVView("submissions.csv", writer.toString)
	}

	class SubmissionsCSVBuilder(items:Seq[SubmissionListItem]) extends CSVLineWriter[SubmissionListItem] {
		val headers: Seq[String] = {
			var extraFields = Set[String]()

			// have to iterate all items to ensure complete field coverage. bleh :(
			items foreach ( item => extraFields = extraFields ++ extraFieldData(item).keySet )

			// return core headers in insertion order (make it easier for parsers), followed by alpha-sorted field headers
			coreFields ++ extraFields.toList.sorted
		}

		def getNoOfColumns(item:SubmissionListItem): Int = headers.size

		def getColumn(item:SubmissionListItem, i:Int): String = {
			itemData(item).getOrElse(headers.get(i), "")
		}
	}

	private def itemData(item: SubmissionListItem) = coreData(item) ++ extraFieldData(item)

	// This Seq specifies the core field order
	private def coreFields = Seq("submission-id", "submission-time", "university-id", "assignment-id", "downloaded")

	private def coreData(item: SubmissionListItem) = Map(
		"submission-id" -> item.submission.id,
		"submission-time" -> csvFormat(item.submission.submittedDate),
		"university-id" -> item.submission.universityId.orNull,
		"assignment-id" -> item.submission.assignment.id,
		"downloaded" -> item.downloaded.toString.toLowerCase
	)

	private def extraFieldData(item: SubmissionListItem) = {
		var fieldDataMap = ListMap[String, String]()

		item.submission.values foreach ( value =>
			if (value.hasAttachments)
				value.attachments foreach {file => {
					fieldDataMap += (value.name + "-name") -> file.name
					fieldDataMap += (value.name + "-zip-path") -> item.submission.zipFileName(file)
				}}
			else if (value.value != null)
				fieldDataMap += value.name -> value.value
		)

		fieldDataMap
	}
}