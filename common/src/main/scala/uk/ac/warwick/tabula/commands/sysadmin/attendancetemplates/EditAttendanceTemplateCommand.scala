package uk.ac.warwick.tabula.commands.sysadmin.attendancetemplates

import org.joda.time.{DateTime, LocalDate}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplate
import uk.ac.warwick.tabula.helpers.DateBuilder
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}

object EditAttendanceTemplateCommand {
	def apply(template: AttendanceMonitoringTemplate) =
		new EditAttendanceTemplateCommandInternal(template)
			with ComposableCommand[AttendanceMonitoringTemplate]
			with PopulatesEditAttendanceTemplateCommand
			with AutowiringAttendanceMonitoringServiceComponent
			with EditAttendanceTemplateValidation
			with EditAttendanceTemplateDescription
			with AttendanceTemplatePermissions
			with EditAttendanceTemplateCommandState
}


class EditAttendanceTemplateCommandInternal(val template: AttendanceMonitoringTemplate) extends CommandInternal[AttendanceMonitoringTemplate] {

	self: EditAttendanceTemplateCommandState with AttendanceMonitoringServiceComponent =>

	override def applyInternal(): AttendanceMonitoringTemplate = {
		template.templateName = name
		template.pointStyle = pointStyle
		template.updatedDate = DateTime.now
		attendanceMonitoringService.saveOrUpdate(template)
		template
	}

}

trait PopulatesEditAttendanceTemplateCommand extends PopulateOnForm {

	self: EditAttendanceTemplateCommandState =>

	override def populate(): Unit = {
		name = template.templateName
		pointStyle = template.pointStyle
	}
}

trait EditAttendanceTemplateValidation extends CreateAttendanceTemplateValidation {

	self: EditAttendanceTemplateCommandState with AttendanceMonitoringServiceComponent =>

	override def validate(errors: Errors) {
		super.validate(errors)
		if (template.points.size() > 0 && template.pointStyle != pointStyle) {
			errors.rejectValue("pointStyle", "attendanceMonitoringScheme.pointStyle.pointsExist")
		}
	}

}

trait EditAttendanceTemplateDescription extends Describable[AttendanceMonitoringTemplate] {

	self: EditAttendanceTemplateCommandState =>

	override lazy val eventName = "EditAttendanceTemplate"

	override def describe(d: Description) {
		d.attendanceMonitoringTemplate(template)
	}
}

trait EditAttendanceTemplateCommandState extends ManageAttendanceTemplateCommandState {
	def template: AttendanceMonitoringTemplate

	def dateString(date: LocalDate) = s"${date.getDayOfMonth}<sup>${DateBuilder.ordinal(date.getDayOfMonth)}</sup> ${date.toString("MMM")}"
}
