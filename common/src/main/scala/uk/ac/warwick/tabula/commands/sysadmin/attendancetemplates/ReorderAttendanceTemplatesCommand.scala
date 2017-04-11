package uk.ac.warwick.tabula.commands.sysadmin.attendancetemplates

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplate
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}

import scala.collection.JavaConverters._

object ReorderAttendanceTemplatesCommand {
	def apply() =
		new ReorderAttendanceTemplatesCommandInternal
			with ComposableCommand[Seq[AttendanceMonitoringTemplate]]
			with AutowiringAttendanceMonitoringServiceComponent
			with ReorderAttendanceTemplatesDescription
			with AttendanceTemplatePermissions
			with ReorderAttendanceTemplatesCommandState
}


class ReorderAttendanceTemplatesCommandInternal extends CommandInternal[Seq[AttendanceMonitoringTemplate]] {

	self: AttendanceMonitoringServiceComponent with ReorderAttendanceTemplatesCommandState =>

	override def applyInternal(): Seq[AttendanceMonitoringTemplate] = {
		templates.asScala.zipWithIndex.map{case (template, index) =>
			template.position = index
			attendanceMonitoringService.saveOrUpdate(template)
			template
		}.toSeq
	}

}

trait ReorderAttendanceTemplatesDescription extends Describable[Seq[AttendanceMonitoringTemplate]] {

	self: ReorderAttendanceTemplatesCommandState =>

	override lazy val eventName = "ReorderAttendanceTemplates"

	override def describe(d: Description) {}
}

trait ReorderAttendanceTemplatesCommandState {
	// Bind variables
	var templates: JList[AttendanceMonitoringTemplate] = LazyLists.create()
}
