package uk.ac.warwick.tabula.commands.attendance.report

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.DescriptionImpl
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{SecurityService, SecurityServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.userlookup.User

import scala.collection.immutable.ListMap

class CreateMonitoringPointReportCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends CreateMonitoringPointReportCommandState
		with AttendanceMonitoringServiceComponent with SecurityServiceComponent {
		val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
		val securityService: SecurityService = smartMock[SecurityService]
	}

	private trait Fixture {
		val department: Department = Fixtures.department("in")

		val user = new User("cuscav")
		user.setDepartmentCode("in")
		user.setWarwickId("0672089")

		val currentUser = new CurrentUser(user, user)
	}

	private trait CommandFixture extends Fixture {
		val command = new CreateMonitoringPointReportCommandInternal(department, currentUser) with CommandTestSupport
	}

	@Test def apply() { new CommandFixture {
		command.academicYear = new AcademicYear(2013)
		command.period = "Autumn"

		val student1: StudentMember = Fixtures.student("1234567")
		student1.freshStudentCourseDetails.head.scjCode = "1234567/1"
		student1.freshStudentCourseDetails.head.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(academicYear = command.academicYear, studentCourseDetails = student1.freshStudentCourseDetails.head)
		)
		val student2: StudentMember = Fixtures.student("9283845")
		student2.freshStudentCourseDetails.head.scjCode = "9283845/1"
		student2.freshStudentCourseDetails.head.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(academicYear = command.academicYear, studentCourseDetails = student2.freshStudentCourseDetails.head)
		)

		command.missedPoints = ListMap(
			student1 -> 3,
			student2 -> 2
		)

		val reports: Seq[MonitoringPointReport] = command.applyInternal()
		reports.size should be (2)

		reports.head.academicYear should be (command.academicYear)
		reports.head.missed should be (3)
		reports.head.monitoringPeriod should be ("Autumn")
		reports.head.reporter should be ("IN0672089")
		reports.head.student should be (student1)

		reports(1).academicYear should be (command.academicYear)
		reports(1).missed should be (2)
		reports(1).monitoringPeriod should be ("Autumn")
		reports(1).reporter should be ("IN0672089")
		reports(1).student should be (student2)

		verify(command.attendanceMonitoringService, times(2)).saveOrUpdate(any[MonitoringPointReport])
	}}

	@Test def permissions() {
		val command = new CreateMonitoringPointReportCommandPermissions with CreateMonitoringPointReportCommandState {
			val department: Department = Fixtures.department("in")
			val currentUser: CurrentUser = mock[CurrentUser]
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.MonitoringPoints.Report, command.department)
	}

	@Test(expected = classOf[ItemNotFoundException]) def noDepartment() {
		val command = new CreateMonitoringPointReportCommandPermissions with CreateMonitoringPointReportCommandState {
			val department = null
			val currentUser: CurrentUser = mock[CurrentUser]
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	private trait ValidationFixture extends Fixture {
		val d: Department = department
		val u: CurrentUser = currentUser

		val command = new CreateMonitoringPointReportCommandValidation with CommandTestSupport {
			val department: Department = d
			val currentUser: CurrentUser = u
		}
	}

	@Test def validateNoErrors() { new ValidationFixture {
		command.academicYear = new AcademicYear(2013)
		command.period = "Autumn"

		val student1: StudentMember = Fixtures.student("1234567")
		student1.freshStudentCourseDetails.head.scjCode = "1234567/1"
		student1.freshStudentCourseDetails.head.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(academicYear = command.academicYear, studentCourseDetails = student1.freshStudentCourseDetails.head)
		)
		val student2: StudentMember = Fixtures.student("9283845")
		student2.freshStudentCourseDetails.head.scjCode = "9283845/1"
		student2.freshStudentCourseDetails.head.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(academicYear = command.academicYear, studentCourseDetails = student2.freshStudentCourseDetails.head)
		)

		command.missedPoints = ListMap(
			student1 -> 3,
			student2 -> 2
		)

		command.attendanceMonitoringService.findNonReportedTerms(Seq(student1), command.academicYear) returns Seq("Autumn", "Spring", "Summer")
		command.attendanceMonitoringService.findNonReportedTerms(Seq(student2), command.academicYear) returns Seq("Autumn", "Spring")
		command.securityService.can(currentUser, Permissions.MonitoringPoints.Report, student1) returns true
		command.securityService.can(currentUser, Permissions.MonitoringPoints.Report, student2) returns true

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be {false}
	}}

	@Test def validateNoStudents() { new ValidationFixture {
		command.academicYear = new AcademicYear(2013)
		command.period = "Autumn"

		command.missedPoints = ListMap()

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("missedPoints")
		errors.getFieldError.getCodes should contain ("monitoringPointReport.noStudents")
	}}

	@Test def validateInvalidTerm() { new ValidationFixture {
		command.academicYear = new AcademicYear(2013)
		command.period = "Winter"

		val student1: StudentMember = Fixtures.student("1234567")
		val student2: StudentMember = Fixtures.student("9283845")

		command.missedPoints = ListMap(
			student1 -> 3,
			student2 -> 2
		)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("period")
		errors.getFieldError.getCodes should contain ("monitoringPointReport.invalidPeriod")
	}}

	@Test def validateNoAcademicYear() { new ValidationFixture {
		command.period = "Autumn"

		val student1: StudentMember = Fixtures.student("1234567")
		val student2: StudentMember = Fixtures.student("9283845")

		command.missedPoints = ListMap(
			student1 -> 3,
			student2 -> 2
		)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("academicYear")
		errors.getFieldError.getCodes should contain ("NotEmpty.academicYear")
	}}

	@Test def validateAlreadyReported() { new ValidationFixture {
		command.academicYear = new AcademicYear(2013)
		command.period = "Autumn"

		val student1: StudentMember = Fixtures.student("1234567")
		student1.freshStudentCourseDetails.head.scjCode = "1234567/1"
		student1.freshStudentCourseDetails.head.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(academicYear = command.academicYear, studentCourseDetails = student1.freshStudentCourseDetails.head)
		)
		val student2: StudentMember = Fixtures.student("9283845")
		student2.freshStudentCourseDetails.head.scjCode = "9283845/1"
		student2.freshStudentCourseDetails.head.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(academicYear = command.academicYear, studentCourseDetails = student2.freshStudentCourseDetails.head)
		)

		command.missedPoints = ListMap(
			student1 -> 3,
			student2 -> 2
		)

		command.attendanceMonitoringService.findNonReportedTerms(Seq(student1), command.academicYear) returns Seq("Spring", "Summer")
		command.attendanceMonitoringService.findNonReportedTerms(Seq(student2), command.academicYear) returns Seq("Autumn", "Spring")
		command.securityService.can(currentUser, Permissions.MonitoringPoints.Report, student1) returns true
		command.securityService.can(currentUser, Permissions.MonitoringPoints.Report, student2) returns true

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("missedPoints")
		errors.getFieldError.getCodes should contain ("monitoringPointReport.period.alreadyReported")
		errors.getFieldError.getArguments should be (Array("1234567"))
	}}

	@Test def validateNoPermission() { new ValidationFixture {
		command.academicYear = new AcademicYear(2013)
		command.period = "Autumn"

		val student1: StudentMember = Fixtures.student("1234567")
		student1.freshStudentCourseDetails.head.scjCode = "1234567/1"
		student1.freshStudentCourseDetails.head.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(academicYear = command.academicYear, studentCourseDetails = student1.freshStudentCourseDetails.head)
		)
		val student2: StudentMember = Fixtures.student("9283845")
		student2.freshStudentCourseDetails.head.scjCode = "9283845/1"
		student2.freshStudentCourseDetails.head.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(academicYear = command.academicYear, studentCourseDetails = student2.freshStudentCourseDetails.head)
		)

		command.missedPoints = ListMap(
			student1 -> 3,
			student2 -> 2
		)

		command.attendanceMonitoringService.findNonReportedTerms(Seq(student1), command.academicYear) returns Seq("Autumn", "Spring", "Summer")
		command.attendanceMonitoringService.findNonReportedTerms(Seq(student2), command.academicYear) returns Seq("Autumn", "Spring")
		command.securityService.can(currentUser, Permissions.MonitoringPoints.Report, student1) returns false
		command.securityService.can(currentUser, Permissions.MonitoringPoints.Report, student2) returns true

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("missedPoints")
		errors.getFieldError.getCodes should contain ("monitoringPointReport.student.noPermission")
		errors.getFieldError.getArguments should be (Array("1234567"))
	}}

	@Test def validateZeroMissedPoints() { new ValidationFixture {
		command.academicYear = new AcademicYear(2013)
		command.period = "Autumn"

		val student1: StudentMember = Fixtures.student("1234567")
		student1.freshStudentCourseDetails.head.scjCode = "1234567/1"
		student1.freshStudentCourseDetails.head.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(academicYear = command.academicYear, studentCourseDetails = student1.freshStudentCourseDetails.head)
		)
		val student2: StudentMember = Fixtures.student("9283845")
		student2.freshStudentCourseDetails.head.scjCode = "9283845/1"
		student2.freshStudentCourseDetails.head.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(academicYear = command.academicYear, studentCourseDetails = student2.freshStudentCourseDetails.head)
		)

		command.missedPoints = ListMap(
			student1 -> 0,
			student2 -> 2
		)

		command.attendanceMonitoringService.findNonReportedTerms(Seq(student1), command.academicYear) returns Seq("Autumn", "Spring", "Summer")
		command.attendanceMonitoringService.findNonReportedTerms(Seq(student2), command.academicYear) returns Seq("Autumn", "Spring")
		command.securityService.can(currentUser, Permissions.MonitoringPoints.Report, student1) returns true
		command.securityService.can(currentUser, Permissions.MonitoringPoints.Report, student2) returns true

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("missedPoints")
		errors.getFieldError.getCodes should contain ("monitoringPointReport.missedPointsZero")
	}}

	@Test def validateNoSCD() { new ValidationFixture {
		command.academicYear = new AcademicYear(2013)
		command.period = "Autumn"

		val student1: StudentMember = Fixtures.student("1234567")
		student1.mostSignificantCourse = null
		val student2: StudentMember = Fixtures.student("9283845")
		student2.freshStudentCourseDetails.head.scjCode = "9283845/1"
		student2.freshStudentCourseDetails.head.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(academicYear = command.academicYear, studentCourseDetails = student2.freshStudentCourseDetails.head)
		)

		command.missedPoints = ListMap(
			student1 -> 3,
			student2 -> 2
		)

		command.attendanceMonitoringService.findNonReportedTerms(Seq(student1), command.academicYear) returns Seq("Autumn", "Spring", "Summer")
		command.attendanceMonitoringService.findNonReportedTerms(Seq(student2), command.academicYear) returns Seq("Autumn", "Spring")
		command.securityService.can(currentUser, Permissions.MonitoringPoints.Report, student1) returns true
		command.securityService.can(currentUser, Permissions.MonitoringPoints.Report, student2) returns true

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("missedPoints")
		errors.getFieldError.getCodes should contain ("monitoringPointReport.student.noSCD")
	}}

	@Test def validateNoSCYD() { new ValidationFixture {
		command.academicYear = new AcademicYear(2013)
		command.period = "Autumn"

		val student1: StudentMember = Fixtures.student("1234567")
		student1.freshStudentCourseDetails.head.scjCode = "1234567/1"
		val student2: StudentMember = Fixtures.student("9283845")
		student2.freshStudentCourseDetails.head.scjCode = "9283845/1"
		student2.freshStudentCourseDetails.head.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(academicYear = command.academicYear, studentCourseDetails = student2.freshStudentCourseDetails.head)
		)

		command.missedPoints = ListMap(
			student1 -> 3,
			student2 -> 2
		)

		command.attendanceMonitoringService.findNonReportedTerms(Seq(student1), command.academicYear) returns Seq("Autumn", "Spring", "Summer")
		command.attendanceMonitoringService.findNonReportedTerms(Seq(student2), command.academicYear) returns Seq("Autumn", "Spring")
		command.securityService.can(currentUser, Permissions.MonitoringPoints.Report, student1) returns true
		command.securityService.can(currentUser, Permissions.MonitoringPoints.Report, student2) returns true

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("missedPoints")
		errors.getFieldError.getCodes should contain ("monitoringPointReport.student.noSCYD")
	}}

	@Test def description() {
		val command = new CreateMonitoringPointReportCommandDescription with CreateMonitoringPointReportCommandState {
			override lazy val eventName: String = "test"
			val department: Department = Fixtures.department("in")
			val currentUser: CurrentUser = mock[CurrentUser]
		}

		command.academicYear = new AcademicYear(2013)
		command.period = "Autumn"

		val student1 = Fixtures.student("1234567")
		val student2 = Fixtures.student("9283845")

		command.missedPoints = ListMap(
			student1 -> 3,
			student2 -> 2
		)

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"department" -> "in",
			"monitoringPeriod" -> "Autumn",
			"academicYear" -> command.academicYear,
			"missedPoints" -> Map("1234567" -> 3, "9283845" -> 2)
		))
	}

}
