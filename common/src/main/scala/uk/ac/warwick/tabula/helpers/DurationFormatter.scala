package uk.ac.warwick.tabula.helpers
import org.joda.time.{DateTime, Duration, Period, PeriodType, ReadablePeriod}
import org.joda.time.format.{PeriodFormatter, PeriodFormatterBuilder}
import uk.ac.warwick.tabula.web.views.BaseTemplateMethodModelEx

class DurationFormatterTag extends BaseTemplateMethodModelEx {
	override def execMethod(args: Seq[_]): String = args match {
		// TAB-688 when passed null values, return a null value
		case Seq(null) | Seq(null, null) => null
		case Seq(end: DateTime) => DurationFormatter.format(new DateTime(), end)
		case Seq(start: DateTime, end: DateTime) => DurationFormatter.format(start, end)
		case _ => throw new IllegalArgumentException("Bad args")
	}
}

/**
 * Actually formats Intervals, not Durations.
 */
object DurationFormatter {

	// if commas are disagreeable, change this to a single space
	val sep = ", "
	// if the final "and" is disagreeable, assign it sep
	val finalSep = " and "
	val ago = " ago"

	/* Determines which fields are populated when creating a Period for formatting.
	 * Notable absent from this value is weeks - we use months and days.
	 * You can change periodType to a value that has weeks - you'll then need to update the
	 * formatter to include weeks in its output, otherwise it'll just be missing.
	 */
	val periodType: PeriodType = PeriodType.yearMonthDayTime

	val formatter: PeriodFormatter = new PeriodFormatterBuilder()
		.appendYears.appendSuffix(" year", " years").appendSeparator(sep, finalSep)
		.appendMonths.appendSuffix(" month", " months").appendSeparator(sep, finalSep)
		.appendDays.appendSuffix(" day", " days").appendSeparator(sep, finalSep)
		.appendHours.appendSuffix(" hour", " hours").appendSeparator(sep, finalSep)
		.appendMinutes.appendSuffix(" minute", " minutes").appendSeparator(sep, finalSep)
		.appendSeconds.appendSuffix(" second", " seconds")
		.toFormatter

	/**
	 * Prints the given Interval as a period. It uses years, months, days, and the time,
	 * but it does not use weeks.
	 */
	def format(start: DateTime, end: DateTime): String =
		if ((start isAfter end) || (start isEqual end))
			formatter.print(toPeriod(end, start)).trim + ago
		else
			formatter.print(toPeriod(start, end)).trim

	private def toPeriod(start: DateTime, end: DateTime): ReadablePeriod = {
		val duration = new Duration(start, end)
		var period = new Period(start, end, periodType)
		period = stripTime(period, duration)
		period = stripSeconds(period, duration)
		period = stripMinutes(period, duration)
		period
	}

	private def stripTime(period: Period, duration: Duration): Period =
		if (duration.getStandardDays >= 7)
			period.withHours(0).withMinutes(0).withSeconds(0)
		else
			period

	/**
	 * If more than an hour, set seconds to 0 so they aren't printed.
	 * Returns the updated Period or the same one if no change.
	 */
	private def stripSeconds(period: Period, duration: Duration): Period =
		if (duration.getStandardHours >= 1)
			period.withSeconds(0)
		else
			period

	/**
	 * If more than a day, set minutes to 0 so they aren't printed.
	 * Returns the updated Period or the same one if no change.
	 */
	private def stripMinutes(period: Period, duration: Duration): Period =
		if (duration.getStandardDays >= 1) period.withMinutes(0)
		else period

}