package uk.ac.warwick.tabula.services

import org.joda.time._
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.AcademicYear

trait WeekToDateConverterComponent{
	val weekToDateConverter: WeekToDateConverter
}
trait WeekToDateConverter {
	def getWeekContainingDate(date: LocalDate): Option[WeekRange.Week]

	def intersectsWeek(interval: Interval, week: WeekRange.Week, year: AcademicYear): Boolean

	def toLocalDatetime(week: WeekRange.Week, day: DayOfWeek, time: LocalTime, year: AcademicYear): Option[LocalDateTime]
}

trait TermAwareWeekToDateConverterComponent extends WeekToDateConverterComponent{
	this: TermServiceComponent =>

	val weekToDateConverter: WeekToDateConverter = new TermAwareWeekRangeToDateConversionService

	class TermAwareWeekRangeToDateConversionService extends WeekToDateConverter {


		def getWeekContainingDate(date: LocalDate): Option[WeekRange.Week] = {
			val zonedDate = date.toDateTimeAtStartOfDay
			val year = AcademicYear.findAcademicYearContainingDate(zonedDate)
			val weeks = weeksForYear(year)
			// brute-force search through the map; since there should never be more than 53 entries to look for it's
			// hopefully OK
			weeks.find {
				case (week, interval) => interval.contains(zonedDate)
			}.map(_._1)
		}

		def intersectsWeek(interval: Interval, week: WeekRange.Week, year: AcademicYear): Boolean = {
			val weekInterval = weeksForYear(year).get(week)
			weekInterval.exists(interval.overlap(_) != null)
		}


		def toLocalDatetime(week: WeekRange.Week, day: DayOfWeek, time: LocalTime, year: AcademicYear): Option[LocalDateTime] = {
			weeksForYear(year).get(week).map(
				interval => interval.getStart
					.withDayOfWeek(day.jodaDayOfWeek)
					.withTime(time.getHourOfDay, time.getMinuteOfHour, time.getSecondOfMinute, time.getMillisOfSecond)
					.toLocalDateTime)
		}

		// TODO consider cacheing this data
		private def weeksForYear(year: AcademicYear) =
			termService.getAcademicWeeksForYear(year.dateInTermOne).toMap

		def weekNumberToDate(year: AcademicYear, weekNumber: Int, dayOfWeek: DayOfWeek): DateTime =
			weeksForYear(year)(weekNumber).getStart.withDayOfWeek(dayOfWeek.jodaDayOfWeek)
	}

}
