package uk.ac.warwick.tabula.commands.attendance.report

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, SecurityServiceComponent, TermService}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

object CreateMonitoringPointReportCommand {
	def apply(department: Department, currentUser: CurrentUser) =
		new CreateMonitoringPointReportCommandInternal(department, currentUser)
			with ComposableCommand[Seq[MonitoringPointReport]]
			with CreateMonitoringPointReportCommandValidation
			with CreateMonitoringPointReportCommandDescription
			with CreateMonitoringPointReportCommandPermissions
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringSecurityServiceComponent
}

trait CreateMonitoringPointReportRequestState {
	var period: String = _
	var academicYear: AcademicYear = _
	var missedPoints: Map[StudentMember, Int] = _
}

trait CreateMonitoringPointReportCommandState extends CreateMonitoringPointReportRequestState {
	def department: Department
	def currentUser: CurrentUser
}

class CreateMonitoringPointReportCommandInternal(val department: Department, val currentUser: CurrentUser) extends CommandInternal[Seq[MonitoringPointReport]] with CreateMonitoringPointReportCommandState {
	self: AttendanceMonitoringServiceComponent =>

	def applyInternal(): Seq[MonitoringPointReport] = {
		missedPoints.map { case (student, missedCount) =>
			val scd = student.mostSignificantCourseDetails.orNull
			val report = new MonitoringPointReport
			report.academicYear = academicYear
			report.createdDate = DateTime.now
			report.missed = missedCount
			report.monitoringPeriod = period
			report.reporter = currentUser.departmentCode.toUpperCase + currentUser.apparentUser.getWarwickId
			report.student = student
			report.studentCourseDetails = scd
			report.studentCourseYearDetails = scd.freshStudentCourseYearDetails.find(_.academicYear == academicYear).orNull
			attendanceMonitoringService.saveOrUpdate(report)
			report
		}.toSeq
	}
}

trait CreateMonitoringPointReportCommandValidation extends SelfValidating {
	self: AttendanceMonitoringServiceComponent with SecurityServiceComponent with CreateMonitoringPointReportCommandState =>

	override def validate(errors: Errors): Unit = {
		val allStudents = missedPoints.keySet.toSeq

		if (allStudents.isEmpty) {
			errors.rejectValue("missedPoints", "monitoringPointReport.noStudents")
		}

		if (!TermService.orderedTermNames.contains(period)) {
			errors.rejectValue("period", "monitoringPointReport.invalidPeriod")
		}

		if (academicYear == null) {
			errors.rejectValue("academicYear", "NotEmpty")
		}

		if (!errors.hasErrors) {
			allStudents.foreach { student =>
				if (student.mostSignificantCourseDetails.isEmpty) {
					errors.rejectValue("missedPoints", "monitoringPointReport.student.noSCD", Array(student.universityId), "")
				} else if (!student.mostSignificantCourseDetails.get.freshStudentCourseYearDetails.exists(_.academicYear == academicYear)) {
					errors.rejectValue("missedPoints", "monitoringPointReport.student.noSCYD", Array(student.universityId, academicYear.toString), "")
				}

				val nonReported = attendanceMonitoringService.findNonReportedTerms(Seq(student), academicYear)

				if (!nonReported.contains(period)) {
					errors.rejectValue("missedPoints", "monitoringPointReport.period.alreadyReported", Array(student.universityId), "")
				}

				if (!securityService.can(currentUser, Permissions.MonitoringPoints.Report, student)) {
					errors.rejectValue("missedPoints", "monitoringPointReport.student.noPermission", Array(student.universityId), "")
				}
			}
		}

		if (missedPoints.exists { case (_, points) => points <= 0 }) {
			errors.rejectValue("missedPoints", "monitoringPointReport.missedPointsZero")
		}
	}
}

trait CreateMonitoringPointReportCommandDescription extends Describable[Seq[MonitoringPointReport]] {
	self: CreateMonitoringPointReportCommandState =>

	override lazy val eventName = "MonitoringPointReport"

	def describe(d: Description) {
		val students = missedPoints.map { case (student, count) => student.universityId -> count }

		d.department(department)
		d.property("monitoringPeriod", period)
		d.property("academicYear", academicYear)
		d.property("missedPoints", students)
	}
}

trait CreateMonitoringPointReportCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: CreateMonitoringPointReportCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Report, mandatory(department))
	}
}