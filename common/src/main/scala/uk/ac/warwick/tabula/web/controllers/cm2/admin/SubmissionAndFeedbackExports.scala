package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.apache.commons.lang3.text.WordUtils
import org.apache.poi.hssf.usermodel.HSSFDataFormat
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.joda.time.ReadableInstant
import org.joda.time.format.DateTimeFormatter
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.XmlUtils._
import uk.ac.warwick.tabula.helpers.cm2.{SubmissionListItem, WorkflowItems}
import uk.ac.warwick.tabula.{CurrentUser, DateFormats}
import uk.ac.warwick.util.csv.CSVLineWriter

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.xml._

class XMLBuilder(val items: Seq[WorkflowItems], val assignment: Assignment, override val module: Module) extends SubmissionAndFeedbackExport {
  var topLevelUrl: String = Wire.property("${toplevel.url}")

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

  def studentElement(item: WorkflowItems): Elem = {
    <student>
      {
        <submission>
          {
            item.enhancedSubmission map { item => item.submission.values.asScala.toSeq map fieldElement(item) } getOrElse Nil
          }
        </submission> % submissionData(item) % submissionStatusData(item)
      }
      { <marking /> % markerData(item) % plagiarismData(item) }
      {
        <feedback>
          { item.enhancedFeedback.flatMap { _.feedback.comments }.orNull }
        </feedback> % feedbackData(item)
      }
      { <adjustment /> % adjustmentData(item) }
    </student> % identityData(item)
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

class CSVBuilder(val items: Seq[WorkflowItems], val assignment: Assignment, override val module: Module)
  extends CSVLineWriter[WorkflowItems] with SubmissionAndFeedbackSpreadsheetExport {

  var topLevelUrl: String = Wire.property("${toplevel.url}")

  def getNoOfColumns(item:WorkflowItems): Int = headers.size

  def getColumn(item:WorkflowItems, i:Int): String = formatData(itemData(item).get(headers(i)))
}

class ExcelBuilder(val items: Seq[WorkflowItems], val assignment: Assignment, override val module: Module) extends SubmissionAndFeedbackSpreadsheetExport {

  var topLevelUrl: String = Wire.property("${toplevel.url}")

  def toXLSX: SXSSFWorkbook = {
    val workbook = new SXSSFWorkbook
    val sheet = generateNewSheet(workbook)

    items foreach { addRow(sheet)(_) }

    formatWorksheet(sheet)
    workbook
  }

  def generateNewSheet(workbook: SXSSFWorkbook): Sheet = {
    val sheet = workbook.createSheet(module.code.toUpperCase + " - " + safeAssignmentName)
    sheet.trackAllColumnsForAutoSizing()

    def formatHeader(header: String) =
      WordUtils.capitalizeFully(header.replace('-', ' '))

    // add header row
    val headerRow = sheet.createRow(0)
    headers.zipWithIndex foreach {
      case (header, index) => headerRow.createCell(index).setCellValue(formatHeader(header))
    }
    sheet
  }

  def addRow(sheet: Sheet)(item: WorkflowItems) {
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

  def formatWorksheet(sheet: Sheet): Unit = {
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
  val items: Seq[WorkflowItems]

  val csvFormatter: DateTimeFormatter = DateFormats.CSVDateTime
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
      prefix(markerFields, "marking") ++
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

  protected def itemData(item: WorkflowItems): Map[String, Any] =
    (
      prefix(identityData(item), "student") ++
        prefix(submissionData(item), "submission") ++
        prefix(extraFieldData(item), "submission") ++
        prefix(submissionStatusData(item), "submission") ++
        prefix(markerData(item), "marking") ++
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
  val module: Module = assignment.module

  def topLevelUrl: String

  val isoFormatter: DateTimeFormatter = DateFormats.IsoDateTime
  def isoFormat(i: ReadableInstant): String = isoFormatter print i

  // This Seq specifies the core field order
  val baseFields: Seq[String] = Seq("module-code", "id", "open-date", "open-ended", "close-date")
  val identityFields: Seq[String] = Seq("university-id") ++
    (if (module.adminDepartment.showStudentName) Seq("name") else Seq()) ++
    (if (assignment.showSeatNumbers) Seq("seat-number") else Seq())

  val submissionFields: Seq[String] = Seq("id", "time", "downloaded")
  val submissionStatusFields: Seq[String] = Seq("late", "within-extension", "markable")
  val markerFields: Seq[String] =
    if (assignment.markingWorkflow != null) Seq("first-marker", "second-marker")
    else if (assignment.cm2MarkingWorkflow != null) {
      val stagesByDescription = assignment.cm2MarkingWorkflow.markerStages.groupBy(_.description)

      val markerDescriptions = stagesByDescription.keys.toList.sortBy(r => stagesByDescription(r).map(_.order).min) // sort descriptions by their earliest stages
      val markerMarks = markerDescriptions.map(marker => marker.concat("-mark"))
      val markerGrades = markerDescriptions.map(marker => marker.concat("-grade"))

      markerDescriptions ++ markerMarks ++ markerGrades ++ (if(assignment.hasModeration) Seq("Was-moderated") else Seq())
    }
    else Seq()
  val plagiarismFields: Seq[String] = Seq("suspected-plagiarised", "similarity-percentage")
  val feedbackFields: Seq[String] = Seq("id", "uploaded", "released","mark", "grade", "downloaded")
  val adjustmentFields: Seq[String] = Seq("mark", "grade", "reason")

  protected def assignmentData: Map[String, Any] = Map(
    "module-code" -> module.code,
    "id" -> assignment.id,
    "open-date" -> assignment.openDate,
    "open-ended" -> assignment.openEnded,
    "close-date" -> (if (assignment.openEnded) "" else assignment.closeDate),
    "submissions-zip-url" -> (topLevelUrl + Routes.admin.assignment.submissionsZip(assignment))
  )

  protected def identityData(item: WorkflowItems): Map[String, Any] = Map("university-id" -> CurrentUser.studentIdentifier(item.student)) ++
    (if (module.adminDepartment.showStudentName) Map("name" -> item.student.getFullName) else Map()) ++
    (if (assignment.showSeatNumbers) assignment.getSeatNumber(item.student).map(n => Map("seat-number" -> n)).getOrElse(Map()) else Map())

  protected def submissionData(student: WorkflowItems): Map[String, Any] = student.enhancedSubmission match {
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

  protected def submissionStatusData(student: WorkflowItems): Map[String, Any] = student.enhancedSubmission match {
    case Some(item) => Map(
      "late" -> item.submission.isLate,
      "within-extension" -> item.submission.isAuthorisedLate,
      "markable" -> assignment.isReleasedForMarking(item.submission.usercode)
    )
    case _ => student.enhancedExtension match {
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

  protected def markerData(student: WorkflowItems): Map[String, Any] =
    if (assignment.markingWorkflow != null) {
      Map(
        "first-marker" -> assignment.getStudentsFirstMarker(student.student.getUserId).map(_.getFullName).getOrElse(""),
        "second-marker" -> assignment.getStudentsSecondMarker(student.student.getUserId).map(_.getFullName).getOrElse("")
      )
    } else if (assignment.cm2MarkingWorkflow != null) {
      val markerNames = student.enhancedFeedback.map(ef => {
        ef.feedback.feedbackByStage.map(fbs => fbs._1.description -> ef.feedback.feedbackMarkerByAllocationName(fbs._1.roleName).map(_.getFullName).getOrElse(""))
      }).getOrElse(Map())

      val markerMarks = student.enhancedFeedback.map(ef => {
        ef.feedback.feedbackByStage.map(fbs => fbs._1.description.concat("-mark") -> fbs._2.mark.getOrElse(""))
      }).getOrElse(Map())

      val markerGrades = student.enhancedFeedback.map(ef => {
          ef.feedback.feedbackByStage.map(fbs => fbs._1.description.concat("-grade") -> fbs._2.grade.getOrElse(""))
      }).getOrElse(Map())

      markerNames ++ markerMarks ++ markerGrades ++ (if(assignment.hasModeration) Map("was-moderated" -> student.enhancedFeedback.exists(_.feedback.wasModerated)) else Map())
    } else {
      Map()
    }

  protected def extraFieldData(student: WorkflowItems): Map[String, String] = {
    var fieldDataMap = mutable.ListMap[String, String]()

    student.enhancedSubmission match {
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

  protected def plagiarismData(student: WorkflowItems): Map[String, Any] = student.enhancedSubmission match {
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

  protected def feedbackData(student: WorkflowItems): Map[String, Any] = student.enhancedFeedback match {
    case Some(item) if item.feedback.id.hasText && !item.feedback.isPlaceholder =>
      Map(
        "id" -> item.feedback.id,
        "uploaded" -> item.feedback.updatedDate,
        "released" -> item.feedback.released
      ) ++
        item.feedback.actualMark.map { mark => "mark" -> mark }.toMap ++
        item.feedback.actualGrade.map { grade => "grade" -> grade }.toMap ++
        Map("downloaded" -> (item.downloaded || (item.feedback.released && !item.feedback.hasAttachments && item.onlineViewed)))
    case _ => Map()
  }

  protected def adjustmentData(student: WorkflowItems): Map[String, Any] = {
    val feedback = student.enhancedFeedback.map(_.feedback)
    feedback.filter(_.hasPrivateOrNonPrivateAdjustments).map( feedback => {
      feedback.latestMark.map("mark" -> _).toMap ++
        feedback.latestGrade.map("grade" -> _).toMap ++
        Map("reason" -> feedback.latestPrivateOrNonPrivateAdjustment.map(_.reason))
    }).getOrElse(Map())
  }

}
