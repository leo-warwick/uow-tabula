package uk.ac.warwick.tabula.commands.reports.smallgroups

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.commands.reports.{ReportCommandState, ReportPermissions}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.userlookup.User

object SmallGroupsByModuleReportCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new SmallGroupsByModuleReportCommandInternal(department, academicYear)
			with ComposableCommand[SmallGroupsByModuleReportCommandResult]
			with AutowiringAttendanceMonitoringServiceComponent
			with ReportPermissions
			with SmallGroupsByModuleReportCommandState
			with SetsFilteredAttendance
			with ReadOnly with Unaudited
}

case class SmallGroupsByModuleReportCommandResult(
	counts: Map[User, Map[Module, Int]],
	studentDatas: Seq[AttendanceMonitoringStudentData],
	modules: Seq[Module]
)

class SmallGroupsByModuleReportCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[SmallGroupsByModuleReportCommandResult] {

	self: SmallGroupsByModuleReportCommandState with AttendanceMonitoringServiceComponent =>

	override def applyInternal(): SmallGroupsByModuleReportCommandResult = {
		val byModule: Map[User, Map[Module, Int]] = filteredAttendance.attendance.map{case(student, eventMap) =>
			student -> eventMap.groupBy(_._1.event.group.groupSet.module).map { case (module, groupedEventMap) =>
				module -> groupedEventMap.keys.size
			}
		}

		SmallGroupsByModuleReportCommandResult(
			byModule,
			attendanceMonitoringService.getAttendanceMonitoringDataForStudents(byModule.keySet.toSeq.sortBy(s => (s.getLastName, s.getFirstName)).map(_.getWarwickId), academicYear),
			byModule.flatMap(_._2.map(_._1)).toSeq.distinct.sorted
		)
	}
}

trait SetsFilteredAttendance {

	self: SmallGroupsByModuleReportCommandState =>

	def setFilteredAttendance(theFilteredAttendance: AllSmallGroupsReportCommandResult): Unit = {
		filteredAttendance = theFilteredAttendance
	}
}

trait SmallGroupsByModuleReportCommandState extends ReportCommandState {
	var filteredAttendance: AllSmallGroupsReportCommandResult = _
}
