package uk.ac.warwick.tabula.commands.attendance.manage

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object CreateMonitoringSchemeCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new CreateMonitoringSchemeCommandInternal(department, academicYear)
			with AutowiringAttendanceMonitoringServiceComponent
			with ComposableCommand[AttendanceMonitoringScheme]
			with CreateMonitoringSchemeValidation
			with CreateMonitoringSchemeDescription
			with CreateMonitoringSchemePermissions
			with CreateMonitoringSchemeCommandState
}


class CreateMonitoringSchemeCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[AttendanceMonitoringScheme] {

	self: CreateMonitoringSchemeCommandState with AttendanceMonitoringServiceComponent =>

	override def applyInternal(): AttendanceMonitoringScheme = {
		val scheme = new AttendanceMonitoringScheme
		scheme.department = department
		scheme.academicYear = academicYear
		scheme.name = name
		scheme.pointStyle = pointStyle
		scheme.createdDate = DateTime.now
		scheme.updatedDate = DateTime.now
		attendanceMonitoringService.saveOrUpdate(scheme)
		scheme
	}

}

trait CreateMonitoringSchemeValidation extends SelfValidating {

	self: CreateMonitoringSchemeCommandState =>

	override def validate(errors: Errors) {
		if (pointStyle == null) {
			errors.rejectValue("pointStyle", "attendanceMonitoringScheme.pointStyle.null")
		}
	}

}

trait CreateMonitoringSchemePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: CreateMonitoringSchemeCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, mandatory(department))
	}

}

trait CreateMonitoringSchemeDescription extends Describable[AttendanceMonitoringScheme] {

	self: CreateMonitoringSchemeCommandState =>

	override lazy val eventName = "CreateMonitoringScheme"

	override def describe(d: Description): Unit = {
		d.department(department)
	}

	override def describeResult(d: Description, result: AttendanceMonitoringScheme): Unit = {
		d.attendanceMonitoringScheme(result)
	}
}

trait CreateMonitoringSchemeCommandState {
	def department: Department
	def academicYear: AcademicYear

	// Bind variables
	var name: String = _
	var pointStyle: AttendanceMonitoringPointStyle = _
}
