package uk.ac.warwick.tabula.services.turnitin

import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.DateTime

/**
 * Object handling dates and date formats with the Turnitin API.
 */
object TurnitinDates {
	val timestampFormat: DateTimeFormatter = DateTimeFormat.forPattern("YYYYMMddHHmm").withZoneUTC
	val dateFormat: DateTimeFormatter = DateTimeFormat.forPattern("YYYYMMdd")

	def gmtTimestamp: String = {
		val s = timestampFormat print DateTime.now
		s.substring(0, s.length - 1) // Must have only the first digit of the minutes!
	}

	def yearsFromNow(years: Int): String = dateFormat print DateTime.now.plusYears(years)
	def monthsFromNow(months: Int): String = dateFormat print DateTime.now.plusMonths(months)
}