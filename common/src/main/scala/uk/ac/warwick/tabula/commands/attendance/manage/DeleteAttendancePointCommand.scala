package uk.ac.warwick.tabula.commands.attendance.manage

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringPointStyle, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object DeleteAttendancePointCommand {
	def apply(department: Department, templatePoint: AttendanceMonitoringPoint) =
		new DeleteAttendancePointCommandInternal(department, templatePoint)
			with ComposableCommand[Seq[AttendanceMonitoringPoint]]
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringProfileServiceComponent
			with DeleteAttendancePointValidation
			with DeleteAttendancePointDescription
			with DeleteAttendancePointPermissions
			with DeleteAttendancePointCommandState
			with SetsFindPointsResultOnCommandState
}


class DeleteAttendancePointCommandInternal(val department: Department, val templatePoint: AttendanceMonitoringPoint)
	extends CommandInternal[Seq[AttendanceMonitoringPoint]] with GeneratesAttendanceMonitoringSchemeNotifications with RequiresCheckpointTotalUpdate {

	self: DeleteAttendancePointCommandState with AttendanceMonitoringServiceComponent
		with ProfileServiceComponent =>

	override def applyInternal(): Seq[AttendanceMonitoringPoint] = {
		pointsToDelete.foreach(p => {
			attendanceMonitoringService.deletePoint(p)
			p.scheme.points.remove(p)
		})

		generateNotifications(schemesToEdit)
		updateCheckpointTotals(schemesToEdit)

		pointsToDelete
	}

}


trait DeleteAttendancePointValidation extends SelfValidating {

	self: DeleteAttendancePointCommandState with AttendanceMonitoringServiceComponent =>

	override def validate(errors: Errors) {

		val pointsWithCheckpoints = pointsToDelete.filter {
			point => attendanceMonitoringService.countCheckpointsForPoint(point) > 0
		}

		if (pointsWithCheckpoints.nonEmpty) {
			errors.reject("attendanceMonitoringPoints.hasCheckpoints.remove")
		}
	}

}

trait DeleteAttendancePointPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: DeleteAttendancePointCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, department)
	}

}

trait DeleteAttendancePointDescription extends Describable[Seq[AttendanceMonitoringPoint]] {

	self: DeleteAttendancePointCommandState =>

	override lazy val eventName = "DeleteAttendancePoint"

	override def describe(d: Description) {
		d.attendanceMonitoringSchemes(schemesToEdit)
	}
	override def describeResult(d: Description, points: Seq[AttendanceMonitoringPoint]) {
		d.attendanceMonitoringPoints(points, verbose = true)
	}
}

trait DeleteAttendancePointCommandState extends FindPointsResultCommandState {

	def department: Department
	def templatePoint: AttendanceMonitoringPoint
	lazy val pointStyle: AttendanceMonitoringPointStyle = templatePoint.scheme.pointStyle

	def pointsToDelete: Seq[AttendanceMonitoringPoint] = (pointStyle match {
		case AttendanceMonitoringPointStyle.Week => findPointsResult.termGroupedPoints
		case AttendanceMonitoringPointStyle.Date => findPointsResult.monthGroupedPoints
	}).flatMap(_._2)
		.find(p => p.templatePoint.id == templatePoint.id)
		.getOrElse(throw new IllegalArgumentException)
		.points

	def schemesToEdit: Seq[AttendanceMonitoringScheme] = pointsToDelete.map(_.scheme)
}
