package uk.ac.warwick.tabula.commands.reports.smallgroups

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.reports.{ReportCommandState, ReportPermissions}
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.services._

import scala.collection.JavaConverters._

object SmallGroupsReportProcessor {
	def apply(department: Department, academicYear: AcademicYear) =
		new SmallGroupsReportProcessorInternal(department, academicYear)
			with AutowiringTermServiceComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[SmallGroupsReportProcessorResult]
			with ReportPermissions
			with SmallGroupsReportProcessorState
			with ReadOnly with Unaudited {
			override lazy val eventName: String = "SmallGroupsReportProcessor"
		}
}

case class EventData(
	id: String,
	moduleCode: String,
	setName: String,
	format: String,
	groupName: String,
	week: Int,
	day: Int,
	dayString: String,
	location: String,
	tutors: String,
	isLate: Boolean
)

case class SmallGroupsReportProcessorResult(
	attendance: Map[AttendanceMonitoringStudentData, Map[EventData, AttendanceState]],
	students: Seq[AttendanceMonitoringStudentData],
	events: Seq[EventData]
)

class SmallGroupsReportProcessorInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[SmallGroupsReportProcessorResult] with TaskBenchmarking {

	self: SmallGroupsReportProcessorState with TermServiceComponent with ProfileServiceComponent =>

	override def applyInternal(): SmallGroupsReportProcessorResult = {
		val processedStudents = students.asScala.map{properties =>
			AttendanceMonitoringStudentData(
				properties.get("firstName"),
				properties.get("lastName"),
				properties.get("universityId"),
				null,
				null,
				null,
				properties.get("route"),
				null,
				properties.get("yearOfStudy"),
				properties.get("sprCode")
			)
		}.toSeq.sortBy(s => (s.lastName, s.firstName))
		val thisWeek = termService.getAcademicWeekForAcademicYear(DateTime.now, academicYear)
		val thisDay = DateTime.now.getDayOfWeek
		val processedEvents = events.asScala.map{properties =>
			EventData(
				properties.get("id"),
				properties.get("moduleCode"),
				properties.get("setName"),
				properties.get("format"),
				properties.get("groupName"),
				properties.get("week").toInt,
				properties.get("day").toInt,
				DayOfWeek(properties.get("day").toInt).getName,
				properties.get("location"),
				properties.get("tutors"),
				properties.get("week").toInt < thisWeek ||
					properties.get("week").toInt == thisWeek && properties.get("day").toInt < thisDay
			)
		}.toSeq.sortBy(event => (event.week, event.day))
		val processedAttendance = attendance.asScala.flatMap{case(universityId, eventMap) =>
			processedStudents.find(_.universityId == universityId).map(studentData =>
				studentData -> eventMap.asScala.flatMap { case (id, stateString) =>
					processedEvents.find(_.id == id).map(event => event -> AttendanceState.fromCode(stateString))
				}.toMap)
		}.toMap
		SmallGroupsReportProcessorResult(processedAttendance, processedStudents, processedEvents)
	}

}

trait SmallGroupsReportProcessorState extends ReportCommandState {
	var attendance: JMap[String, JMap[String, String]] =
		LazyMaps.create{_: String => JMap[String, String]() }.asJava

	var students: JList[JMap[String, String]] = JArrayList()

	var events: JList[JMap[String, String]] = JArrayList()
}
