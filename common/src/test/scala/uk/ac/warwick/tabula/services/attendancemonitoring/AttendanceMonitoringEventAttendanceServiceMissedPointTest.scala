package uk.ac.warwick.tabula.services.attendancemonitoring

import org.joda.time.DateTimeConstants
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.{Department, Module, StudentMember}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class AttendanceMonitoringEventAttendanceServiceMissedPointTest extends TestBase with Mockito {

  val mockModuleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]

  trait ServiceTestSupport extends SmallGroupServiceComponent
    with ProfileServiceComponent with AttendanceMonitoringServiceComponent {

    val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
    val profileService: ProfileService = smartMock[ProfileService]
    val smallGroupService: SmallGroupService = smartMock[SmallGroupService]
  }

  trait Fixture {
    val service = new AbstractAttendanceMonitoringEventAttendanceService with ServiceTestSupport

    val academicYear2013 = AcademicYear(2013)

    val student: StudentMember = Fixtures.student("1234")

    val department = Fixtures.department("aa")
    department.autoMarkMissedMonitoringPoints = true

    val module1: Module = Fixtures.module("aa101")
    module1.id = "aa101"
    module1.adminDepartment = department
    val module2: Module = Fixtures.module("aa202")
    module2.id = "aa202"
    module2.adminDepartment = department
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

    val attendanceMarkedAsMissed = new SmallGroupEventAttendance
    attendanceMarkedAsMissed.occurrence = occurrence
    attendanceMarkedAsMissed.universityId = student.universityId
    attendanceMarkedAsMissed.state = AttendanceState.MissedAuthorised
    occurrence.attendance.add(attendanceMarkedAsMissed)
    attendanceMarkedAsMissed.updatedBy = "cusfal"

    service.profileService.getMemberByUniversityId(student.universityId) returns Option(student)

    val smallGroupPoint = new AttendanceMonitoringPoint
    // start date: Tuesday week 1
    smallGroupPoint.startDate = academicYear2013.weeks(1).firstDay.withDayOfWeek(DateTimeConstants.TUESDAY)
    // end date: Thursday week 2
    smallGroupPoint.endDate = academicYear2013.weeks(2).firstDay.withDayOfWeek(DateTimeConstants.THURSDAY)
    smallGroupPoint.pointType = AttendanceMonitoringPointType.SmallGroup
    smallGroupPoint.smallGroupEventModules = Seq()
    smallGroupPoint.smallGroupEventQuantity = 1
    smallGroupPoint.moduleAndDepartmentService = mockModuleAndDepartmentService

    service.attendanceMonitoringService.listStudentsPoints(student, None, groupSet.academicYear) returns Seq(smallGroupPoint)
    service.attendanceMonitoringService.getCheckpoints(Seq(smallGroupPoint), Seq(student)) returns Map()
    service.attendanceMonitoringService.studentAlreadyReportedThisTerm(student, smallGroupPoint) returns false

    val setAttendanceResult: (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal]) =
      (
        Seq(Fixtures.attendanceMonitoringCheckpoint(smallGroupPoint, student, AttendanceState.MissedAuthorised)),
        Seq[AttendanceMonitoringCheckpointTotal]()
      )
    service.attendanceMonitoringService.setAttendance(student, Map(smallGroupPoint -> AttendanceState.MissedAuthorised), attendanceMarkedAsMissed.updatedBy, autocreated = true) returns setAttendanceResult

  }

  @Test
  def updatesMissedCheckpoint() {
    new Fixture {
      service.smallGroupService.findAttendanceForStudentInModulesInWeeks(student, 1, 2, academicYear2013, smallGroupPoint.smallGroupEventModules) returns Seq()
      service.smallGroupService.findOccurrencesInWeeks(1, 2, academicYear2013) returns Seq()
      service.smallGroupService.findAttendanceNotes(Seq("1234"), Seq()) returns Seq()
      service.getMissedCheckpoints(Seq(attendanceMarkedAsMissed)).size should be(1)
      service.updateMissedCheckpoints(Seq(attendanceMarkedAsMissed), currentUser)
      verify(service.attendanceMonitoringService, times(1)).setAttendance(student, Map(smallGroupPoint -> AttendanceState.MissedAuthorised), attendanceMarkedAsMissed.updatedBy, autocreated = true)
    }
  }

  @Test
  def updatesMissedCheckpointSpecificModule() {
    new Fixture {
      smallGroupPoint.smallGroupEventModules = Seq(groupSet.module)
      service.smallGroupService.findAttendanceForStudentInModulesInWeeks(student, 1, 2, academicYear2013, smallGroupPoint.smallGroupEventModules) returns Seq()
      service.smallGroupService.findOccurrencesInModulesInWeeks(1, 2, smallGroupPoint.smallGroupEventModules, academicYear2013) returns Seq()
      service.smallGroupService.findAttendanceNotes(Seq("1234"), Seq()) returns Seq()
      service.getMissedCheckpoints(Seq(attendanceMarkedAsMissed)).size should be(1)
      service.updateMissedCheckpoints(Seq(attendanceMarkedAsMissed), currentUser)
      verify(service.attendanceMonitoringService, times(1)).setAttendance(student, Map(smallGroupPoint -> AttendanceState.MissedAuthorised), attendanceMarkedAsMissed.updatedBy, autocreated = true)
    }
  }

  @Test
  def updatesMissedCheckpointQuantityForMoreThanOnceAttendance() {
    new Fixture {
      val otherAttendance = new SmallGroupEventAttendance
      otherAttendance.occurrence = new SmallGroupEventOccurrence
      otherAttendance.occurrence.week = 2
      otherAttendance.occurrence.event = new SmallGroupEvent(group)
      otherAttendance.occurrence.event.day = DayOfWeek.Monday
      otherAttendance.universityId = student.universityId
      otherAttendance.state = AttendanceState.Attended
      otherAttendance.updatedBy = "cusxx"

      smallGroupPoint.smallGroupEventQuantity = 2
      service.smallGroupService.findAttendanceForStudentInModulesInWeeks(student, 1, 2, academicYear2013, smallGroupPoint.smallGroupEventModules) returns Seq(attendanceMarkedAsMissed)
      service.smallGroupService.findOccurrencesInWeeks(1, 2, academicYear2013) returns Seq()
      service.smallGroupService.findAttendanceNotes(Seq("1234"), Seq()) returns Seq()

      service.getMissedCheckpoints(Seq(attendanceMarkedAsMissed)).size should be(1)
      service.updateMissedCheckpoints(Seq(attendanceMarkedAsMissed), currentUser)
      verify(service.attendanceMonitoringService, times(1)).setAttendance(student, Map(smallGroupPoint -> AttendanceState.MissedAuthorised), attendanceMarkedAsMissed.updatedBy, autocreated = true)
    }
  }

  @Test
  def ignoresAttendanceFromPreviousYears() {
    new Fixture {
      val groupOld = new SmallGroup
      val groupSetOld = new SmallGroupSet
      groupSetOld.academicYear = AcademicYear(2012)
      groupOld.groupSet = groupSetOld
      groupSetOld.module = module1

      val eventOld = new SmallGroupEvent(group)
      eventOld.day = DayOfWeek.Wednesday

      val occurrenceOld = new SmallGroupEventOccurrence
      occurrenceOld.event = event
      occurrenceOld.week = 1

      val attendanceOldMarkedAsAttended = new SmallGroupEventAttendance
      attendanceOldMarkedAsAttended.occurrence = occurrence
      attendanceOldMarkedAsAttended.universityId = student.universityId
      attendanceOldMarkedAsAttended.state = AttendanceState.Attended
      occurrenceOld.attendance.add(attendanceOldMarkedAsAttended)
      attendanceOldMarkedAsAttended.updatedBy = "cusfal"

      service.smallGroupService.findAttendanceForStudentInModulesInWeeks(student, 1, 2, academicYear2013, smallGroupPoint.smallGroupEventModules) returns Seq(attendanceMarkedAsMissed)
      service.smallGroupService.findOccurrencesInWeeks(1, 2, academicYear2013) returns Seq()
      service.smallGroupService.findAttendanceNotes(Seq("1234"), Seq()) returns Seq()

      service.getMissedCheckpoints(Seq(attendanceMarkedAsMissed)).size should be(1)
      service.updateMissedCheckpoints(Seq(attendanceMarkedAsMissed), currentUser)
      verify(service.attendanceMonitoringService, times(1)).setAttendance(student, Map(smallGroupPoint -> AttendanceState.MissedAuthorised), attendanceMarkedAsMissed.updatedBy, autocreated = true)
    }
  }

  @Test
  def notMissed() {
    new Fixture {
      attendanceMarkedAsMissed.state = AttendanceState.Attended
      service.getMissedCheckpoints(Seq(attendanceMarkedAsMissed)).size should be(0)
    }
  }
}
