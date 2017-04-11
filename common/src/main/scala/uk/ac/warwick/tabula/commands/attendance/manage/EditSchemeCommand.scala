package uk.ac.warwick.tabula.commands.attendance.manage

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object EditSchemeCommand {
	def apply(scheme: AttendanceMonitoringScheme, user: CurrentUser) =
		new EditSchemeCommandInternal(scheme, user)
			with AutowiringAttendanceMonitoringServiceComponent
			with ComposableCommand[AttendanceMonitoringScheme]
			with PopulateEditSchemeCommandInternal
			with EditSchemeValidation
			with EditSchemeDescription
			with EditSchemePermissions
			with EditSchemeCommandState
}


class EditSchemeCommandInternal(val scheme: AttendanceMonitoringScheme, val user: CurrentUser)
	extends CommandInternal[AttendanceMonitoringScheme] {

	self: EditSchemeCommandState with AttendanceMonitoringServiceComponent =>

	override def applyInternal(): AttendanceMonitoringScheme = {
		scheme.name = name
		scheme.pointStyle = pointStyle
		scheme.updatedDate = DateTime.now
		attendanceMonitoringService.saveOrUpdate(scheme)
		scheme
	}

}

trait PopulateEditSchemeCommandInternal extends PopulateOnForm {

	self: EditSchemeCommandState =>

	override def populate(): Unit = {
		name = scheme.name
		pointStyle = scheme.pointStyle
	}
}

trait EditSchemeValidation extends SelfValidating {

	self: EditSchemeCommandState =>

	override def validate(errors: Errors) {

		if (!scheme.points.isEmpty && pointStyle != scheme.pointStyle) {
			errors.rejectValue("pointStyle", "attendanceMonitoringScheme.pointStyle.pointsExist")
		}
	}

}

trait EditSchemePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: EditSchemeCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, scheme)
	}

}

trait EditSchemeDescription extends Describable[AttendanceMonitoringScheme] {

	self: EditSchemeCommandState =>

	override lazy val eventName = "EditScheme"

	override def describe(d: Description) {
		d.attendanceMonitoringScheme(scheme)
	}
}

trait EditSchemeCommandState {

	def scheme: AttendanceMonitoringScheme
	def user: CurrentUser

	// Bind variables
	var name: String = _
	var pointStyle: AttendanceMonitoringPointStyle = _
}
