package uk.ac.warwick.tabula.commands.groups

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent, SmallGroupEventOccurrence, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.{Module, UserGroup}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringEventAttendanceService, AttendanceMonitoringEventAttendanceServiceComponent}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

class RecordAttendanceCommandTest extends TestBase with Mockito {

  val smallGroupEventOccurrence: SmallGroupEventOccurrence = mock[SmallGroupEventOccurrence]
  val mockCurrentUser: CurrentUser = mock[CurrentUser]

  // Implements the dependencies declared by the command
  trait CommandTestSupport extends SmallGroupServiceComponent with UserLookupComponent with ProfileServiceComponent
    with FeaturesComponent with AttendanceMonitoringEventAttendanceServiceComponent {
    val smallGroupService: SmallGroupService = smartMock[SmallGroupService]
    val userLookup: UserLookupService = smartMock[UserLookupService]
    val profileService: ProfileService = smartMock[ProfileService]
    val attendanceMonitoringEventAttendanceService: AttendanceMonitoringEventAttendanceService = smartMock[AttendanceMonitoringEventAttendanceService]
    val features: FeaturesImpl = emptyFeatures

    def apply(): SmallGroupEventOccurrence = {
      smallGroupEventOccurrence
    }
  }

  @Test
  def commandApply() = withCurrentUser(mockCurrentUser) {
    val event = new SmallGroupEvent
    event.group = new SmallGroup
    event.group.groupSet = new SmallGroupSet
    event.group.groupSet.module = new Module
    event.group.groupSet.module.adminDepartment = Fixtures.department("in")
    event.group.groupSet.module.adminDepartment.autoMarkMissedMonitoringPoints = false
    val occurrence = new SmallGroupEventOccurrence
    occurrence.event = event

    val week = 1
    val user = new User("abcde")
    user.setWarwickId("1234567")

    val command = new RecordAttendanceCommand(event, week, currentUser) with CommandTestSupport
    command.smallGroupService.getOrCreateSmallGroupEventOccurrence(event, week) returns Option(occurrence)
    command.studentsState.put("1234567", SmallGroupAttendanceState.Attended)
    command.applyInternal()
    verify(command.userLookup, times(0)).getUsersByUserIds(Seq("abcde").asJava)
    verify(command.smallGroupService, times(1)).saveOrUpdateAttendance("1234567", event, week, SmallGroupAttendanceState.Attended, currentUser)
  }

  trait Fixture {
    val invalidUser = new User("invalid")
    invalidUser.setWarwickId("invalid")
    invalidUser.setFoundUser(false)

    val missingUser = new User("missing")
    missingUser.setWarwickId("missing")
    missingUser.setFoundUser(true)
    missingUser.setWarwickId("missing")

    val validUser = new User("valid")
    validUser.setWarwickId("valid")
    validUser.setFoundUser(true)
    validUser.setWarwickId("valid")

    val event = new SmallGroupEvent()
    val group = new SmallGroup()
    event.group = group
    val students: UserGroup = UserGroup.ofUsercodes
    group.students = students

    val set = new SmallGroupSet()
    group.groupSet = set
    set.academicYear = AcademicYear.now()

    val week = 1

    val command = new RecordAttendanceCommand(event, week, currentUser) with CommandTestSupport with RecordAttendanceCommandValidation with SmallGroupEventInFutureCheck
    command.userLookup.getUserByWarwickUniId(invalidUser.getWarwickId) returns invalidUser
    command.userLookup.getUserByWarwickUniId(missingUser.getWarwickId) returns missingUser
    command.userLookup.getUserByWarwickUniId(validUser.getWarwickId) returns validUser
    students.userLookup = command.userLookup
    students.userLookup.getUsersByUserIds(JArrayList(validUser.getUserId)) returns JMap(validUser.getUserId -> validUser)

    students.addUserId(validUser.getUserId)
  }

  @Test
  def validateInvalid() = withCurrentUser(mockCurrentUser) {
    new Fixture {
      command.studentsState = JHashMap()
      command.studentsState.put(invalidUser.getWarwickId, SmallGroupAttendanceState.Attended)
      command.studentsState.put(validUser.getWarwickId, SmallGroupAttendanceState.Attended)

      val errors = new BindException(command, "command")
      command.validate(errors)
      errors.hasFieldErrors should be (true)
      errors.getFieldError("studentsState").getArguments should have size 1
    }
  }

  @Test
  def validateMissing() = withCurrentUser(mockCurrentUser) {
    new Fixture {
      command.studentsState = JHashMap()
      command.studentsState.put(missingUser.getWarwickId, SmallGroupAttendanceState.Attended)
      command.studentsState.put(validUser.getWarwickId, SmallGroupAttendanceState.Attended)

      val errors = new BindException(command, "command")
      command.validate(errors)
      errors.hasFieldErrors should be (false) // TAB-1791
    }
  }

  @Test
  def validateValid() = withCurrentUser(mockCurrentUser) {
    new Fixture {
      command.studentsState = JHashMap()
      command.studentsState.put(validUser.getWarwickId, SmallGroupAttendanceState.Attended)

      val errors = new BindException(command, "command")
      command.validate(errors)
      errors.hasFieldErrors should be (false)
    }
  }

}
