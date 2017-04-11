package uk.ac.warwick.tabula.data.convert

import org.joda.time.DateTime
import uk.ac.warwick.tabula.DateFormats.DateTimePickerFormatter
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.TwoWayConverter

class JodaDateTimeConverter extends TwoWayConverter[String, DateTime] {

	override def convertRight(text: String): DateTime =
		if (text.hasText) try {	DateTime.parse(text, DateTimePickerFormatter)	} catch {	case e: IllegalArgumentException => null }
		else null

	override def convertLeft(date: DateTime): String = Option(date) match {
		case Some (date) => DateTimePickerFormatter print date
		case None => null
	}
}