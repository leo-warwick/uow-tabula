package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPointType
import uk.ac.warwick.tabula.helpers.StringUtils._

class AttendanceMonitoringPointTypeConverter extends TwoWayConverter[String, AttendanceMonitoringPointType] {

	override def convertRight(value: String): AttendanceMonitoringPointType =
		if (value.hasText) AttendanceMonitoringPointType.fromCode(value)
		else null

	override def convertLeft(pointType: AttendanceMonitoringPointType): String = Option(pointType).map { _.dbValue }.orNull

}