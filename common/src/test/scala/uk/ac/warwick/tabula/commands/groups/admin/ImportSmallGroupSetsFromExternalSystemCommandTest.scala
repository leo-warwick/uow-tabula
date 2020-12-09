package uk.ac.warwick.tabula.commands.groups.admin

import org.joda.time.LocalTime
import org.junit.Before
import uk.ac.warwick.spring.SpringConfigurer
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.DescriptionImpl
import uk.ac.warwick.tabula.commands.groups.admin.ImportSmallGroupSetsFromExternalSystemCommand.TimetabledSmallGroupEvent
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.{Department, Location, Module, NamedLocation}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.services.timetables.{ModuleTimetableFetchingService, ModuleTimetableFetchingServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.userlookup.User

import scala.concurrent.Future

class ImportSmallGroupSetsFromExternalSystemCommandTest extends TestBase with Mockito {

  @Before def tidyUpContext(): Unit = {
    // TODO it would be better to find where this context is actually coming from
    SpringConfigurer.applicationContext = null
  }

  private trait CommandTestSupport extends ModuleTimetableFetchingServiceComponent
    with ImportSmallGroupSetsFromExternalSystemPermissionsRestrictedState
    with SmallGroupServiceComponent
    with SmallGroupSetGenerator
    with SmallGroupEventGenerator
    with UserLookupComponent
    with ModuleAndDepartmentServiceComponent
    with SecurityServiceComponent {
    self: ImportSmallGroupSetsFromExternalSystemCommandState =>

    val timetableFetchingService: ModuleTimetableFetchingService = mock[ModuleTimetableFetchingService]
    val smallGroupService: SmallGroupService = mock[SmallGroupService]
    val userLookup = new MockUserLookup
    val moduleAndDepartmentService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]
    val securityService: SecurityService = mock[SecurityService]

    def createSet(module: Module, format: SmallGroupFormat, name: String): SmallGroupSet = {
      val set = new SmallGroupSet(module)
      set.format = format
      set.name = name

      module.groupSets.add(set)
      set
    }

    def createEvent(module: Module, set: SmallGroupSet, group: SmallGroup, weeks: Seq[WeekRange], day: DayOfWeek, startTime: LocalTime, endTime: LocalTime, location: Option[Location], title: String, tutorUsercodes: Seq[String]): SmallGroupEvent = {
      val event = new SmallGroupEvent(group)
      event.title = title
      event.weekRanges = weeks
      event.day = day
      event.startTime = startTime
      event.endTime = endTime
      location.foreach { location => event.location = location }
      event.tutors.knownType.includedUserIds = tutorUsercodes.toSet

      group.addEvent(event)
      event
    }
  }

  private trait Fixture {
    val department: Department = Fixtures.department("in")

    val module1: Module = Fixtures.module("in101")
    val module2: Module = Fixtures.module("in102")

    department.modules.add(module1)
    department.modules.add(module2)

    val tutor = new User("abcdef")
    tutor.setFoundUser(true)
    tutor.setWarwickId("1170047")

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

    val user = new User("cuscav")
    val currentUser = new CurrentUser(user, user)
  }

  private trait CommandFixture extends Fixture {
    val command = new ImportSmallGroupSetsFromExternalSystemCommandInternal(department, currentUser) with CommandTestSupport

    command.userLookup.registerUserObjects(tutor)

    command.academicYear = AcademicYear(2012)

    command.securityService.can(currentUser, Permissions.SmallGroups.ImportFromExternalSystem, department) returns true

    command.userLookup.registerUserObjects(student1, student2, student3, student4, student5, student6)
  }

  private trait FixtureWithSingleSeminarForYear {
    self: CommandFixture =>

    val tEventModule1Seminar1 = TimetableEvent(
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
      parent = TimetableEvent.Parent(Some(module1)),
      comments = None,
      staff = Seq(tutor),
      students = Seq(student1, student2, student3),
      year = AcademicYear(2012),
      relatedUrl = None,
      attendance = Map(),
      sgtGroupId = None,
      sgtGroupEventId = None
    )
    val tEventModule1Seminar2 = TimetableEvent(
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
      parent = TimetableEvent.Parent(Some(module1)),
      comments = None,
      staff = Seq(tutor),
      students = Seq(student4, student5, student6),
      year = AcademicYear(2012),
      relatedUrl = None,
      attendance = Map(),
      sgtGroupId = None,
      sgtGroupEventId = None
    )

    command.timetableFetchingService.getTimetableForModule("IN101", includeStudents = true) returns Future.successful(EventList.fresh(Seq(
      tEventModule1Seminar1, tEventModule1Seminar2
    )))
    command.timetableFetchingService.getTimetableForModule("IN102", includeStudents = true) returns Future.successful(EventList.fresh(Seq(
      TimetableEvent(
        uid = "uuid4",
        name = "IN102S",
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
        parent = TimetableEvent.Parent(Some(module2)),
        comments = None,
        staff = Seq(tutor),
        students = Seq(student4, student5, student6),
        year = AcademicYear(2013),
        relatedUrl = None,
        attendance = Map(),
        sgtGroupId = None,
        sgtGroupEventId = None
      )
    )))
  }

  private trait FixtureWithSingleLectureForYear {
    self: CommandFixture =>

    val tEventLecture1 = TimetableEvent(
      uid = "uuid1",
      name = "IN101S",
      title = "",
      description = "",
      startTime = new LocalTime(9, 0),
      endTime = new LocalTime(10, 0),
      weekRanges = Seq(WeekRange(1, 5)),
      day = DayOfWeek.Tuesday,
      eventType = TimetableEventType.Lecture,
      location = Some(NamedLocation("CS1.01")),
      onlineDeliveryUrl = None,
      deliveryMethod = None,
      parent = TimetableEvent.Parent(Some(module1)),
      comments = None,
      staff = Seq(tutor),
      students = Seq(student1, student2, student3, student4, student5),
      year = AcademicYear(2012),
      relatedUrl = None,
      attendance = Map(),
      sgtGroupId = None,
      sgtGroupEventId = None
    )

    command.timetableFetchingService.getTimetableForModule("IN101", includeStudents = true) returns Future.successful(EventList.fresh(Seq(
      tEventLecture1
    )))
    command.timetableFetchingService.getTimetableForModule("IN102", includeStudents = true) returns Future.successful(EventList.fresh(Seq(
      TimetableEvent(
        uid = "uuid4",
        name = "IN102S",
        title = "",
        description = "",
        startTime = new LocalTime(12, 0),
        endTime = new LocalTime(13, 0),
        weekRanges = Seq(WeekRange(6, 10)),
        day = DayOfWeek.Thursday,
        eventType = TimetableEventType.Lecture,
        location = Some(NamedLocation("CS1.04")),
        onlineDeliveryUrl = None,
        deliveryMethod = None,
        parent = TimetableEvent.Parent(Some(module2)),
        comments = None,
        staff = Seq(tutor),
        students = Seq(student4, student5, student6),
        year = AcademicYear(2013),
        relatedUrl = None,
        attendance = Map(),
        sgtGroupId = None,
        sgtGroupEventId = None
      )
    )))
  }

  @Test def init(): Unit = {
    new CommandFixture with FixtureWithSingleSeminarForYear {
      command.canManageDepartment should be (true)
      command.modules should be(Seq(module1, module2))
      command.timetabledEvents should be(Seq(
        TimetabledSmallGroupEvent(module1, TimetableEventType.Seminar, Seq(tEventModule1Seminar2, tEventModule1Seminar1))
      ))
    }
  }

  @Test def apply(): Unit = {
    new CommandFixture with FixtureWithSingleSeminarForYear {
      val sets: Seq[SmallGroupSet] = command.applyInternal()

      verify(command.smallGroupService, times(1)).saveOrUpdate(any[SmallGroupSet])
      verify(command.smallGroupService, times(2)).saveOrUpdate(any[SmallGroup])

      sets.size should be(1)

      val set: SmallGroupSet = sets.head
      set.format should be(SmallGroupFormat.Seminar)
      set.name should be("IN101 Seminars")
      set.groups.size should be(2)

      val group1: SmallGroup = set.groups.get(1) // Order intentionally reversed; the events are re-ordered because Thursday is before Friday
      group1.name = "Group 1"
      group1.students.knownType.includedUserIds should be(Set("0000001", "0000002", "0000003"))
      group1.events.size should be(1)

      val group1event: SmallGroupEvent = group1.events.head
      group1event.weekRanges should be(Seq(WeekRange(6, 10)))
      group1event.day should be(DayOfWeek.Friday)
      group1event.startTime should be(new LocalTime(12, 0))
      group1event.endTime should be(new LocalTime(13, 0))
      group1event.location should be(NamedLocation("CS1.04"))
      group1event.tutors.knownType.includedUserIds should be(Set("abcdef"))

      val group2: SmallGroup = set.groups.get(0) // Order intentionally reversed; the events are re-ordered because Thursday is before Friday
      group2.name = "Group 2"
      group2.students.knownType.includedUserIds should be(Set("0000004", "0000005", "0000006"))

      val group2event: SmallGroupEvent = group2.events.head
      group2event.weekRanges should be(Seq(WeekRange(6, 10)))
      group2event.day should be(DayOfWeek.Thursday)
      group2event.startTime should be(new LocalTime(12, 0))
      group2event.endTime should be(new LocalTime(13, 0))
      group2event.location should be(NamedLocation("CS1.04"))
      group2event.tutors.knownType.includedUserIds should be(Set("abcdef"))
    }
  }

  @Test def applyLectureEvents(): Unit = {
    new CommandFixture with FixtureWithSingleLectureForYear {
      val sets: Seq[SmallGroupSet] = command.applyInternal()

      verify(command.smallGroupService, times(1)).saveOrUpdate(any[SmallGroupSet])
      verify(command.smallGroupService, times(1)).saveOrUpdate(any[SmallGroup])

      sets.size should be(1)

      val set: SmallGroupSet = sets.head
      set.format should be(SmallGroupFormat.Lecture)
      set.name should be("IN101 Lectures")
      set.groups.size should be(1)

      val group: SmallGroup = set.groups.get(0) // Order intentionally reversed; the events are re-ordered because Thursday is before Friday
      group.name = "Group 1"
      group.students.knownType.includedUserIds should be(Set("0000001", "0000002", "0000003","0000004","0000005"))
      group.events.size should be(1)

      val group1event: SmallGroupEvent = group.events.head
      group1event.weekRanges should be(Seq(WeekRange(1, 5)))
      group1event.day should be(DayOfWeek.Tuesday)
      group1event.startTime should be(new LocalTime(9, 0))
      group1event.endTime should be(new LocalTime(10, 0))
      group1event.location should be(NamedLocation("CS1.01"))
      group1event.tutors.knownType.includedUserIds should be(Set("abcdef"))

    }
  }

  private trait PermissionsTestSupport extends ImportSmallGroupSetsFromExternalSystemPermissionsRestrictedState
    with SecurityServiceComponent with ModuleAndDepartmentServiceComponent {
    self: ImportSmallGroupSetsFromExternalSystemCommandState with RequiresPermissionsChecking =>

    val securityService: SecurityService = mock[SecurityService]
    val moduleAndDepartmentService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]
  }

  private trait PermissionsFixture extends Fixture {
    val command = new ImportSmallGroupSetsFromExternalSystemPermissions with ImportSmallGroupSetsFromExternalSystemCommandState with PermissionsTestSupport {
      val department: Department = PermissionsFixture.this.department
      val user: CurrentUser = PermissionsFixture.this.currentUser
    }
  }

  private trait DepartmentalAdministratorPermissions {
    self: PermissionsFixture =>

    command.securityService.can(currentUser, Permissions.SmallGroups.ImportFromExternalSystem, department) returns true
  }

  private trait ModuleManagerPermissions {
    self: PermissionsFixture =>

    command.securityService.can(currentUser, Permissions.SmallGroups.ImportFromExternalSystem, department) returns false
    command.moduleAndDepartmentService.modulesWithPermission(currentUser, Permissions.SmallGroups.ImportFromExternalSystem, department) returns Set(module1, module2)
  }

  private trait NoPermissions {
    self: PermissionsFixture =>

    command.securityService.can(currentUser, Permissions.SmallGroups.ImportFromExternalSystem, department) returns false
    command.moduleAndDepartmentService.modulesWithPermission(currentUser, Permissions.SmallGroups.ImportFromExternalSystem, department) returns Set.empty
  }

  @Test def deptAdminPermissions(): Unit = {
    new PermissionsFixture with DepartmentalAdministratorPermissions {
      val checking: PermissionsChecking = mock[PermissionsChecking]
      command.permissionsCheck(checking)

      verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.ImportFromExternalSystem, department)
    }
  }

  @Test def noPermissions(): Unit = {
    new PermissionsFixture with DepartmentalAdministratorPermissions {
      val checking: PermissionsChecking = mock[PermissionsChecking]
      command.permissionsCheck(checking)

      verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.ImportFromExternalSystem, department)
    }
  }

  @Test def moduleManagerPermissions(): Unit = {
    new PermissionsFixture with ModuleManagerPermissions {
      val checking: PermissionsChecking = mock[PermissionsChecking]
      command.permissionsCheck(checking)

      verify(checking, times(1)).PermissionCheckAll(Permissions.SmallGroups.ImportFromExternalSystem, Seq(module1, module2))
    }
  }

  @Test def description(): Unit = {
    val command = new ImportSmallGroupSetsFromExternalSystemDescription with ImportSmallGroupSetsFromExternalSystemCommandState {
      override val eventName: String = "test"
      val department: Department = Fixtures.department("in")
      val user: CurrentUser = mock[CurrentUser]
    }

    val d = new DescriptionImpl
    command.describe(d)

    d.allProperties should be(Map(
      "department" -> "in"
    ))
  }

}
