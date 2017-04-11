package uk.ac.warwick.tabula.web.controllers.exams.exams.admin

import org.apache.commons.lang3.text.WordUtils
import org.apache.poi.hssf.usermodel.HSSFDataFormat
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.exams.ViewExamCommandResult
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.csv.CSVLineWriter

import scala.xml.Elem

trait ExamExports {

class CSVBuilder(val students:Seq[User], val results:ViewExamCommandResult, val exam: Exam, val module: Module, val academicYear: AcademicYear)
		extends CSVLineWriter[User] with ExamHeaderInformation with ItemData with FormatsContent{

		def getNoOfColumns(item:User): Int = headers.size
		def getColumn(item:User, i:Int): String = formatData(getItemData(item, results, module).get(headers(i)))
	}
}

trait FormatsContent {

	protected def formatData(data: Option[Any]): String = data match {
		case Some(date: DateTime) => DateTimeFormat.forPattern("HH:mm:ss dd/MM/yyyy").print(date)
		case Some(b: Boolean) => b.toString.toLowerCase
		case Some(i: Int) => i.toString
		case Some(s: String) => s
		case Some(Some(s: String)) => s
		case Some(Some(Some(i:Int))) => i.toString
		case None => ""
		case _ => ""
	}

	def toXMLString(value: Option[Any]): String = value match {
		case Some(o: Option[Any]) => toXMLString(o)
		case Some(b: Boolean) => b.toString.toLowerCase
		case Some(i: Int) => i.toString
		case Some(s: String) => s
		case Some(other) => other.toString
		case None => ""
	}
}

trait ExamHeaderInformation {

	val SEAT_NUMBER: String = "Seat number"
	val STUDENT: String = "Student"
	val ORIGINAL_MARK: String = "Original Mark"
	val ORIGINAL_GRADE: String = "Original Grade"
	val ADJUSTED_MARK: String = "Adjusted Mark"
	val ADJUSTED_GRADE: String = "Adjusted Grade"
	val SITS_UPLOAD_STATUS: String = "SITS upload Status"
	val SITS_UPLOAD_DATE: String = "SITS upload Date"
	val SITS_UPLOAD_MARK: String = "SITS upload Mark"
  val SITS_UPLOAD_GRADE: String = "SITS upload Grade"

	var headers:Seq[String] = List(
		SEAT_NUMBER,
		STUDENT,
		ORIGINAL_MARK,
		ORIGINAL_GRADE,
		ADJUSTED_MARK,
		ADJUSTED_GRADE,
		SITS_UPLOAD_STATUS,
		SITS_UPLOAD_DATE,
		SITS_UPLOAD_MARK,
		SITS_UPLOAD_GRADE
	)
}

trait ItemData extends ExamHeaderInformation {
	def getItemData(student: User,  results:ViewExamCommandResult, module: Module): Map[String, Any] = {

		val feedback:ExamFeedback = studentFeedback(results, student)
		val hasFeedback = studentHasFeedback(results, student)
		val hasSitsStatus = studentHasSitsFeedack(results, student)

		var data:Map[String, Any] = Map()

		data += (SEAT_NUMBER -> results.seatNumberMap.get(student))
		data += (STUDENT -> studentName(module, student))
		if (hasFeedback) {
			data += (ORIGINAL_MARK -> feedback.actualMark.get)
			data += (ORIGINAL_GRADE -> feedback.actualGrade.getOrElse(""))
			data += (ADJUSTED_MARK -> feedback.latestPrivateOrNonPrivateAdjustment.map(_.mark).getOrElse(""))
			data += (ADJUSTED_GRADE -> feedback.latestPrivateOrNonPrivateAdjustment.flatMap(_.grade).getOrElse(""))
			if (hasSitsStatus) {
				val sitsStatus = studentSitsFeedback(results, student)
				data += (SITS_UPLOAD_STATUS -> sitsStatus.status.description)
				data += (SITS_UPLOAD_DATE -> sitsStatus.dateOfUpload)
				data += (SITS_UPLOAD_MARK -> sitsStatus.actualMarkLastUploaded)
				data += (SITS_UPLOAD_GRADE -> sitsStatus.actualGradeLastUploaded)
			}
		}
		data
	}

	def studentName(module: Module, student: User): String = {
		if (module.adminDepartment.showStudentName) {
			student.getFullName
		} else {
			student.getWarwickId
		}
	}

	def studentHasFeedback(results:ViewExamCommandResult, student:User): Boolean = results.feedbackMap.get(student).isDefined

	def studentFeedback(results:ViewExamCommandResult, student:User): ExamFeedback = {
		if (studentHasFeedback(results, student)) {
			results.feedbackMap.get(student).get.get
		} else {
			new ExamFeedback
		}
	}

	def studentHasSitsFeedack(results:ViewExamCommandResult, student:User) : Boolean = {
		val feedback = studentFeedback(results, student)
		studentHasFeedback(results, student) && results.sitsStatusMap.get(feedback).isDefined
	}

	def studentSitsFeedback(results:ViewExamCommandResult, student:User) : FeedbackForSits = {
		val feedback = studentFeedback(results, student)
		if (studentHasSitsFeedack(results, student)) {
			results.sitsStatusMap.get(feedback).get.get
		} else {
			new FeedbackForSits
		}
	}
}


class XMLBuilder(val students:Seq[User], val results:ViewExamCommandResult, val exam: Exam, val module: Module, val academicYear: AcademicYear)
	extends ItemData with FormatsContent {

	def toXML: Elem = {
		<exam>
			<name>{exam.name}</name>
			<module-code>{ module.code }</module-code>
			<module-name>{ module.name }</module-name>
			<department-name>{ module.adminDepartment.name }</department-name>
			<department-code>{ module.adminDepartment.code }</department-code>
			<academic-year>{academicYear}</academic-year>
			<students>
					{ students map studentXML }
			</students>
		</exam>
	}

	def studentXML(student: User): Elem = {

		val feedback:ExamFeedback = studentFeedback(results, student)
		val hasFeedback = studentHasFeedback(results, student)
		val hasSitsStatus = studentHasSitsFeedack(results, student)
		val sitsStatus = studentSitsFeedback(results, student)
		<student>
			<university-number>{ student.getWarwickId }</university-number>
			<student-name>{if (module.adminDepartment.showStudentName){ student.getFullName } }</student-name>
			<seat-number>{ results.seatNumberMap.get(student).flatten.getOrElse("") }</seat-number>
			<original-mark>{if (hasFeedback) {feedback.actualMark.getOrElse("")} }</original-mark>
			<original-grade>{if (hasFeedback) {feedback.actualGrade.getOrElse("")} }</original-grade>
			<adjusted-mark>{if (hasFeedback) {feedback.latestPrivateOrNonPrivateAdjustment.map(_.mark).getOrElse("")} }</adjusted-mark>
			<adjusted-grade>{if (hasFeedback) {feedback.latestPrivateOrNonPrivateAdjustment.flatMap(_.grade).getOrElse("")} }</adjusted-grade>
			<sits-upload-status>{if (hasSitsStatus) {sitsStatus.status.description} }</sits-upload-status>
			<sits-upload-date>{if (hasSitsStatus) {sitsStatus.dateOfUpload} }</sits-upload-date>
			<sits-upload-mark>{if (hasSitsStatus) {sitsStatus.actualMarkLastUploaded} }</sits-upload-mark>
			<sits-upload-grade>{if (hasSitsStatus) {sitsStatus.actualGradeLastUploaded} }</sits-upload-grade>
		</student>
	}
}

class ExcelBuilder(val students: Seq[User], val results:ViewExamCommandResult, val module: Module)
	extends ExamHeaderInformation with ItemData with FormatsContent {

	def toXLSX: XSSFWorkbook = {
		val workbook = new XSSFWorkbook()
		val sheet = generateNewSheet(workbook)

		students foreach { addRow(sheet)(_) }

		formatWorksheet(sheet)
		workbook
	}

	def generateNewSheet(workbook: XSSFWorkbook): XSSFSheet = {
		val sheet = workbook.createSheet(module.code.toUpperCase + " - " + safeExamName)

		def formatHeader(header: String) =
			WordUtils.capitalizeFully(header.replace('-', ' '))

		// add header row
		val headerRow = sheet.createRow(0)
		headers.zipWithIndex foreach {
			case (header, index) => headerRow.createCell(index).setCellValue(formatHeader(header))
		}
		sheet
	}

	def addRow(sheet: XSSFSheet)(student: User) {
		val plainCellStyle = {
			val cs = sheet.getWorkbook.createCellStyle()
			cs.setDataFormat(HSSFDataFormat.getBuiltinFormat("@"))
			cs
		}

		val row = sheet.createRow(sheet.getLastRowNum + 1)
		headers.zipWithIndex foreach {
			case (header, index) =>
				val cell = row.createCell(index)

				if (index == 1) {
					// University IDs have leading zeros and Excel would normally remove them.
					// Set a manual data format to remove this possibility
					cell.setCellStyle(plainCellStyle)
				}
				cell.setCellValue(formatData(getItemData(student,results, module).get(header)))
		}
	}

	def formatWorksheet(sheet: XSSFSheet): Unit = {
		(0 to headers.size) foreach sheet.autoSizeColumn
	}

	// trim the assignment name down to 20 characters. Excel sheet names must be 31 chars or less so
	val trimmedModuleName: String = {
		if (module.name.length > 20)
			module.name.substring(0, 20)
		else
			module.name
	}

	// util to replace unsafe characters with spaces
	val safeExamName: String = WorkbookUtil.createSafeSheetName(trimmedModuleName)
}