package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState

class AttendanceStateConverter extends TwoWayConverter[String, AttendanceState] {

	override def convertRight(value: String): AttendanceState =
		if (value.hasText) AttendanceState.fromCode(value)
		else null

	override def convertLeft(state: AttendanceState): String = Option(state).map { _.dbValue }.orNull

}