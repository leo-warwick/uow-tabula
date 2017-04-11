package uk.ac.warwick.tabula.commands.sysadmin.attendancetemplates

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplatePoint
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}

object DeleteAttendanceTemplatePointCommand {
	def apply(point: AttendanceMonitoringTemplatePoint) =
		new DeleteAttendanceTemplatePointCommandInternal(point)
			with ComposableCommand[Unit]
			with AutowiringAttendanceMonitoringServiceComponent
			with DeleteAttendanceTemplatePointDescription
			with AttendanceTemplatePermissions
			with DeleteAttendanceTemplatePointCommandState
}


class DeleteAttendanceTemplatePointCommandInternal(val point: AttendanceMonitoringTemplatePoint) extends CommandInternal[Unit] {

	self: AttendanceMonitoringServiceComponent =>

	override def applyInternal(): Unit = {
		attendanceMonitoringService.deleteTemplatePoint(point)
	}

}

trait DeleteAttendanceTemplatePointDescription extends Describable[Unit] {

	self: DeleteAttendanceTemplatePointCommandState =>

	override lazy val eventName = "DeleteAttendanceTemplatePoint"

	override def describe(d: Description) {
		d.attendanceMonitoringTemplatePoint(point)
	}
}

trait DeleteAttendanceTemplatePointCommandState {
	def point: AttendanceMonitoringTemplatePoint
}
