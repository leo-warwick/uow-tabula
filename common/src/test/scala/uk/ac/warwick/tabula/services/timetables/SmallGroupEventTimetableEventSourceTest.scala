package uk.ac.warwick.tabula.services.timetables

import org.joda.time.LocalTime
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.{Module, NamedLocation, StudentMember}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class SmallGroupEventTimetableEventSourceTest extends TestBase with Mockito {

  val mockSmallGroupService: SmallGroupService = smartMock[SmallGroupService]
  val mockUserLookup: UserLookupService = smartMock[UserLookupService]
  val mockSecurityService: SecurityService = smartMock[SecurityService]

  val eventSource: StudentTimetableEventSource = new SmallGroupEventTimetableEventSourceComponentImpl with UserLookupComponent with SmallGroupServiceComponent with SecurityServiceComponent {
    val smallGroupService: SmallGroupService = mockSmallGroupService
    val userLookup: UserLookupService = mockUserLookup
    val securityService: SecurityService = mockSecurityService
  }.studentGroupEventSource

  val student: StudentMember = Fixtures.student(universityId = "0000001", userId = "studentUserId")
  val studentUser = new User
  mockUserLookup.getUserByUserId(any[String]) returns studentUser

  val group = new SmallGroup
  val event = new SmallGroupEvent
  event.day = DayOfWeek.Monday
  event.startTime = LocalTime.now
  event.endTime = event.startTime.plusHours(1)
  event.group = group
  event.location = NamedLocation("location")
  group.addEvent(event)
  group.name = "group name"
  group.groupSet = new SmallGroupSet
  group.groupSet.format = SmallGroupFormat.Lab
  group.groupSet.releasedToStudents = true
  group.groupSet.module = new Module
  group.groupSet.module.code = "modcode"
  group.groupSet.name = "groupset name"


  @Test
  def translatesFromSmallGroupEventToTimetableEvent(): Unit = {
    mockSmallGroupService.findSmallGroupsByStudent(any[User], any[Seq[AcademicYear]]) returns Seq(group)
    mockSmallGroupService.findSmallGroupEventsByTutor(any[User], any[Seq[AcademicYear]]) returns Nil
    mockSmallGroupService.findManuallyAddedAttendance(any[String]) returns Nil
    mockSmallGroupService.findStudentAttendanceInEvents(any[String], any[Seq[SmallGroupEvent]]) returns Nil
    val events = eventSource.eventsFor(student, currentUser, TimetableEvent.Context.Student).futureValue.events
    events.size should be(1)

    val tte: TimetableEvent = events.head
    tte.day should be(DayOfWeek.Monday)
    tte.description should be("groupset name: group name")
    tte.endTime should be(event.endTime)
    tte.eventType should be(TimetableEventType.Practical)
    tte.location should be(Some(NamedLocation("location")))
    tte.parent.shortName should be(Some("MODCODE"))
    tte.name should be("groupset name: group name")
    tte.startTime should be(event.startTime)
  }

}
