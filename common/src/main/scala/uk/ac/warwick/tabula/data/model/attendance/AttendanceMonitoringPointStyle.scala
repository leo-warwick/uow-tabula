package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.data.model.AbstractStringUserType

sealed abstract class AttendanceMonitoringPointStyle(val dbValue: String, val description: String)

object AttendanceMonitoringPointStyle {
	case object Week extends AttendanceMonitoringPointStyle("week", "Term Week")
	case object Date extends AttendanceMonitoringPointStyle("date", "Calendar Date")

	def fromCode(code: String): AttendanceMonitoringPointStyle = code match {
		case Week.dbValue => Week
		case Date.dbValue => Date
		case null => null
		case _ => throw new IllegalArgumentException()
	}

	val values: Seq[AttendanceMonitoringPointStyle] = Seq(Week, Date)
}

class AttendanceMonitoringPointStyleUserType extends AbstractStringUserType[AttendanceMonitoringPointStyle] {

	override def convertToObject(string: String): AttendanceMonitoringPointStyle = AttendanceMonitoringPointStyle.fromCode(string)

	override def convertToValue(state: AttendanceMonitoringPointStyle): String = state.dbValue

}