package uk.ac.warwick.tabula.commands.sysadmin.attendancetemplates

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceMonitoringTemplate, AttendanceMonitoringTemplatePoint}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}

import scala.collection.JavaConverters._

object EditAttendanceTemplatePointCommand {
	def apply(point: AttendanceMonitoringTemplatePoint) =
		new EditAttendanceTemplatePointCommandInternal(point)
			with ComposableCommand[AttendanceMonitoringTemplatePoint]
			with PopulatesEditAttendanceTemplatePointCommand
			with AutowiringAttendanceMonitoringServiceComponent
			with EditAttendanceTemplatePointValidation
			with EditAttendanceTemplatePointDescription
			with AttendanceTemplatePermissions
			with EditAttendanceTemplatePointCommandState
}


class EditAttendanceTemplatePointCommandInternal(val point: AttendanceMonitoringTemplatePoint) extends CommandInternal[AttendanceMonitoringTemplatePoint] {

	self: EditAttendanceTemplatePointCommandState with AttendanceMonitoringServiceComponent =>

	override def applyInternal(): AttendanceMonitoringTemplatePoint = {
		point.name = name
		point.updatedDate = DateTime.now
		if (point.scheme.pointStyle == AttendanceMonitoringPointStyle.Week) {
			point.startWeek = startWeek
			point.endWeek = endWeek
		} else {
			point.startDate = startDate
			point.endDate = endDate
		}
		attendanceMonitoringService.saveOrUpdate(point)
		point
	}

}

trait PopulatesEditAttendanceTemplatePointCommand extends PopulateOnForm {

	self: EditAttendanceTemplatePointCommandState =>

	override def populate(): Unit = {
		name = point.name
		if (point.scheme.pointStyle == AttendanceMonitoringPointStyle.Week) {
			startWeek = point.startWeek
			endWeek = point.endWeek
		} else {
			startDate = point.startDate
			endDate = point.endDate
		}
	}
}

trait EditAttendanceTemplatePointValidation extends CreateAttendanceTemplatePointValidation {

	self: EditAttendanceTemplatePointCommandState =>

	override def validate(errors: Errors) {
		if (!name.hasText) {
			errors.rejectValue("name", "NotEmpty")
		} else if (name.length > 4000) {
			errors.rejectValue("name", "attendanceMonitoringPoint.name.toolong")
		}

		point.scheme.pointStyle match {
			case AttendanceMonitoringPointStyle.Week =>
				validateWeek(errors, startWeek, "startWeek")
				validateWeek(errors, endWeek, "endWeek")
				validateWeeks(errors, startWeek, endWeek)
				if (point.scheme.points.asScala.exists(p => p.id != point.id && p.name == name && p.startWeek == startWeek && p.endWeek == endWeek)) {
					errors.rejectValue("name", "attendanceMonitoringPoint.name.weeks.exists")
				}
			case AttendanceMonitoringPointStyle.Date =>
				validateDates(errors, startDate, endDate)
				if (point.scheme.points.asScala.exists(p => p.id != point.id && p.name == name && p.startDate == startDate && p.endDate == endDate)) {
					errors.rejectValue("name", "attendanceMonitoringPoint.name.dates.exists")
				}
		}
	}

}

trait EditAttendanceTemplatePointDescription extends Describable[AttendanceMonitoringTemplatePoint] {

	self: EditAttendanceTemplatePointCommandState =>

	override lazy val eventName = "EditAttendanceTemplatePoint"

	override def describe(d: Description) {
		d.attendanceMonitoringTemplatePoint(point)
	}
}

trait EditAttendanceTemplatePointCommandState extends CreateAttendanceTemplatePointCommandState {
	def point: AttendanceMonitoringTemplatePoint
	lazy val template: AttendanceMonitoringTemplate = point.scheme
}
