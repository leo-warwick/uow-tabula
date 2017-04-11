package uk.ac.warwick.tabula.commands.reports.smallgroups

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.services.TermService
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService

object SmallGroupsReportFilters {

	val termService: TermService = Wire[TermService]
	val attendanceMonitoringService: AttendanceMonitoringService = Wire[AttendanceMonitoringService]

	def identity(result: AllSmallGroupsReportCommandResult): AllSmallGroupsReportCommandResult = result

	def unrecorded(academicYear: AcademicYear)(result: AllSmallGroupsReportCommandResult): AllSmallGroupsReportCommandResult = {
		val thisWeek = termService.getAcademicWeekForAcademicYear(DateTime.now, academicYear)
		val thisDay = DateTime.now.getDayOfWeek
		val unrecordedMap = result.attendance.map{ case(studentData, eventMap) =>
			studentData -> eventMap.filter { case (event, state) =>
				(event.week < thisWeek || event.week == thisWeek && event.event.day.getAsInt < thisDay) &&
					state == AttendanceState.NotRecorded
			}
		}.filter{ case(studentData, eventMap) => eventMap.nonEmpty }
		AllSmallGroupsReportCommandResult(
			unrecordedMap,
			attendanceMonitoringService.getAttendanceMonitoringDataForStudents(unrecordedMap.keySet.toSeq.sortBy(s => (s.getLastName, s.getFirstName)).map(_.getWarwickId), academicYear),
			unrecordedMap.flatMap { case (user, attendanceMap) => attendanceMap.keys }.toSeq.distinct.sortBy(sgew => (sgew.week, sgew.event.day.getAsInt))
		)
	}

	def missed(academicYear: AcademicYear)(result: AllSmallGroupsReportCommandResult): AllSmallGroupsReportCommandResult = {
		val missedMap = result.attendance.map{ case(studentData, eventMap) =>
			studentData -> eventMap.filter { case (_, state) =>	state == AttendanceState.MissedUnauthorised || state == AttendanceState.MissedAuthorised	}
		}.filter{ case(studentData, eventMap) => eventMap.nonEmpty }
		AllSmallGroupsReportCommandResult(
			missedMap,
			attendanceMonitoringService.getAttendanceMonitoringDataForStudents(missedMap.keySet.toSeq.sortBy(s => (s.getLastName, s.getFirstName)).map(_.getWarwickId), academicYear),
			missedMap.flatMap { case (user, attendanceMap) => attendanceMap.keys }.toSeq.distinct.sortBy(sgew => (sgew.week, sgew.event.day.getAsInt))
		)
	}
}
