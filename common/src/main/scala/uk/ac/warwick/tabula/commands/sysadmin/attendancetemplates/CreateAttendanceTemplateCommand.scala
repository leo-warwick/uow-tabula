package uk.ac.warwick.tabula.commands.sysadmin.attendancetemplates

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceMonitoringTemplate}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}

object CreateAttendanceTemplateCommand {
	def apply() =
		new CreateAttendanceTemplateCommandInternal
			with ComposableCommand[AttendanceMonitoringTemplate]
			with AutowiringAttendanceMonitoringServiceComponent
			with CreateAttendanceTemplateValidation
			with CreateAttendanceTemplateDescription
			with AttendanceTemplatePermissions
			with ManageAttendanceTemplateCommandState
}


class CreateAttendanceTemplateCommandInternal extends CommandInternal[AttendanceMonitoringTemplate] {

	self: ManageAttendanceTemplateCommandState with AttendanceMonitoringServiceComponent =>

	override def applyInternal(): AttendanceMonitoringTemplate = {
		val template = new AttendanceMonitoringTemplate
		template.templateName = name
		template.pointStyle = pointStyle
		template.createdDate = DateTime.now
		template.updatedDate = DateTime.now
		template.position = attendanceMonitoringService.listAllTemplateSchemes.size
		attendanceMonitoringService.saveOrUpdate(template)
		template
	}

}

trait CreateAttendanceTemplateValidation extends SelfValidating {

	self: ManageAttendanceTemplateCommandState with AttendanceMonitoringServiceComponent =>

	override def validate(errors: Errors) {
		if (!name.hasText) {
			errors.rejectValue("name", "NotEmpty.name")
		} else {
			if (name.size > 255) {
				errors.rejectValue("name", "attendanceMonitoringTemplate.templateName.toolong")
			}
		}
		if (pointStyle == null) {
			errors.rejectValue("pointStyle", "attendanceMonitoringScheme.pointStyle.null")
		}
	}

}

trait CreateAttendanceTemplateDescription extends Describable[AttendanceMonitoringTemplate] {

	self: ManageAttendanceTemplateCommandState =>

	override lazy val eventName = "CreateAttendanceTemplate"

	override def describe(d: Description) {

	}
}

trait ManageAttendanceTemplateCommandState {
	// Bind variables
	var name: String = _
	var pointStyle: AttendanceMonitoringPointStyle = _
}
