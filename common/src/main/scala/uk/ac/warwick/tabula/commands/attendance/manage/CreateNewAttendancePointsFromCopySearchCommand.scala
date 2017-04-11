package uk.ac.warwick.tabula.commands.attendance.manage

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object CreateNewAttendancePointsFromCopySearchCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new CreateNewAttendancePointsFromCopySearchCommandInternal(department, academicYear)
			with ComposableCommand[Seq[AttendanceMonitoringScheme]]
			with AutowiringAttendanceMonitoringServiceComponent
			with CreateNewAttendancePointsFromCopySearchPermissions
			with CreateNewAttendancePointsFromCopySearchCommandState
			with ReadOnly with Unaudited
}


class CreateNewAttendancePointsFromCopySearchCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[Seq[AttendanceMonitoringScheme]] {

	self: AttendanceMonitoringServiceComponent =>

	override def applyInternal(): Seq[AttendanceMonitoringScheme] = {
		attendanceMonitoringService.listSchemes(department, academicYear).sortBy(_.displayName)
	}

}

trait CreateNewAttendancePointsFromCopySearchPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: CreateNewAttendancePointsFromCopySearchCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, department)
	}

}

trait CreateNewAttendancePointsFromCopySearchCommandState {
	def department: Department
	def academicYear: AcademicYear
}
