package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}

class UnlinkAttendanceMonitoringSchemeCommandTest extends TestBase with Mockito {

  @Test
  def commandApply(): Unit = withFakeTime(new DateTime(2015, DateTimeConstants.JULY, 1, 0, 0, 0, 0)) {
    val student1 = Fixtures.student("1111", "1111")
    val student2 = Fixtures.student("2222", "2222")
    val student3 = Fixtures.student("3333", "3333")

    val dept1 = Fixtures.department("its")

    val dept1scheme1 = new AttendanceMonitoringScheme
    dept1scheme1.department = dept1
    dept1scheme1.academicYear = AcademicYear(2014)
    dept1scheme1.attendanceMonitoringService = None
    val ug = UserGroup.ofUniversityIds
    ug.staticUserIds = Set(student1.userId, student2.userId)
    ug.includedUserIds = Set(student3.userId)
    ug.excludedUserIds = Set(student2.userId)
    dept1scheme1.members = ug
    dept1scheme1.memberQuery = "some-filter"

    val command = new UnlinkAttendanceMonitoringSchemeCommandInternal with AttendanceMonitoringServiceComponent {
      val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
    }

    command.attendanceMonitoringService.findSchemesLinkedToSITSByDepartment(AcademicYear(2014)) returns Map(
      dept1 -> Seq(dept1scheme1)
    )

    val result = command.applyInternal()
    result.get(dept1).isDefined should be (true)
    result(dept1).size should be(1)
    result(dept1).head.members.members.size should be(2)
    result(dept1).head.members.includesUserId(student1.userId) should be (true)
    result(dept1).head.members.includesUserId(student2.userId) should be (false)
    result(dept1).head.members.includesUserId(student3.userId) should be (true)
    verify(command.attendanceMonitoringService, times(1)).saveOrUpdate(dept1scheme1)
  }

}
