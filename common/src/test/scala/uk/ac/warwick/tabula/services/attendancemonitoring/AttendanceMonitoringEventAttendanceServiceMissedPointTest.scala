package uk.ac.warwick.tabula.services.attendancemonitoring

import org.joda.time.{DateTime, DateTimeConstants, Interval}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{Module, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class AttendanceMonitoringEventAttendanceServiceMissedPointTest extends TestBase with Mockito {

	val mockModuleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]

	trait ServiceTestSupport extends SmallGroupServiceComponent with TermServiceComponent
		with ProfileServiceComponent with AttendanceMonitoringServiceComponent {

		val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
		val profileService: ProfileService = smartMock[ProfileService]
		val smallGroupService: SmallGroupService = smartMock[SmallGroupService]
		val termService: TermService = smartMock[TermService]
	}

	trait Fixture {
		val service = new AbstractAttendanceMonitoringEventAttendanceService with ServiceTestSupport

		val academicYear2013 = AcademicYear(2013)

		val termWeeks = Seq(
			(JInteger(Option(1)), new Interval(
				new DateTime().minusWeeks(4).withDayOfWeek(DateTimeConstants.MONDAY),
				new DateTime().minusWeeks(3).withDayOfWeek(DateTimeConstants.MONDAY)
			)),
			(JInteger(Option(2)), new Interval(
				new DateTime().minusWeeks(3).withDayOfWeek(DateTimeConstants.MONDAY),
				new DateTime().minusWeeks(2).withDayOfWeek(DateTimeConstants.MONDAY)
			)),
			(JInteger(Option(3)), new Interval(
				new DateTime().minusWeeks(2).withDayOfWeek(DateTimeConstants.MONDAY),
				new DateTime().minusWeeks(1).withDayOfWeek(DateTimeConstants.MONDAY)
			))
		)

		service.termService.getAcademicWeeksForYear(academicYear2013.dateInTermOne) returns termWeeks

		val student: StudentMember = Fixtures.student("1234")

		val module1: Module = Fixtures.module("aa101")
		module1.id = "aa101"
		val module2: Module = Fixtures.module("aa202")
		module2.id = "aa202"
		mockModuleAndDepartmentService.getModuleById(module1.id) returns Option(module1)
		mockModuleAndDepartmentService.getModuleById(module2.id) returns Option(module2)

		val group = new SmallGroup
		val groupSet = new SmallGroupSet
		groupSet.academicYear = academicYear2013
		group.groupSet = groupSet
		groupSet.module = module1

		val event = new SmallGroupEvent(group)
		event.day = DayOfWeek.Wednesday

		val occurrence = new SmallGroupEventOccurrence
		occurrence.event = event
		occurrence.week = 1
		occurrence.termService = service.termService

		val attendanceMarkedAsMissed = new SmallGroupEventAttendance
		attendanceMarkedAsMissed.occurrence = occurrence
		attendanceMarkedAsMissed.universityId = student.universityId
		attendanceMarkedAsMissed.state = AttendanceState.MissedAuthorised
		occurrence.attendance.add(attendanceMarkedAsMissed)
		attendanceMarkedAsMissed.updatedBy = "cusfal"

		service.profileService.getMemberByUniversityId(student.universityId) returns Option(student)

		val smallGroupPoint = new AttendanceMonitoringPoint
		// start date: Tuesday week 1
		smallGroupPoint.startDate = termWeeks.toMap.apply(JInteger(Option(1))).getStart.toLocalDate.withDayOfWeek(DateTimeConstants.TUESDAY)
		// end date: Thursday week 2
		smallGroupPoint.endDate = termWeeks.toMap.apply(JInteger(Option(2))).getStart.toLocalDate.withDayOfWeek(DateTimeConstants.THURSDAY)
		smallGroupPoint.pointType = AttendanceMonitoringPointType.SmallGroup
		smallGroupPoint.smallGroupEventModules = Seq()
		smallGroupPoint.smallGroupEventQuantity = 1
		smallGroupPoint.moduleAndDepartmentService = mockModuleAndDepartmentService

		service.termService.getAcademicWeekForAcademicYear(smallGroupPoint.startDate.toDateTimeAtStartOfDay, groupSet.academicYear) returns 1
		service.termService.getAcademicWeekForAcademicYear(smallGroupPoint.endDate.toDateTimeAtStartOfDay, groupSet.academicYear) returns 2

		service.attendanceMonitoringService.listStudentsPoints(student, None, groupSet.academicYear) returns Seq(smallGroupPoint)
		service.attendanceMonitoringService.getCheckpoints(Seq(smallGroupPoint), Seq(student)) returns Map()
		service.attendanceMonitoringService.studentAlreadyReportedThisTerm(student, smallGroupPoint) returns false
		service.attendanceMonitoringService.setAttendance(student, Map(smallGroupPoint -> AttendanceState.MissedAuthorised), attendanceMarkedAsMissed.updatedBy, autocreated = true) returns
			((Seq(Fixtures.attendanceMonitoringCheckpoint(smallGroupPoint, student, AttendanceState.MissedAuthorised)), Seq[AttendanceMonitoringCheckpointTotal]()))

	}

	@Test
	def updatesMissedCheckpoint() { new Fixture {
		service.smallGroupService.findAttendanceForStudentInModulesInWeeks(student, 1, 2, smallGroupPoint.smallGroupEventModules) returns Seq()
		service.smallGroupService.findOccurrencesInWeeks(1, 2, academicYear2013) returns Seq()
		service.smallGroupService.findAttendanceNotes(Seq("1234"), Seq()) returns Seq()
		service.getMissedCheckpoints(Seq(attendanceMarkedAsMissed)).size should be (1)
		service.updateMissedCheckpoints(Seq(attendanceMarkedAsMissed), currentUser)
		verify(service.attendanceMonitoringService, times(1)).setAttendance(student, Map(smallGroupPoint -> AttendanceState.MissedAuthorised), attendanceMarkedAsMissed.updatedBy, autocreated = true)
	}}

	@Test
	def updatesMissedCheckpointSpecificModule() {
		new Fixture {
			smallGroupPoint.smallGroupEventModules = Seq(groupSet.module)
			service.smallGroupService.findAttendanceForStudentInModulesInWeeks(student, 1, 2, smallGroupPoint.smallGroupEventModules) returns Seq()
			service.smallGroupService.findOccurrencesInModulesInWeeks(1, 2, smallGroupPoint.smallGroupEventModules, academicYear2013) returns Seq()
			service.smallGroupService.findAttendanceNotes(Seq("1234"), Seq()) returns Seq()
			service.getMissedCheckpoints(Seq(attendanceMarkedAsMissed)).size should be (1)
			service.updateMissedCheckpoints(Seq(attendanceMarkedAsMissed), currentUser)
			verify(service.attendanceMonitoringService, times(1)).setAttendance(student, Map(smallGroupPoint -> AttendanceState.MissedAuthorised), attendanceMarkedAsMissed.updatedBy, autocreated = true)
		}
	}

	@Test
	def updatesMissedCheckpointQuantityForMoreThanOnceAttendance() { new Fixture {
		val otherAttendance = new SmallGroupEventAttendance
		otherAttendance.occurrence = new SmallGroupEventOccurrence
		otherAttendance.occurrence.week = 2
		otherAttendance.occurrence.event = new SmallGroupEvent(group)
		otherAttendance.occurrence.event.day = DayOfWeek.Monday
		otherAttendance.universityId = student.universityId
		otherAttendance.state = AttendanceState.Attended
		otherAttendance.updatedBy = "cusxx"

		smallGroupPoint.smallGroupEventQuantity = 2
		service.smallGroupService.findAttendanceForStudentInModulesInWeeks(student, 1, 2, smallGroupPoint.smallGroupEventModules) returns Seq(attendanceMarkedAsMissed)
		service.smallGroupService.findOccurrencesInWeeks(1, 2, academicYear2013) returns Seq()
		service.smallGroupService.findAttendanceNotes(Seq("1234"), Seq()) returns Seq()

		service.getMissedCheckpoints(Seq(attendanceMarkedAsMissed)).size should be (1)
		service.updateMissedCheckpoints(Seq(attendanceMarkedAsMissed), currentUser)
		verify(service.attendanceMonitoringService, times(1)).setAttendance(student, Map(smallGroupPoint -> AttendanceState.MissedAuthorised), attendanceMarkedAsMissed.updatedBy, autocreated = true)
	}}

	@Test
	def notMissed() { new Fixture {
		attendanceMarkedAsMissed.state = AttendanceState.Attended
		service.getMissedCheckpoints(Seq(attendanceMarkedAsMissed)).size should be (0)
	}}
}
