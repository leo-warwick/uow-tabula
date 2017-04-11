package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.timetables.TimetableEventType

class TimetableEventTypeConverter extends TwoWayConverter[String, TimetableEventType] {

	override def convertRight(code: String) = TimetableEventType(code)
	override def convertLeft(format: TimetableEventType): String = (Option(format) map { _.code }).orNull
}