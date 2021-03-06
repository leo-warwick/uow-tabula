package uk.ac.warwick.tabula.helpers

import java.time.temporal.{ChronoField => JChronoField}
import java.{time => javatime}

import org.joda.{time => jodatime}

import scala.language.implicitConversions

object JodaConverters extends JodaAsJavaExtensions with JavaAsJodaExtensions

trait JodaAsJavaExtensions {
  implicit class JodaLocalDateAsJava(i: jodatime.LocalDate) {
    def asJava: javatime.LocalDate =
      javatime.LocalDate.of(i.getYear, i.getMonthOfYear, i.getDayOfMonth)
  }

  implicit class JodaLocalDateTimeAsJava(i: jodatime.LocalDateTime) {
    def asJava: javatime.LocalDateTime =
      javatime.LocalDateTime.of(i.getYear, i.getMonthOfYear, i.getDayOfMonth, i.getHourOfDay, i.getMinuteOfHour, i.getSecondOfMinute, i.getMillisOfSecond)
  }

  implicit class JodaDateTimeAsJava(i: jodatime.DateTime) {
    def asJava: javatime.ZonedDateTime =
      javatime.ZonedDateTime.of(i.toLocalDateTime.asJava, i.getZone.toTimeZone.toZoneId)
  }
}

trait JavaAsJodaExtensions {
  implicit class JavaLocalDateAsJoda(i: javatime.LocalDate) {
    def asJoda: jodatime.LocalDate =
      new jodatime.LocalDate(i.getYear, i.getMonthValue, i.getDayOfMonth)
  }

  implicit class JavaLocalDateTimeAsJoda(i: javatime.LocalDateTime) {
    def asJoda: jodatime.LocalDateTime =
      new jodatime.LocalDateTime(i.getYear, i.getMonthValue, i.getDayOfMonth, i.getHour, i.getMinute, i.getSecond, i.get(JChronoField.MILLI_OF_SECOND))
  }

  implicit class JavaZonedDateTimeAsJoda(i: javatime.ZonedDateTime) {
    def asJoda: jodatime.DateTime =
      new jodatime.DateTime(i.getYear, i.getMonthValue, i.getDayOfMonth, i.getHour, i.getMinute, i.getSecond, i.get(JChronoField.MILLI_OF_SECOND), jodatime.DateTimeZone.forTimeZone(java.util.TimeZone.getTimeZone(i.getZone)))
  }
}
