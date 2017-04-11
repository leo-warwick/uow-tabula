package uk.ac.warwick.tabula.helpers

import org.joda.time._
import org.joda.time.format.{DateTimeFormatter, DateTimeFormat}
import collection.JavaConversions._
import freemarker.template.TemplateMethodModelEx
import freemarker.template.utility.DeepUnwrap
import freemarker.template.TemplateModel
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.ConfigurableIntervalFormatter._
import java.util.Date

/**
Formats an Interval (which is a start and an end date together)
	in a compact format that avoids repetition. If start and end are
	in the same year, for example, the year is only printed at the end.
	*/
object IntervalFormatter {

	def format(start: DateTime, end: DateTime, includeTime: Boolean = true, includeDays: Boolean = true): String = {
		val timeFormatter = if (includeTime) Hour24IncludeMins else OmitTimes
		val dateFormatter = if (includeDays) IncludeDays else OmitDays
		val formatter = new ConfigurableIntervalFormatter(timeFormatter, dateFormatter)
		formatter.format(new Interval(start, end))

	}

	/** Useful sometimes if you have an "endless" interval like an open-ended Assignment. */
	def format(start: DateTime): String = doFormat(start, includeYear = true)

	def format(start: DateTime, includeTime: Boolean): String = doFormat(start, includeYear = true, includeTime = includeTime)

	/** @see #format(DateTime, DateTime, Boolean) */
	def format(interval: Interval): String = format(interval.getStart, interval.getEnd)

	private def doFormat(date: DateTime, includeYear: Boolean, includeTime: Boolean = true, includeDays: Boolean = true): String = {
		val timeFormatter = if (includeTime) Hour24IncludeMins else OmitTimes
		val dateFormatter = if (includeDays) IncludeDays else OmitDays
		val formatter = new ConfigurableIntervalFormatter(timeFormatter, dateFormatter)
		formatter.format(date)
	}

}

/**
 * Companion class for Freemarker.
 */
class IntervalFormatter extends TemplateMethodModelEx {

	import IntervalFormatter.format

	/** Two-argument method taking a start and end date. */
	override def exec(list: JList[_]): String = {
		val args = list.toSeq.map {
			model => {
				DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel])
			}
		}
		args match {
			case Seq(start: DateTime) => format(start)
			case Seq(start: DateTime, end: DateTime) => format(start, end)
			case Seq(start: LocalDate, end: LocalDate) => format(start.toDateTimeAtStartOfDay, end.toDateTimeAtStartOfDay, includeTime = false)
			case Seq(start: Date, end: Date) => format(new LocalDate(start).toDateTimeAtStartOfDay, new LocalDate(end).toDateTimeAtStartOfDay, includeTime = false)
			case _ => throw new IllegalArgumentException("Bad args")
		}
	}
}

/**
 * Does the actual work of formatting; can be called directly or through the convenience methods on IntervalFormatter.
 */
class ConfigurableIntervalFormatter(val timeFormat: TimeFormats, val dateFormat: DateFormats) {


	def format(start: DateTime, end: DateTime): String = {
		if (start.toLocalDate == end.toLocalDate) {
			// don't print the date twice if they're the same
			val timeBit = timeFormat.formatTimes(new Interval(start, end)) match {
				case None => ""
				case Some((startTime, endTime)) => s"$startTime - $endTime, "
			}
			val date = dateFormat.formatDate(start)
			s"$timeBit$date"

		} else {
			val (startDate, endDate) = dateFormat.formatDates(new Interval(start, end))
			val (startString, endString) = timeFormat.formatTimes(new Interval(start, end)) match {
				case None => (startDate, endDate)
				case Some((startTime, endTime)) => (s"$startTime, $startDate", s"$endTime, $endDate")
			}
			s"$startString - $endString"
		}
	}

	def format(interval: Interval): String = format(interval.getStart, interval.getEnd)

	/** Useful sometimes if you have an "endless" interval like an open-ended Assignment. */
	def format(start: DateTime): String = {
		val time = timeFormat.formatTime(start)
		val date = dateFormat.formatDate(start)
		time match {
			case None => date
			case Some(t) => s"$t, $date"

		}
	}
}

/**
 * holds the various different rule-sets for formatting times and dates
 */
object ConfigurableIntervalFormatter {

	/**
	 * Format a time, or a pair of times. One possible configuration is that times are not displayed, in which
	 * case, return None.
	 */
	sealed trait TimeFormats {
		def formatTime(time: DateTime): Option[String]

		def formatTimes(interval: Interval): Option[(String, String)]
	}

	case object Hour24IncludeMins extends TimeFormats {
		private val hourMinuteFormat = DateTimeFormat.forPattern("HH:mm")

		def formatTime(time: DateTime): Option[String] = Some(hourMinuteFormat.print(time))

		def formatTimes(interval: Interval): Option[(String, String)] = Some((hourMinuteFormat.print(interval.getStart), hourMinuteFormat.print(interval.getEnd)))
	}

	case object OmitTimes extends TimeFormats {
		def formatTime(time: DateTime): Option[String] = None

		def formatTimes(interval: Interval): Option[(String, String)] = None
	}

	case object Hour12OptionalMins extends TimeFormats {
		private val hourMinuteFormat = DateTimeFormat.forPattern("h:mma")
		private val hourOnlyFormat = DateTimeFormat.forPattern("ha")

		def formatTime(time: DateTime): Option[String] = {
			if (time.getMinuteOfHour == 0) {
				Some(hourOnlyFormat.print(time).toLowerCase)
			} else {
				Some(hourMinuteFormat.print(time).toLowerCase)
			}
		}

		def formatTimes(interval: Interval): Option[(String, String)] = {
			def format(date:DateTime) = (if(date.getMinuteOfHour == 0)hourOnlyFormat else hourMinuteFormat).print(date).toLowerCase
			Some(format(interval.getStart), format(interval.getEnd))
		}
	}

	/**
	 * Format a date, or a pair of dates. In general, when formatting a pair of dates, if the month and/or year
	 * are the same on both dates then they are only printed once.
	 */
	sealed trait DateFormats {
		def formatDate(date: DateTime): String

		def formatDates(interval: Interval): (String, String)

		def ordinal(date: DateTime): String = "<sup>" + DateBuilder.ordinal(date.getDayOfMonth) + "</sup>"

		protected val yearFormat: DateTimeFormatter = DateTimeFormat.forPattern(" yyyy")
		protected val monthFormat: DateTimeFormatter = DateTimeFormat.forPattern(" MMM")

		protected def formatInterval(interval: Interval, dayFormat: DateTimeFormatter): (String, String) = {
			val sameMonth = interval.getStart.getMonthOfYear == interval.getEnd.getMonthOfYear
			val sameYear = interval.getStart.getYear == interval.getEnd.getYear
			val startString = dayFormat.print(interval.getStart) + ordinal(interval.getStart) +
				(if (!sameMonth) monthFormat.print(interval.getStart) else "") +
				(if (!sameYear) yearFormat.print(interval.getStart) else "")
			(startString,
				dayFormat.print(interval.getEnd) + ordinal(interval.getEnd) +
					monthFormat.print(interval.getEnd) +
					yearFormat.print(interval.getEnd))

		}
	}

	/**
	 * Include the day of the week in the output
	 */
	case object IncludeDays extends DateFormats {
		private val dayAndDateFormat = DateTimeFormat.forPattern("EE d")

		def formatDate(date: DateTime): String = {
			dayAndDateFormat.print(date) + ordinal(date) + monthFormat.print(date) + yearFormat.print(date)
		}

		def formatDates(interval: Interval): (String, String) = formatInterval(interval, dayAndDateFormat)
	}

	/**
	 * Omit the day of the week from the output, only print the day of the month
	 */
	case object OmitDays extends DateFormats {
		private val dateFormatWithoutDay = DateTimeFormat.forPattern("d")

		def formatDate(date: DateTime): String = {
			dateFormatWithoutDay.print(date) + ordinal(date) + yearFormat.print(date)
		}

		def formatDates(interval: Interval): (String, String) = formatInterval(interval, dateFormatWithoutDay)
	}

}
