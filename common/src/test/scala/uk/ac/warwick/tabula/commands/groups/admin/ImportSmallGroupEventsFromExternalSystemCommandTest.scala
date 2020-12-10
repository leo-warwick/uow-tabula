package uk.ac.warwick.tabula.commands.groups.admin

import org.joda.time.LocalTime
import org.junit.Before
import uk.ac.warwick.spring.SpringConfigurer
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.DescriptionImpl
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.{Location, Module, NamedLocation}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.services.timetables.{ModuleTimetableFetchingService, ModuleTimetableFetchingServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.userlookup.User

import scala.collection.mutable
import scala.concurrent.Future

class ImportSmallGroupEventsFromExternalSystemCommandTest extends TestBase with Mockito {

  @Before def tidyUpContext(): Unit = {
    // TODO it would be better to find where this context is actually coming from
    SpringConfigurer.applicationContext = null
  }

  val timetableFetchingService: ModuleTimetableFetchingService = mock[ModuleTimetableFetchingService]
  val smallGroupService: SmallGroupService = mock[SmallGroupService]
  val userLookup = new MockUserLookup

  private trait MockServices extends ModuleTimetableFetchingServiceComponent
    with SmallGroupServiceComponent
    with UserLookupComponent {

    val timetableFetchingService: ModuleTimetableFetchingService = ImportSmallGroupEventsFromExternalSystemCommandTest.this.timetableFetchingService
    val smallGroupService: SmallGroupService = ImportSmallGroupEventsFromExternalSystemCommandTest.this.smallGroupService
    val userLookup: MockUserLookup = ImportSmallGroupEventsFromExternalSystemCommandTest.this.userLookup

  }

  private trait CommandTestSupport extends SmallGroupEventGenerator {
    self: MockServices with ImportSmallGroupEventsFromExternalSystemCommandState =>

    def createEvent(module: Module, set: SmallGroupSet, group: SmallGroup, weeks: Seq[WeekRange], day: DayOfWeek, startTime: LocalTime, endTime: LocalTime, location: Option[Location], title: String, tutorUsercodes: Seq[String]): SmallGroupEvent = {
      val event = new SmallGroupEvent(group)
      updateEvent(module, set, group, event, weeks, day, startTime, endTime, location, title, tutorUsercodes)

      group.addEvent(event)
      event
    }

    def updateEvent(module: Module, set: SmallGroupSet, group: SmallGroup, event: SmallGroupEvent, weeks: Seq[WeekRange], day: DayOfWeek, startTime: LocalTime, endTime: LocalTime, location: Option[Location], title: String, tutorUsercodes: Seq[String]): SmallGroupEvent = {
      event.title = title
      event.weekRanges = weeks
      event.day = day
      event.startTime = startTime
      event.endTime = endTime
      location.foreach { location => event.location = location }
      event.tutors.knownType.includedUserIds = tutorUsercodes.toSet

      event
    }
  }

  private trait Fixture {
    val module: Module = Fixtures.module("in101")
    val set: SmallGroupSet = Fixtures.smallGroupSet("IN101 Seminars")
    val group1: SmallGroup = Fixtures.smallGroup("Group 1")
    val group2: SmallGroup = Fixtures.smallGroup("Group 2")
  }

  private trait CommandFixture extends Fixture {
    val command =
      new ImportSmallGroupEventsFromExternalSystemCommandInternal(module, set)
        with CommandTestSupport
        with MockServices
        with LookupEventsFromModuleTimetable
  }

  private trait FixtureWithSingleSeminarForYear extends Fixture with MockServices {
    set.academicYear = AcademicYear(2012)

    val tutor = new User("abcdef")
    tutor.setFoundUser(true)
    tutor.setWarwickId("1170047")
    userLookup.registerUserObjects(tutor)

    val student1 = new User("student1")
    student1.setFoundUser(true)
    student1.setWarwickId("0000001")

    val student2 = new User("student2")
    student2.setFoundUser(true)
    student2.setWarwickId("0000002")

    val student3 = new User("student3")
    student3.setFoundUser(true)
    student3.setWarwickId("0000003")

    val student4 = new User("student4")
    student4.setFoundUser(true)
    student4.setWarwickId("0000004")

    val student5 = new User("student5")
    student5.setFoundUser(true)
    student5.setWarwickId("0000005")

    val student6 = new User("student6")
    student6.setFoundUser(true)
    student6.setWarwickId("0000006")

    userLookup.registerUserObjects(student1, student2, student3, student4, student5, student6)

    val tEventSeminar1 = TimetableEvent(
      uid = "uuid1",
      name = "IN101S",
      title = "",
      description = "",
      startTime = new LocalTime(12, 0),
      endTime = new LocalTime(13, 0),
      weekRanges = Seq(WeekRange(6, 10)),
      day = DayOfWeek.Friday,
      eventType = TimetableEventType.Seminar,
      location = Some(NamedLocation("CS1.04")),
      onlineDeliveryUrl = None,
      deliveryMethod = None,
      parent = TimetableEvent.Parent(Some(module)),
      comments = None,
      staff = Seq(tutor),
      students = Seq(student1, student2, student3),
      year = AcademicYear(2012),
      relatedUrl = None,
      attendance = Map(),
      sgtGroupId = None,
      sgtGroupEventId = None
    )
    val tEventSeminar2 = TimetableEvent(
      uid = "uuid2",
      name = "IN101S",
      title = "",
      description = "",
      startTime = new LocalTime(12, 0),
      endTime = new LocalTime(13, 0),
      weekRanges = Seq(WeekRange(6, 10)),
      day = DayOfWeek.Thursday,
      eventType = TimetableEventType.Seminar,
      location = Some(NamedLocation("CS1.04")),
      onlineDeliveryUrl = None,
      deliveryMethod = None,
      parent = TimetableEvent.Parent(Some(module)),
      comments = None,
      staff = Seq(tutor),
      students = Seq(student4, student5, student6),
      year = AcademicYear(2012),
      relatedUrl = None,
      attendance = Map(),
      sgtGroupId = None,
      sgtGroupEventId = None
    )

    timetableFetchingService.getTimetableForModule("IN101", includeStudents = false) returns Future.successful(EventList.fresh(Seq(
      tEventSeminar1, tEventSeminar2
    )))
  }

  @Test def init(): Unit = {
    new FixtureWithSingleSeminarForYear with CommandFixture {
      command.eventsToImport.size() should be(2)
      command.eventsToImport.get(0).timetableEvent should be(tEventSeminar2)
      command.eventsToImport.get(1).timetableEvent should be(tEventSeminar1)
    }
  }

  @Test def apply(): Unit = {
    new FixtureWithSingleSeminarForYear with CommandFixture {
      command.eventsToImport.get(0).group = group1
      command.eventsToImport.get(1).group = group2

      val sets: Seq[SmallGroupEvent] = command.applyInternal()

      verify(command.smallGroupService, times(1)).saveOrUpdate(set)

      val group1event: SmallGroupEvent = group1.events.head
      group1event.weekRanges should be(Seq(WeekRange(6, 10)))
      group1event.day should be(DayOfWeek.Thursday)
      group1event.startTime should be(new LocalTime(12, 0))
      group1event.endTime should be(new LocalTime(13, 0))
      group1event.location should be(NamedLocation("CS1.04"))
      group1event.tutors.knownType.includedUserIds should be(Set("abcdef"))

      val group2event: SmallGroupEvent = group2.events.head
      group2event.weekRanges should be(Seq(WeekRange(6, 10)))
      group2event.day should be(DayOfWeek.Friday)
      group2event.startTime should be(new LocalTime(12, 0))
      group2event.endTime should be(new LocalTime(13, 0))
      group2event.location should be(NamedLocation("CS1.04"))
      group2event.tutors.knownType.includedUserIds should be(Set("abcdef"))
    }
  }

  private trait PermissionsFixture extends Fixture {
    val command = new ImportSmallGroupEventsFromExternalSystemPermissions with ImportSmallGroupEventsFromExternalSystemCommandState {
      val module: Module = PermissionsFixture.this.module
      val set: SmallGroupSet = PermissionsFixture.this.set
    }
  }

  @Test def permissions(): Unit = {
    new PermissionsFixture {
      module.groupSets.add(set)
      set.module = module

      val checking: PermissionsChecking = mock[PermissionsChecking]
      command.permissionsCheck(checking)

      verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Update, set)
    }
  }

  @Test(expected = classOf[ItemNotFoundException])
  def notLinked(): Unit = {
    new PermissionsFixture {
      val checking: PermissionsChecking = mock[PermissionsChecking]
      command.permissionsCheck(checking)

      fail("Expected exception")
    }
  }

  @Test def description(): Unit = {
    val command = new ImportSmallGroupEventsFromExternalSystemDescription with ImportSmallGroupEventsFromExternalSystemCommandState {
      override val eventName: String = "test"
      val module: Module = Fixtures.module("in101")
      val set: SmallGroupSet = Fixtures.smallGroupSet("IN101 Seminars")

      module.id = "moduleId"
      set.id = "setId"

      module.groupSets.add(set)
      set.module = module
    }

    val d = new DescriptionImpl
    command.describe(d)

    d.allProperties should be(Map(
      "smallGroupSet" -> "setId",
      "module" -> "moduleId",
      "moduleCode" -> "in101",
    ))
  }

}
