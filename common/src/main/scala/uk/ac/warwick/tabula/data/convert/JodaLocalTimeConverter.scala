package uk.ac.warwick.tabula.data.convert

import org.joda.time.LocalTime
import uk.ac.warwick.tabula.DateFormats.TimePickerFormatter
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.TwoWayConverter

class JodaLocalTimeConverter extends TwoWayConverter[String, LocalTime] {

	override def convertRight(text: String): LocalTime =
		if (text.hasText) try {	LocalTime.parse(text, TimePickerFormatter) } catch {	case e: IllegalArgumentException => null }
		else null

	override def convertLeft(time: LocalTime): String = Option(time) match {
		case Some (time) => TimePickerFormatter print time
		case None => null
	}
}