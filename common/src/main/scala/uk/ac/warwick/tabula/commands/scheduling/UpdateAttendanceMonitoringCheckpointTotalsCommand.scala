package uk.ac.warwick.tabula.commands.scheduling

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringCheckpointTotal
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.data.Transactions._

object UpdateAttendanceMonitoringCheckpointTotalsCommand {
	def apply() =
		new UpdateAttendanceMonitoringCheckpointTotalsCommandInternal
			with ComposableCommand[Seq[AttendanceMonitoringCheckpointTotal]]
			with AutowiringAttendanceMonitoringServiceComponent
			with UpdateAttendanceMonitoringCheckpointTotalsDescription
			with UpdateAttendanceMonitoringCheckpointTotalsPermissions
			with UpdateAttendanceMonitoringCheckpointTotalsState
			with UpdateAttendanceMonitoringCheckpointTotalsCommandValidation
}


class UpdateAttendanceMonitoringCheckpointTotalsCommandInternal extends CommandInternal[Seq[AttendanceMonitoringCheckpointTotal]] {

	self: AttendanceMonitoringServiceComponent with UpdateAttendanceMonitoringCheckpointTotalsState =>

	override def applyInternal(): Seq[AttendanceMonitoringCheckpointTotal] = {
			totalsToUpdate.map(total =>
				attendanceMonitoringService.updateCheckpointTotal(total.student, total.department, total.academicYear)
			)
	}

}

trait UpdateAttendanceMonitoringCheckpointTotalsCommandValidation extends SelfValidating {

	self: UpdateAttendanceMonitoringCheckpointTotalsState =>

	def validate(errors: Errors) {
		if (totalsToUpdate.isEmpty) {
			errors.reject("", "No totals to update")
		}
	}
}

trait UpdateAttendanceMonitoringCheckpointTotalsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.UpdateMembership)
	}

}

trait UpdateAttendanceMonitoringCheckpointTotalsDescription extends Describable[Seq[AttendanceMonitoringCheckpointTotal]] {

	override lazy val eventName = "UpdateAttendanceMonitoringCheckpointTotals"

	override def describe(d: Description) {}

	override def describeResult(d: Description, result: Seq[AttendanceMonitoringCheckpointTotal]): Unit = {
		d.property("totals", result.map(total => Map(
			"student" -> total.student.universityId,
			"department" -> total.department.code,
			"academicYear" -> total.academicYear.toString,
			"unrecorded" -> total.unrecorded,
			"unauthorised" -> total.unauthorised,
			"authorised" -> total.authorised,
			"attended" -> total.attended
		)))
	}
}

trait UpdateAttendanceMonitoringCheckpointTotalsState {
	self: AttendanceMonitoringServiceComponent =>
	lazy val totalsToUpdate: Seq[AttendanceMonitoringCheckpointTotal] = transactional(readOnly = true) { attendanceMonitoringService.listCheckpointTotalsForUpdate }
}
