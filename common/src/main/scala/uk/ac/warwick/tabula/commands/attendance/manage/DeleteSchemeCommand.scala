package uk.ac.warwick.tabula.commands.attendance.manage

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object DeleteSchemeCommand {
	def apply(scheme: AttendanceMonitoringScheme) =
		new DeleteSchemeCommandInternal(scheme)
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[AttendanceMonitoringScheme]
			with DeleteSchemeCommandState
			with DeleteSchemeDescription
			with DeleteSchemePermissions
			with DeleteSchemeValidation

}

class DeleteSchemeCommandInternal(val scheme: AttendanceMonitoringScheme)
	extends CommandInternal[AttendanceMonitoringScheme] with GeneratesAttendanceMonitoringSchemeNotifications with RequiresCheckpointTotalUpdate {

	self: AttendanceMonitoringServiceComponent with ProfileServiceComponent =>

	override def applyInternal(): AttendanceMonitoringScheme = {
		val previousUniversityIds = scheme.members.members

		attendanceMonitoringService.deleteScheme(scheme)

		generateNotifications(Seq(scheme))
		updateCheckpointTotals(previousUniversityIds, scheme.department, scheme.academicYear)

		scheme
	}
}

trait DeleteSchemeDescription extends Describable[AttendanceMonitoringScheme] {

	self: DeleteSchemeCommandState =>

	override lazy val eventName = "DeleteScheme"

	override def describe(d: Description) {
		d.attendanceMonitoringScheme(scheme)
	}
}

trait DeleteSchemeCommandState  {
	def scheme: AttendanceMonitoringScheme
}


trait DeleteSchemePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: DeleteSchemeCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, scheme)
	}

}

trait DeleteSchemeValidation extends SelfValidating {

	self: DeleteSchemeCommandState with AttendanceMonitoringServiceComponent =>

	override def validate(errors: Errors) {
		if (scheme.points.asScala.exists { point => attendanceMonitoringService.countCheckpointsForPoint(point) > 0 }) {
			errors.reject("attendanceMonitoringScheme.hasCheckpoints.remove")
		}
	}

}