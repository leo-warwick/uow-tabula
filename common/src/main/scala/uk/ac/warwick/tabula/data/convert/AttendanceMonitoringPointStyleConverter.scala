package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPointStyle
import uk.ac.warwick.tabula.helpers.StringUtils._

class AttendanceMonitoringPointStyleConverter extends TwoWayConverter[String, AttendanceMonitoringPointStyle] {

	override def convertRight(value: String): AttendanceMonitoringPointStyle =
		if (value.hasText) AttendanceMonitoringPointStyle.fromCode(value)
		else null

	override def convertLeft(pointStyle: AttendanceMonitoringPointStyle): String = Option(pointStyle).map { _.dbValue }.orNull

}