package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.apache.commons.lang3.text.WordUtils
import org.apache.poi.hssf.usermodel.HSSFDataFormat
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import org.joda.time.ReadableInstant
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.ListSubmissionsCommand._
import uk.ac.warwick.tabula.commands.cm2.assignments.SubmissionAndFeedbackCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.{CurrentUser, DateFormats}
import uk.ac.warwick.util.csv.CSVLineWriter

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.xml._

class XMLBuilder(val items: Seq[Student], val assignment: Assignment, val module: Module) extends SubmissionAndFeedbackExport {

	var topLevelUrl: String = Wire.property("${toplevel.url}")

	// Pimp XML elements to allow a map mutator for attributes
	implicit class PimpedElement(elem: Elem) {
		def %(attrs:Map[String,Any]): Elem = {
			val seq = for( (n,v) <- attrs ) yield new UnprefixedAttribute(n, toXMLString(Some(v)), Null)
			(elem /: seq) ( _ % _ )
		}
	}

	def toXMLString(value: Option[Any]): String = value match {
		case Some(o: Option[Any]) => toXMLString(o)
		case Some(i: ReadableInstant) => isoFormat(i)
		case Some(b: Boolean) => b.toString.toLowerCase
		case Some(i: Int) => i.toString
		case Some(s: String) => s
		case Some(other) => other.toString
		case None => ""
	}

	def toXML: Elem = {
		<assignment>
			<generic-feedback>
				{ assignment.genericFeedback }
			</generic-feedback>
			<students>
				{ items map studentElement }
			</students>
		</assignment> % assignmentData
	}

	def studentElement(item: Student): Elem = {
		<student>
			{
				<submission>
					{
						item.cm2.enhancedSubmission map { item => item.submission.values.asScala.toSeq map fieldElement(item) } getOrElse Nil
					}
				</submission> % submissionData(item) % submissionStatusData(item)
			}
			{ <marking /> % markerData(item, assignment) % plagiarismData(item) }
			{
				<feedback>
					{ item.cm2.enhancedFeedback.flatMap { _.feedback.comments }.orNull }
				</feedback> % feedbackData(item)
			}
			{ <adjustment /> % adjustmentData(item) }
		</student> % identityData(item)
	}

	private def markerData(item: Student, assignment: Assignment): Map[String, Any] = {
		if (assignment.markingWorkflow != null) markerData(item)
		else Map()
	}

	def fieldElement(item: SubmissionListItem)(value: SavedFormValue): Seq[Node] =
		if (value.hasAttachments)
			<field name={ value.name }>
				{
					value.attachments.asScala map { file =>
						<file name={ file.name } zip-path={ item.submission.zipFileName(file) }/>
					}
				}
			</field>
		else if (value.value != null)
			<field name={ value.name }>
				{ value.value }
			</field>
		else
			Nil //empty Node seq, no element
}

class CSVBuilder(val items: Seq[Student], val assignment: Assignment, val module: Module)
	extends CSVLineWriter[Student] with SubmissionAndFeedbackSpreadsheetExport {

	var topLevelUrl: String = Wire.property("${toplevel.url}")

	def getNoOfColumns(item:Student): Int = headers.size

	def getColumn(item:Student, i:Int): String = formatData(itemData(item).get(headers(i)))
}

class ExcelBuilder(val items: Seq[Student], val assignment: Assignment, val module: Module) extends SubmissionAndFeedbackSpreadsheetExport {

	var topLevelUrl: String = Wire.property("${toplevel.url}")

	def toXLSX: XSSFWorkbook = {
		val workbook = new XSSFWorkbook()
		val sheet = generateNewSheet(workbook)

		items foreach { addRow(sheet)(_) }

		formatWorksheet(sheet)
		workbook
	}

	def generateNewSheet(workbook: XSSFWorkbook): XSSFSheet = {
		val sheet = workbook.createSheet(module.code.toUpperCase + " - " + safeAssignmentName)

		def formatHeader(header: String) =
			WordUtils.capitalizeFully(header.replace('-', ' '))

		// add header row
		val headerRow = sheet.createRow(0)
		headers.zipWithIndex foreach {
			case (header, index) => headerRow.createCell(index).setCellValue(formatHeader(header))
		}
		sheet
	}

	def addRow(sheet: XSSFSheet)(item: Student) {
		val plainCellStyle = {
			val cs = sheet.getWorkbook.createCellStyle()
			cs.setDataFormat(HSSFDataFormat.getBuiltinFormat("@"))
			cs
		}

		val dateTimeStyle = {
			val cs = sheet.getWorkbook.createCellStyle()
			val df = sheet.getWorkbook.createDataFormat()
			cs.setDataFormat(df.getFormat("d-mmm-yy h:mm:ss"))
			cs
		}

		val row = sheet.createRow(sheet.getLastRowNum + 1)
		headers.zipWithIndex foreach {
			case (header, index) =>
				val cell = row.createCell(index)

				if (index == 0) {
					// University IDs have leading zeros and Excel would normally remove them.
					// Set a manual data format to remove this possibility
					cell.setCellStyle(plainCellStyle)
				}

				itemData(item).get(header) match {
					case Some(i: ReadableInstant) =>
						cell.setCellValue(i.toInstant.toDate)
						cell.setCellStyle(dateTimeStyle)
					case Some(b: Boolean) => cell.setCellValue(b)
					case Some(i: Int) => cell.setCellValue(i)
					case Some(other) => cell.setCellValue(other.toString)
					case None => ""
				}
		}
	}

	def formatWorksheet(sheet: XSSFSheet): Unit = {
	    (0 to headers.size) foreach sheet.autoSizeColumn
	}

	// trim the assignment name down to 20 characters. Excel sheet names must be 31 chars or less so
	val trimmedAssignmentName: String = {
		if (assignment.name.length > 20)
			assignment.name.substring(0, 20)
		else
			assignment.name
	}

	// util to replace unsafe characters with spaces
	val safeAssignmentName: String = WorkbookUtil.createSafeSheetName(trimmedAssignmentName)
}

trait SubmissionAndFeedbackSpreadsheetExport extends SubmissionAndFeedbackExport {
	val items: Seq[Student]

	val csvFormatter = DateFormats.CSVDateTime
	def csvFormat(i: ReadableInstant): String = csvFormatter print i

	val headers: Seq[String] = {
		var extraFields = Set[String]()

		// have to iterate all items to ensure complete field coverage. bleh :(
		items foreach ( item => extraFields = extraFields ++ extraFieldData(item).keySet )

		// return core headers in insertion order (make it easier for parsers), followed by alpha-sorted field headers
		prefix(identityFields, "student") ++
			prefix(submissionFields, "submission") ++
			prefix(extraFields.toList.sorted, "submission") ++
			prefix(submissionStatusFields, "submission") ++
			(if (assignment.markingWorkflow != null) prefix(markerFields, "marking") else Seq()) ++
			prefix(plagiarismFields, "marking") ++
			prefix(feedbackFields, "feedback") ++
			prefix(adjustmentFields, "adjustment")
	}

	protected def formatData(data: Option[Any]): String = data match {
		case Some(i: ReadableInstant) => csvFormat(i)
		case Some(b: Boolean) => b.toString.toLowerCase
		case Some(i: Int) => i.toString
		case Some(s: String) => s
		case Some(other) => other.toString
		case None => ""
	}

	protected def itemData(item: Student): Map[String, Any] =
		(
			prefix(identityData(item), "student") ++
			prefix(submissionData(item), "submission") ++
			prefix(extraFieldData(item), "submission") ++
			prefix(submissionStatusData(item), "submission") ++
			(if (assignment.markingWorkflow != null) prefix(markerData(item), "marking") else Map()) ++
			prefix(plagiarismData(item), "marking") ++
			prefix(feedbackData(item), "feedback") ++
			prefix(adjustmentData(item), "adjustment")
		).mapValues {
			case Some(any) => any
			case any => any
		}

	private def prefix(fields: Seq[String], prefix: String) = fields map { name => prefix + "-" + name }

	private def prefix(data: Map[String, Any], prefix: String) = data map {
		case (key, value) => prefix + "-" + key -> value
	}

}

trait SubmissionAndFeedbackExport {
	val assignment: Assignment
	val module: Module

	def topLevelUrl: String

	val isoFormatter = DateFormats.IsoDateTime
	def isoFormat(i: ReadableInstant): String = isoFormatter print i

	// This Seq specifies the core field order
	val baseFields = Seq("module-code", "id", "open-date", "open-ended", "close-date")
	val identityFields: Seq[String] =
		if (module.adminDepartment.showStudentName) Seq("university-id", "name")
		else Seq("university-id")

	val submissionFields = Seq("id", "time", "downloaded")
	val submissionStatusFields = Seq("late", "within-extension", "markable")
	val markerFields = Seq("first-marker", "second-marker")
	val plagiarismFields = Seq("suspected-plagiarised", "similarity-percentage")
	val feedbackFields = Seq("id", "uploaded", "released","mark", "grade", "downloaded")
	val adjustmentFields = Seq("mark", "grade", "reason")

	protected def assignmentData: Map[String, Any] = Map(
		"module-code" -> module.code,
		"id" -> assignment.id,
		"open-date" -> assignment.openDate,
		"open-ended" -> assignment.openEnded,
		"close-date" -> (if (assignment.openEnded) "" else assignment.closeDate),
		"submissions-zip-url" -> (topLevelUrl + Routes.admin.assignment.submissionsZip(assignment))
	)

	protected def identityData(item: Student): Map[String, Any] = Map(
		"university-id" -> CurrentUser.studentIdentifier(item.user)
	) ++ (if (module.adminDepartment.showStudentName) Map("name" -> item.user.getFullName) else Map())

	protected def submissionData(student: Student): Map[String, Any] = student.cm2.enhancedSubmission match {
		case Some(item) if item.submission.id.hasText => Map(
			"submitted" -> true,
			"id" -> item.submission.id,
			"time" -> item.submission.submittedDate,
			"downloaded" -> item.downloaded
		)
		case _ => Map(
			"submitted" -> false
		)
	}

	protected def submissionStatusData(student: Student): Map[String, Any] = student.cm2.enhancedSubmission match {
		case Some(item) => Map(
			"late" -> item.submission.isLate,
			"within-extension" -> item.submission.isAuthorisedLate,
			"markable" -> assignment.isReleasedForMarking(item.submission.usercode)
		)
		case _ => student.cm2.enhancedExtension match {
			case Some(item) =>
				val assignmentClosed = !assignment.openEnded && assignment.isClosed
				val late = assignmentClosed && !item.within
				val within = item.within
				Map(
					"late" -> late,
					"within-extension" -> within
				)
			case _ => Map(
				"late" -> (if (!assignment.openEnded && assignment.isClosed) true else false)
			)
		}
	}

	protected def markerData(student: Student): Map[String, Any] = Map(
		"first-marker" -> assignment.getStudentsFirstMarker(student.user.getUserId).map(_.getFullName).getOrElse(""),
		"second-marker" -> assignment.getStudentsSecondMarker(student.user.getUserId).map(_.getFullName).getOrElse("")
	)

	protected def extraFieldData(student: Student): Map[String, String] = {
		var fieldDataMap = mutable.ListMap[String, String]()

		student.cm2.enhancedSubmission match {
			case Some(item) => item.submission.values.asScala foreach ( value =>
				if (value.hasAttachments) {
					val attachmentNames = value.attachments.asScala.map { file =>
						(file.name, item.submission.zipFileName(file))
					}

					if (attachmentNames.nonEmpty) {
						val fileNames = attachmentNames.map { case (fileName, _) => fileName }.mkString(",")
						val zipPaths = attachmentNames.map { case (_, zipPath) => zipPath }.mkString(",")

						fieldDataMap += (value.name + "-name") -> fileNames
						fieldDataMap += (value.name + "-zip-path") -> zipPaths
					}
				} else if (value.value != null) {
					fieldDataMap += value.name -> value.value
				}
			)
			case None =>
		}

		fieldDataMap.toMap
	}

	protected def plagiarismData(student: Student): Map[String, Any] = student.cm2.enhancedSubmission match {
		case Some(item) if item.submission.id.hasText =>
			Map(
				"suspected-plagiarised" -> item.submission.suspectPlagiarised
			) ++ (item.submission.allAttachments.find(_.originalityReport != null) match {
				case Some(a) =>
					val report = a.originalityReport
					report.overlap.map { overlap => "similarity-percentage" -> overlap }.toMap
				case _ => Map()
			})
		case _ => Map()
	}

	protected def feedbackData(student: Student): Map[String, Any] = student.cm2.enhancedFeedback match {
		case Some(item) if item.feedback.id.hasText =>
			Map(
				"id" -> item.feedback.id,
				"uploaded" -> item.feedback.createdDate,
				"released" -> item.feedback.released
			) ++
			item.feedback.actualMark.map { mark => "mark" -> mark }.toMap ++
			item.feedback.actualGrade.map { grade => "grade" -> grade }.toMap ++
			Map("downloaded" -> item.downloaded)
		case None => Map()
	}

	protected def adjustmentData(student: Student): Map[String, Any] = {
		val feedback = student.cm2.enhancedFeedback.map(_.feedback)
		feedback.filter(_.hasPrivateOrNonPrivateAdjustments).map( feedback => {
			feedback.latestMark.map("mark" -> _).toMap ++
			feedback.latestGrade.map("grade" -> _).toMap ++
			Map("reason" -> feedback.latestPrivateOrNonPrivateAdjustment.map(_.reason))
		}).getOrElse(Map())
	}

}
