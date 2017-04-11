package uk.ac.warwick.tabula.commands.attendance.manage

import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}

class AddStudentsToSchemeCommandTest extends TestBase with Mockito {

	trait Fixture {
		val thisDepartment: Department = Fixtures.department("its")
		val thisAcademicYear = AcademicYear(2014)
		val thisScheme = new AttendanceMonitoringScheme
		thisScheme.department = thisDepartment
		thisScheme.academicYear = thisAcademicYear
		val staticStudent: StudentMember = Fixtures.student("1234")
		val includeStudent: StudentMember = Fixtures.student("2345")
		val excludeStudent: StudentMember = Fixtures.student("3456")
		val updatedStaticStudent: StudentMember = Fixtures.student("1234")
		val updatedIncludeStudent: StudentMember = Fixtures.student("9999")
		val updatedExcludeStudent: StudentMember = Fixtures.student("8888")
		val allStudents = Seq(staticStudent, includeStudent, excludeStudent, updatedStaticStudent, updatedIncludeStudent, updatedExcludeStudent)
		val thisProfileService: ProfileService = smartMock[ProfileService]
		val thisSecurityService: SecurityService = smartMock[SecurityService]
		val thisAttendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
		thisProfileService.getAllMembersWithUniversityIds(any[Seq[String]]) answers { arg => arg match {
			case universityIds: Seq[String] @unchecked => universityIds.flatMap(u => allStudents.find(_.universityId == u))
			case _ => Seq()
		}}
	}

	@Test
	def validateOk() = withUser("cusfal") { new Fixture {
		val validator = new AddStudentsToSchemeValidation with AddStudentsToSchemeCommandState
			with AttendanceMonitoringServiceComponent with ProfileServiceComponent with SecurityServiceComponent {

			val scheme: AttendanceMonitoringScheme = thisScheme
			val user: CurrentUser = currentUser
			val profileService: ProfileService = thisProfileService
			val securityService: SecurityService = thisSecurityService
			val attendanceMonitoringService: AttendanceMonitoringService = thisAttendanceMonitoringService
		}

		validator.staticStudentIds.add(staticStudent.universityId)
		validator.includedStudentIds.add(includeStudent.universityId)
		validator.excludedStudentIds.add(excludeStudent.universityId)

		thisProfileService.getAllMembersWithUniversityIds(
			Seq(staticStudent.universityId, includeStudent.universityId)
		) returns Seq(staticStudent, includeStudent)

		thisSecurityService.can(currentUser, Permissions.MonitoringPoints.Manage, staticStudent) returns true
		thisSecurityService.can(currentUser, Permissions.MonitoringPoints.Manage, includeStudent) returns true

		var errors = new BindException(validator, "command")
		validator.validate(errors)
		errors.getAllErrors.size should be (0)

	}}

	@Test
	def validateNotOk() { withUser("cusfal") { new Fixture {
		val validator = new AddStudentsToSchemeValidation with AddStudentsToSchemeCommandState
			with AttendanceMonitoringServiceComponent with ProfileServiceComponent with SecurityServiceComponent {

			val scheme: AttendanceMonitoringScheme = thisScheme
			val user: CurrentUser = currentUser
			val profileService: ProfileService = thisProfileService
			val securityService: SecurityService = thisSecurityService
			val attendanceMonitoringService: AttendanceMonitoringService = thisAttendanceMonitoringService
		}

		validator.staticStudentIds.add(staticStudent.universityId)
		validator.includedStudentIds.add(includeStudent.universityId)
		validator.excludedStudentIds.add(excludeStudent.universityId)

		thisSecurityService.can(currentUser, Permissions.MonitoringPoints.Manage, staticStudent) returns true
		thisSecurityService.can(currentUser, Permissions.MonitoringPoints.Manage, includeStudent) returns false

		var errors = new BindException(validator, "command")
		validator.validate(errors)
		errors.getAllErrors.size should be (1)

	}}}

	@Test
	def applyInternal(): Unit = withUser("cusfal") { new Fixture {
		val cmd = new AddStudentsToSchemeCommandInternal(thisScheme, currentUser)
			with AddStudentsToSchemeCommandState
			with AttendanceMonitoringServiceComponent
			with ProfileServiceComponent {

			val attendanceMonitoringService: AttendanceMonitoringService = thisAttendanceMonitoringService
			val profileService: ProfileService = thisProfileService
		}

		thisScheme.members.staticUserIds = Seq(staticStudent.universityId)
		thisScheme.members.includedUserIds = Seq(includeStudent.universityId)
		thisScheme.members.excludedUserIds = Seq(excludeStudent.universityId)

		cmd.staticStudentIds.add(updatedStaticStudent.universityId)
		cmd.includedStudentIds.add(updatedIncludeStudent.universityId)
		cmd.excludedStudentIds.add(updatedExcludeStudent.universityId)

		cmd.applyInternal()

		thisScheme.members.size should be (2)
		verify(thisAttendanceMonitoringService, times(1)).setCheckpointTotalsForUpdate(
			Seq(updatedStaticStudent, includeStudent, updatedIncludeStudent),
			thisDepartment,
			thisAcademicYear
		)
	}}

}
