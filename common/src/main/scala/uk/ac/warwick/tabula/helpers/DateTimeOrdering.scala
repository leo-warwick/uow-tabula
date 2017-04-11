package uk.ac.warwick.tabula.helpers
import org.joda.time.{DateTime, LocalDate, LocalDateTime, LocalTime}

import scala.language.implicitConversions

/**
 * import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
 * to tell your code how to sort DateTime objects
 */
object DateTimeOrdering {
	implicit def orderedDateTime(d: DateTime): math.Ordered[DateTime] = new math.Ordered[DateTime] {
		override def compare(d2: DateTime): Int = d.compareTo(d2)
	}
	implicit def orderedLocalDate(d:LocalDate):math.Ordered[LocalDate] = new math.Ordered[LocalDate]{
		override def compare(d2:LocalDate): Int = d.compareTo(d2)
	}
	implicit def orderedLocalTime(d:LocalTime):math.Ordered[LocalTime] = new math.Ordered[LocalTime]{
		override def compare(d2:LocalTime): Int = d.compareTo(d2)
	}
	implicit def orderedLocalDateTime(d:LocalDateTime):math.Ordered[LocalDateTime] = new math.Ordered[LocalDateTime]{
		override def compare(d2:LocalDateTime): Int = d.compareTo(d2)
	}

}