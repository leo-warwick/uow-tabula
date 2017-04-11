package uk.ac.warwick.tabula.commands.attendance.manage

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object AddPointsToSchemesCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new AddPointsToSchemesCommandInternal(department, academicYear)
			with ComposableCommand[AddPointsToSchemesCommandResult]
			with AutowiringAttendanceMonitoringServiceComponent
			with AddPointsToSchemesPermissions
			with AddPointsToSchemesCommandState
			with ReadOnly with Unaudited
}

case class AddPointsToSchemesCommandResult(
	weekSchemes: Map[AttendanceMonitoringScheme, Boolean],
	dateSchemes: Map[AttendanceMonitoringScheme, Boolean]
)

class AddPointsToSchemesCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[AddPointsToSchemesCommandResult] with AddPointsToSchemesCommandState {

	self: AttendanceMonitoringServiceComponent =>

	override def applyInternal(): AddPointsToSchemesCommandResult = {
		val groupedSchemeMap = attendanceMonitoringService.listSchemes(department, academicYear).map{
			scheme => scheme -> Option(schemes).getOrElse("").contains(scheme.id)
		}.groupBy(_._1.pointStyle).withDefaultValue(Seq())
		AddPointsToSchemesCommandResult(
			groupedSchemeMap(AttendanceMonitoringPointStyle.Week).toMap,
			groupedSchemeMap(AttendanceMonitoringPointStyle.Date).toMap
		)
	}

}

trait AddPointsToSchemesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AddPointsToSchemesCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, department)
	}

}

trait AddPointsToSchemesCommandState {
	def department: Department
	def academicYear: AcademicYear

	var schemes: String = ""
}
