package uk.ac.warwick.tabula.data.convert
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.DateFormats.DatePickerFormatter
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.TwoWayConverter

class JodaLocalDateConverter extends TwoWayConverter[String, LocalDate] {

	override def convertRight(text: String): LocalDate =
		if (text.hasText && text.forall(_.isDigit))
			try { new DateTime(text.toLong * 1000).toLocalDate } catch { case e: NumberFormatException => null }
		else if (text.hasText)
			try {	LocalDate.parse(text, DatePickerFormatter) } catch { case e: IllegalArgumentException => null }
		else null

	override def convertLeft(date: LocalDate): String = Option(date) match {
		case Some (d) => DatePickerFormatter print d
		case None => null
	}
}