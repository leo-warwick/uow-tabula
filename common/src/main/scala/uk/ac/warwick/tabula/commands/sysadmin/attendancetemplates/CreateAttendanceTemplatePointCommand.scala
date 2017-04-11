package uk.ac.warwick.tabula.commands.sysadmin.attendancetemplates

import org.joda.time.{DateTime, LocalDate}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceMonitoringTemplate, AttendanceMonitoringTemplatePoint}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}

import scala.collection.JavaConverters._

object CreateAttendanceTemplatePointCommand {
	def apply(template: AttendanceMonitoringTemplate) =
		new CreateAttendanceTemplatePointCommandInternal(template)
			with ComposableCommand[AttendanceMonitoringTemplatePoint]
			with AutowiringAttendanceMonitoringServiceComponent
			with CreateAttendanceTemplatePointValidation
			with CreateAttendanceTemplatePointDescription
			with AttendanceTemplatePermissions
			with CreateAttendanceTemplatePointCommandState
}


class CreateAttendanceTemplatePointCommandInternal(val template: AttendanceMonitoringTemplate) extends CommandInternal[AttendanceMonitoringTemplatePoint] {

	self: CreateAttendanceTemplatePointCommandState with AttendanceMonitoringServiceComponent =>

	override def applyInternal(): AttendanceMonitoringTemplatePoint = {
		val point = new AttendanceMonitoringTemplatePoint
		point.name = name
		point.scheme = template
		point.createdDate = DateTime.now
		point.updatedDate = DateTime.now
		if (template.pointStyle == AttendanceMonitoringPointStyle.Week) {
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

trait CreateAttendanceTemplatePointValidation extends SelfValidating {

	self: CreateAttendanceTemplatePointCommandState =>

	override def validate(errors: Errors) {
		if (!name.hasText) {
			errors.rejectValue("name", "NotEmpty")
		} else if (name.length > 4000) {
			errors.rejectValue("name", "attendanceMonitoringPoint.name.toolong")
		}

		template.pointStyle match {
			case AttendanceMonitoringPointStyle.Week =>
				validateWeek(errors, startWeek, "startWeek")
				validateWeek(errors, endWeek, "endWeek")
				validateWeeks(errors, startWeek, endWeek)
				if (template.points.asScala.exists(p => p.name == name && p.startWeek == startWeek && p.endWeek == endWeek)) {
					errors.rejectValue("name", "attendanceMonitoringPoint.name.weeks.exists")
				}
			case AttendanceMonitoringPointStyle.Date =>
				validateDates(errors, startDate, endDate)
				if (template.points.asScala.exists(p => p.name == name && p.startDate == startDate && p.endDate == endDate)) {
					errors.rejectValue("name", "attendanceMonitoringPoint.name.dates.exists")
				}
		}
	}

	protected def validateWeek(errors: Errors, week: Int, bindPoint: String) {
		week match {
			case y if y < 1  => errors.rejectValue(bindPoint, "attendanceMonitoringPoint.week.min")
			case y if y > 52 => errors.rejectValue(bindPoint, "attendanceMonitoringPoint.week.max")
			case _ =>
		}
	}

	protected def validateWeeks(errors: Errors, startWeek: Int, endWeek: Int) {
		if (startWeek > endWeek) {
			errors.rejectValue("startWeek", "attendanceMonitoringPoint.weeks")
		}
	}

	protected def validateDates(errors: Errors, startDate: LocalDate, endDate: LocalDate) {
		if (startDate.isAfter(endDate)) {
			errors.rejectValue("startDate", "attendanceMonitoringPoint.dates")
		}
	}

}

trait CreateAttendanceTemplatePointDescription extends Describable[AttendanceMonitoringTemplatePoint] {

	self: CreateAttendanceTemplatePointCommandState =>

	override lazy val eventName = "CreateAttendanceTemplatePoint"

	override def describe(d: Description) {
		d.attendanceMonitoringTemplate(template)
	}
}

trait CreateAttendanceTemplatePointCommandState {
	def template: AttendanceMonitoringTemplate

	// Bind variables
	var name: String = _
	var startWeek: Int = 0
	var endWeek: Int = 0
	var startDate: LocalDate = _
	var endDate: LocalDate = _
}
