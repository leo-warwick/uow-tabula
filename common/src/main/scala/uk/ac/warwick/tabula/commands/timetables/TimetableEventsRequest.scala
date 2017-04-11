package uk.ac.warwick.tabula.commands.timetables

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.JavaImports.JLong

trait TimetableEventsRequest {

	var from: JLong = LocalDate.now.minusMonths(1).toDateTimeAtStartOfDay.getMillis
	var to: JLong = LocalDate.now.plusMonths(1).toDateTimeAtStartOfDay.getMillis
	def start: LocalDate = new DateTime(from).toLocalDate
	def end: LocalDate = new DateTime(to).toLocalDate

}
