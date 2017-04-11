package uk.ac.warwick.tabula.commands.attendance.manage

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.services.{ProfileService, ProfileServiceComponent, ScheduledNotificationService}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class DeleteAttendancePointCommandTest extends TestBase with Mockito {

	trait Fixture {
		val thisDepartment: Department = Fixtures.department("its")
		val thisAcademicYear = AcademicYear(2014)
		val student: StudentMember = Fixtures.student("1234")
		val scheme = new AttendanceMonitoringScheme
		scheme.department = thisDepartment
		scheme.academicYear = thisAcademicYear
		scheme.members.addUserId(student.universityId)

		val point = new AttendanceMonitoringPoint
		point.scheme = scheme
		scheme.points.add(point)

		val validator = new DeleteAttendancePointValidation with AttendanceMonitoringServiceComponent with DeleteAttendancePointCommandState {
			val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
			def department = null
			def templatePoint = null
			override def pointsToDelete = Seq(point)
		}
		val errors = new BindException(validator, "errors")

		val cmd = new DeleteAttendancePointCommandInternal(thisDepartment, null) with DeleteAttendancePointCommandState
			with AttendanceMonitoringServiceComponent with ProfileServiceComponent {
			override val pointsToDelete = Seq(point)
			override val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
			override val profileService: ProfileService = smartMock[ProfileService]
			thisScheduledNotificationService = smartMock[ScheduledNotificationService]
		}
	}

	@Test
	def validateNoCheckpoints() { new Fixture {
		validator.attendanceMonitoringService.countCheckpointsForPoint(point) returns 0
		validator.validate(errors)
		errors.getAllErrors.size should be (0)
	}}

	@Test
	def validateHasCheckpoints() { new Fixture {
		validator.attendanceMonitoringService.countCheckpointsForPoint(point) returns 2
		validator.validate(errors)
		errors.getAllErrors.size should be (1)
	}}

	@Test
	def applyInternal(): Unit = new Fixture {
		cmd.attendanceMonitoringService.listAllSchemes(thisDepartment) returns Seq()
		cmd.profileService.getAllMembersWithUniversityIds(Seq(student.universityId)) returns Seq(student)
		cmd.applyInternal()
		verify(cmd.attendanceMonitoringService, times(1)).setCheckpointTotalsForUpdate(Seq(student), thisDepartment, thisAcademicYear)
	}

}