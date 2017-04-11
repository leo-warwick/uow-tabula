package uk.ac.warwick.tabula.commands.attendance.manage

import org.joda.time.{DateTime, LocalDate}
import org.mockito.Matchers
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringPointStyle, AttendanceMonitoringScheme, AttendanceMonitoringTemplate}
import uk.ac.warwick.tabula.data.model.{Department, ScheduledNotification, StudentMember}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}


class AddTemplatePointsToSchemesCommandTest extends TestBase with Mockito {

	val academicYear = new AcademicYear(2014)
	val department = new Department
	val student: StudentMember = Fixtures.student("1234")

	trait CommandTestSupport extends AddTemplatePointsToSchemesCommandState with TermServiceComponent
		with AttendanceMonitoringServiceComponent with ProfileServiceComponent {
		val termService: TermService = smartMock[TermService]
		val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
		val profileService: ProfileService = smartMock[ProfileService]

		templateScheme = new AttendanceMonitoringTemplate
		templateScheme.pointStyle = AttendanceMonitoringPointStyle.Date

		val baseDate: LocalDate = DateTime.now.toLocalDate

		val point1 = new AttendanceMonitoringPoint
		point1.name = "point1"
		point1.startDate = baseDate
		point1.endDate = baseDate.plusDays(1)

		val point2 = new AttendanceMonitoringPoint
		point2.name = "point2"
		point2.startDate = baseDate
		point2.endDate = baseDate.plusDays(1)

		val point3 = new AttendanceMonitoringPoint
		point3.name = "point3"
		point3.startDate = baseDate
		point3.endDate = baseDate.plusDays(1)

		val points = Seq(point1, point2, point3)

		val scheme = new AttendanceMonitoringScheme
		scheme.department = department
		scheme.academicYear = academicYear
		scheme.pointStyle = AttendanceMonitoringPointStyle.Date
		scheme.members.addUserId(student.universityId)

		val scheme1 = new AttendanceMonitoringScheme
		scheme1.department = department
		scheme1.pointStyle = AttendanceMonitoringPointStyle.Date

		schemes.add(scheme)
		schemes.add(scheme1)

		attendanceMonitoringService.generatePointsFromTemplateScheme(templateScheme, academicYear) returns points
		profileService.getAllMembersWithUniversityIds(Seq(student.universityId)) returns Seq(student)
		attendanceMonitoringService.listAllSchemes(department) returns Seq(scheme)

	}

	trait Fixture {

		val command = new AddTemplatePointsToSchemesCommandInternal(department, academicYear) with CommandTestSupport with AddTemplatePointsToSchemesValidation
		val errors = new BindException(command, "command")
		command.thisScheduledNotificationService = smartMock[ScheduledNotificationService]
	}

	@Test
	def addPointsFromTemplate() {
		new Fixture {
			val newPoints: Seq[AttendanceMonitoringPoint] = command.applyInternal()
			newPoints.size should be (6)
			verify(command.thisScheduledNotificationService, times(1)).removeInvalidNotifications(department)
			verify(command.thisScheduledNotificationService, atLeast(1)).push(Matchers.any[ScheduledNotification[Department]])
			verify(command.attendanceMonitoringService, times(1)).setCheckpointTotalsForUpdate(Seq(student), department, academicYear)
		}
	}

	@Test
	def validateAll() {
		new Fixture {
			command.validate(errors)
			errors.getErrorCount should be (0)
		}
	}

	@Test
	def noTemplateScheme() {
		new Fixture {
			command.templateScheme = null
			command.validate(errors)
			errors.getErrorCount should be (1)
		}
	}

	@Test
	def mixedSchemeType() {
		new Fixture {
			command.scheme1.pointStyle = AttendanceMonitoringPointStyle.Week
			command.validate(errors)
			errors.getErrorCount should be (1)
		}
	}

	@Test
	def duplicatePoints() {
		new Fixture {
			command.scheme1.points.add(command.point1)
			command.validate(errors)
			errors.getErrorCount should be (1)
		}
	}

}
