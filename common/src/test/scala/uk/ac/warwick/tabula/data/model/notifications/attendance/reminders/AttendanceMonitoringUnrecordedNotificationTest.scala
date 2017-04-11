package uk.ac.warwick.tabula.data.model.notifications.attendance.reminders

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.{Department, Notification}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.services.TermServiceImpl
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.AnonymousUser

class AttendanceMonitoringUnrecordedNotificationTest extends TestBase with Mockito {

	val department: Department = Fixtures.department("cs")

	@Test def titlePoints() {
		val notification = Notification.init(new AttendanceMonitoringUnrecordedPointsNotification, new AnonymousUser, department)
		notification.attendanceMonitoringService = mock[AttendanceMonitoringService]
		notification.termService = new TermServiceImpl
		notification.created = new DateTime(2014, DateTimeConstants.NOVEMBER, 15, 9, 18, 27, 0)

		val scheme = new AttendanceMonitoringScheme
		val unrecorded = Seq(
			Fixtures.attendanceMonitoringPoint(scheme),
			Fixtures.attendanceMonitoringPoint(scheme)
		)

		notification.attendanceMonitoringService.findUnrecordedPoints(department, new AcademicYear(2014), notification.created.minusDays(7).toLocalDate) returns unrecorded

		notification.title should be ("1 monitoring point needs recording")
	}

	@Test def titlePointsPlural() {
		val notification = Notification.init(new AttendanceMonitoringUnrecordedPointsNotification, new AnonymousUser, department)
		notification.attendanceMonitoringService = mock[AttendanceMonitoringService]
		notification.termService = new TermServiceImpl
		notification.created = new DateTime(2014, DateTimeConstants.NOVEMBER, 15, 9, 18, 27, 0)

		val scheme = new AttendanceMonitoringScheme
		val unrecorded = Seq(
			Fixtures.attendanceMonitoringPoint(scheme, startWeek = 2, endWeek = 3),
			Fixtures.attendanceMonitoringPoint(scheme, startWeek = 2, endWeek = 4)
		)

		notification.attendanceMonitoringService.findUnrecordedPoints(department, new AcademicYear(2014), notification.created.minusDays(7).toLocalDate) returns unrecorded

		notification.title should be ("2 monitoring points need recording")
	}

	@Test def titleStudents() {
		val notification = Notification.init(new AttendanceMonitoringUnrecordedStudentsNotification, new AnonymousUser, department)
		notification.attendanceMonitoringService = mock[AttendanceMonitoringService]
		notification.termService = new TermServiceImpl
		notification.created = new DateTime(2014, DateTimeConstants.NOVEMBER, 15, 9, 18, 27, 0)

		val unrecorded = Seq(
			AttendanceMonitoringStudentData(
				firstName = "Mat",
				lastName = "Mannion",
				universityId = "0672089",
				userId = "cuscav",
				scdBeginDate = DateTime.now.minusYears(2).toLocalDate,
				scdEndDate = None,
				null,
				null,
				null,
				null
			)
		)

		notification.attendanceMonitoringService.findUnrecordedStudents(department, new AcademicYear(2014), notification.created.minusDays(7).toLocalDate) returns unrecorded

		notification.title should be ("1 student needs monitoring points recording")
	}

	@Test def titleStudentsPlural() {
		val notification = Notification.init(new AttendanceMonitoringUnrecordedStudentsNotification, new AnonymousUser, department)
		notification.attendanceMonitoringService = mock[AttendanceMonitoringService]
		notification.termService = new TermServiceImpl
		notification.created = new DateTime(2014, DateTimeConstants.NOVEMBER, 15, 9, 18, 27, 0)

		val unrecorded = Seq(
			AttendanceMonitoringStudentData(
				firstName = "Mat",
				lastName = "Mannion",
				universityId = "0672089",
				userId = "cuscav",
				scdBeginDate = DateTime.now.minusYears(2).toLocalDate,
				scdEndDate = None,
				null,
				null,
				null,
				null
			),
			AttendanceMonitoringStudentData(
				firstName = "Nick",
				lastName = "Howes",
				universityId = "0672088",
				userId = "cusebr",
				scdBeginDate = DateTime.now.minusYears(2).toLocalDate,
				scdEndDate = None,
				null,
				null,
				null,
				null
			)
		)

		notification.attendanceMonitoringService.findUnrecordedStudents(department, new AcademicYear(2014), notification.created.minusDays(7).toLocalDate) returns unrecorded

		notification.title should be ("2 students need monitoring points recording")
	}

}
