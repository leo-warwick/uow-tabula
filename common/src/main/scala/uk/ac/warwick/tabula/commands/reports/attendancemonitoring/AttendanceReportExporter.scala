package uk.ac.warwick.tabula.commands.reports.attendancemonitoring

import freemarker.template.{Configuration, DefaultObjectWrapper}
import org.apache.poi.hssf.usermodel.HSSFDataFormat
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState.{MissedUnauthorised, NotRecorded}
import uk.ac.warwick.tabula.helpers.IntervalFormatter
import uk.ac.warwick.util.csv.CSVLineWriter

import scala.xml.Elem

class AttendanceReportExporter(val processorResult: AttendanceReportProcessorResult, val department: Department)
  extends CSVLineWriter[AttendanceMonitoringStudentData] {

  val intervalFormatter = new IntervalFormatter
  val wrapper = new DefaultObjectWrapper(Configuration.VERSION_2_3_28)
  val isoFormatter = DateFormats.IsoDateTime

  val result: Map[AttendanceMonitoringStudentData, Map[PointData, AttendanceState]] = processorResult.attendance
  val students: Seq[AttendanceMonitoringStudentData] = processorResult.students
  val points: Seq[PointData] = processorResult.points

  val headers: Seq[String] = Seq("First name", "Last name", "University ID", "Route", "Year of study", "SPR code", "Tier 4 requirements") ++
    points.map(p => s"${p.name} (${
      intervalFormatter.exec(JList(
        wrapper.wrap(p.startDate.toDate),
        wrapper.wrap(p.endDate.toDate)
      ))
    })") ++
    Seq("Unrecorded", "Missed (unauthorised)")

  val unrecordedIndex: Int = headers.size - 2
  val missedIndex: Int = headers.size - 1

  override def getNoOfColumns(o: AttendanceMonitoringStudentData): Int = headers.size

  override def getColumn(studentData: AttendanceMonitoringStudentData, pointIndex: Int): String = {
    pointIndex match {
      case 0 =>
        studentData.firstName
      case 1 =>
        studentData.lastName
      case 2 =>
        studentData.universityId
      case 3 =>
        studentData.routeCode
      case 4 =>
        studentData.yearOfStudy
      case 5 =>
        studentData.sprCode
      case 6 =>
        if (studentData.tier4Requirements) "Yes" else "No"
      case index if index == unrecordedIndex =>
        result(studentData).map { case (_, state) => state }.count(_ == NotRecorded).toString
      case index if index == missedIndex =>
        result(studentData).map { case (_, state) => state }.count(_ == MissedUnauthorised).toString
      case _ =>
        val thisPoint = points(pointIndex - 7)
        result(studentData).get(thisPoint).map {
          case state if state == NotRecorded =>
            if (thisPoint.isLate)
              "Late"
            else
              state.description
          case state =>
            state.description
        }.getOrElse("n/a")
    }
  }

  def toXLSX: SXSSFWorkbook = {
    val workbook = new SXSSFWorkbook
    val sheet = generateNewSheet(workbook)

    result.keys.foreach(addRow(sheet))

    (0 to headers.size) foreach sheet.autoSizeColumn
    workbook
  }

  private def generateNewSheet(workbook: SXSSFWorkbook) = {
    val sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(department.name))
    sheet.trackAllColumnsForAutoSizing()

    // add header row
    val headerRow = sheet.createRow(0)
    headers.zipWithIndex foreach {
      case (header, index) => headerRow.createCell(index).setCellValue(header)
    }
    sheet
  }

  private def addRow(sheet: Sheet)(studentData: AttendanceMonitoringStudentData) {
    val plainCellStyle = {
      val cs = sheet.getWorkbook.createCellStyle()
      cs.setDataFormat(HSSFDataFormat.getBuiltinFormat("@"))
      cs
    }

    val row = sheet.createRow(sheet.getLastRowNum + 1)
    headers.zipWithIndex foreach { case (_, index) =>
      val cell = row.createCell(index)

      if (index == 2) {
        // University IDs have leading zeros and Excel would normally remove them.
        // Set a manual data format to remove this possibility
        cell.setCellStyle(plainCellStyle)
      }

      cell.setCellValue(getColumn(studentData, index))
    }
  }

  def toXML: Elem = {
    <result>
      <attendance>
        {result.map { case (studentData, pointMap) =>
        <student universityid={studentData.universityId}>
          {pointMap.map { case (point, state) =>
          <point id={point.id}>
            {state}
          </point>
        }}
        </student>
      }}
      </attendance>

      <students>
        {students.map(studentData =>
          <student
          firstname={studentData.firstName}
          lastname={studentData.lastName}
          universityid={studentData.universityId}
          route={studentData.routeCode}
          year={studentData.yearOfStudy}
          spr={studentData.sprCode}
          tier4Requirements={if (studentData.tier4Requirements) "Yes" else "No"}/>
      )}
      </students>
      <points>
        {points.map(point =>
          <point
          id={point.id}
          name={point.name}
          startdate={isoFormatter.print(point.startDate.toDateTimeAtStartOfDay)}
          enddate={isoFormatter.print(point.endDate.toDateTimeAtStartOfDay)}/>
      )}
      </points>
    </result>
  }
}
