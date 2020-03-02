package uk.ac.warwick.tabula.commands.attendance.view

import org.springframework.validation.BindException
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}

class ReportStudentsConfirmCommandTest extends TestBase with Mockito {

  trait CommandTestSupport extends ReportStudentsConfirmCommandState with AttendanceMonitoringServiceComponent {
    val department: Department = Fixtures.department("its")
    val academicYear = AcademicYear(2014)
    val user: CurrentUser = null
    val profileService: ProfileService = smartMock[ProfileService]
    val securityService: SecurityService = smartMock[SecurityService]
    val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
  }

  @Test
  def validateInvalidPeriod(): Unit = {
    val validator = new ReportStudentsConfirmValidation with CommandTestSupport {
      override lazy val availablePeriods = Seq(("Autumn", true), ("Spring", false))
      override lazy val studentMissedReportCounts = Seq()
    }
    validator.period = "Summer"
    val errors = new BindException(validator, "command")
    validator.validate(errors)
    errors.hasFieldErrors should be (true)
    errors.getFieldErrors("availablePeriods").size() should be(1)
  }

  @Test
  def validateUnavailablePeriod(): Unit = {
    val validator = new ReportStudentsConfirmValidation with CommandTestSupport {
      override lazy val availablePeriods = Seq(("Autumn", true), ("Spring", false))
      override lazy val studentMissedReportCounts = Seq()
    }
    validator.period = "Spring"
    val errors = new BindException(validator, "command")
    validator.validate(errors)
    errors.hasFieldErrors should be (true)
    errors.getFieldErrors("availablePeriods").size() should be(1)
  }

  @Test
  def validateNoStudents(): Unit = {
    val validator = new ReportStudentsConfirmValidation with CommandTestSupport {
      override lazy val availablePeriods = Seq(("Autumn", true), ("Spring", false))
      override lazy val studentMissedReportCounts = Seq()
    }
    validator.period = "Autumn"
    val errors = new BindException(validator, "command")
    validator.validate(errors)
    errors.hasFieldErrors should be (true)
    errors.getFieldErrors("studentMissedReportCounts").size() should be(1)
  }

  @Test
  def validateNotConfirmed(): Unit = {
    val validator = new ReportStudentsConfirmValidation with CommandTestSupport {
      override lazy val availablePeriods = Seq(("Autumn", true), ("Spring", false))
      override lazy val studentMissedReportCounts = Seq()
    }
    validator.period = "Autumn"
    val errors = new BindException(validator, "command")
    validator.validate(errors)
    errors.hasFieldErrors should be (true)
    errors.getFieldErrors("confirm").size() should be(1)
  }

  @Test
  def validateValid(): Unit = {
    val validator = new ReportStudentsConfirmValidation with CommandTestSupport {
      override lazy val availablePeriods = Seq(("Autumn", true), ("Spring", false))
      override lazy val studentMissedReportCounts = Seq(StudentReportCount(null, 1, 0))
    }
    validator.period = "Autumn"
    validator.confirm = true
    val errors = new BindException(validator, "command")
    validator.validate(errors)
    errors.hasFieldErrors should be (false)
  }

}
