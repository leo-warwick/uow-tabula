package uk.ac.warwick.tabula.commands.reports.smallgroups

import org.apache.poi.hssf.usermodel.HSSFDataFormat
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState._
import uk.ac.warwick.util.csv.CSVLineWriter

import scala.xml.Elem

class SmallGroupsReportExporter(val processorResult: SmallGroupsReportProcessorResult, val department: Department)
  extends CSVLineWriter[AttendanceMonitoringStudentData] {

  val attendance: Map[AttendanceMonitoringStudentData, Map[EventData, AttendanceState]] = processorResult.attendance
  val students: Seq[AttendanceMonitoringStudentData] = processorResult.students
  val events: Seq[EventData] = processorResult.events

  val studentInfoHeaders: Seq[String] = Seq("First name", "Last name", "University ID", "Route", "Year of study", "SPR code", "Email address", "Tutor email address")
  val headers: Seq[String] = studentInfoHeaders ++ events.map(e => s"${e.moduleCode} ${e.setName} ${e.format} ${e.groupName} ${e.dayString} Week ${e.week}") ++
    Seq("Not recorded", "Not recorded - Late", "Missed (unauthorised)", "Missed (authorised)", "Attended")

  val unrecordedIndex: Int = headers.size - 5
  val unrecordedLateIndex: Int = headers.size - 4
  val missedIndex: Int = headers.size - 3
  val authorisedIndex: Int = headers.size - 2
  val attendedIndex: Int = headers.size - 1

  override def getNoOfColumns(o: AttendanceMonitoringStudentData): Int = headers.size

  override def getColumn(studentData: AttendanceMonitoringStudentData, eventIndex: Int): String = {
    eventIndex match {
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
        studentData.email
      case 7 =>
        studentData.tutorEmail.getOrElse("")
      case index if index == unrecordedIndex =>
        attendance.get(studentData).map(eventMap =>
          eventMap.count { case (_, state) => state == NotRecorded }.toString
        ).getOrElse("0")
      case index if index == unrecordedLateIndex =>
        attendance.get(studentData).map(eventMap =>
          eventMap.count { case (event, state) => state == NotRecorded && event.isLate }.toString
        ).getOrElse("0")
      case index if index == missedIndex =>
        attendance.get(studentData).map(eventMap =>
          eventMap.count { case (_, state) => state == MissedUnauthorised }.toString
        ).getOrElse("0")
      case index if index == authorisedIndex =>
        attendance.get(studentData).map(eventMap =>
          eventMap.count { case (_, state) => state == MissedAuthorised }.toString
        ).getOrElse("0")
      case index if index == attendedIndex =>
        attendance.get(studentData).map(eventMap =>
          eventMap.count { case (_, state) => state == Attended }.toString
        ).getOrElse("0")
      case _ =>
        val thisEvent = events(eventIndex - studentInfoHeaders.size)
        attendance.get(studentData).flatMap(_.get(thisEvent).map {
          case state if state == NotRecorded =>
            if (thisEvent.isLate)
              "Not recorded - Late"
            else
              state.description
          case state =>
            state.description
        }).getOrElse("n/a")
    }
  }

  def toXLSX: SXSSFWorkbook = {
    val workbook = new SXSSFWorkbook
    val sheet = generateNewSheet(workbook)

    students.foreach(addRow(sheet))

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
        {attendance.map { case (studentData, eventMap) =>
        <student universityid={studentData.universityId}>
          {eventMap.map { case (event, state) =>
          <event id={event.id}>
            {state}
          </event>
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
          emailAddress={studentData.email}
          tutorEmail={studentData.tutorEmail.getOrElse("")}
          />
      )}
      </students>
      <events>
        {events.map(event =>
          <event
          id={event.id}
          moduleCode={event.moduleCode}
          setName={event.setName}
          format={event.format}
          groupName={event.groupName}
          week={event.week.toString}
          day={event.day.toString}
          location={event.location}
          tutors={event.tutors}/>
      )}
      </events>
    </result>
  }
}
