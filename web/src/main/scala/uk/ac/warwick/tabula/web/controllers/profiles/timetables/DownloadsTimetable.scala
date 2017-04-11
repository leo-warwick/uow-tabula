package uk.ac.warwick.tabula.web.controllers.profiles.timetables

import org.joda.time.{Hours, LocalTime, Minutes}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.commands.profiles.PhotosWarwickMemberPhotoUrlGeneratorComponent
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.pdf.FreemarkerXHTMLPDFGeneratorComponent
import uk.ac.warwick.tabula.timetables.TimetableEvent
import uk.ac.warwick.tabula.web.views.{AutowiredTextRendererComponent, PDFView}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

trait DownloadsTimetable extends TaskBenchmarking {

	case class RoundedEventTime(startTime: LocalTime, endTime: LocalTime, duration: Int, hourRange: Seq[LocalTime])

	def getTimetable(
		events: Seq[TimetableEvent],
		academicYear: AcademicYear,
		fileNameSuffix: String,
		title: String
	): PDFView = {

		if (events.isEmpty) {

			new PDFView(
				s"timetable-$fileNameSuffix.pdf",
				"/WEB-INF/freemarker/profiles/timetables/export.ftl",
				Map(
					"title" -> title,
					"academicYear" -> academicYear,
					"days" -> DayOfWeek.members,
					"hours" -> (0 until Hours.hoursBetween(new LocalTime(9, 0), new LocalTime(17, 0)).getHours).map(new LocalTime(9, 0).plusHours),
					"eventMap" -> Map()
				)
			) with FreemarkerXHTMLPDFGeneratorComponent
				with AutowiredTextRendererComponent
				with PhotosWarwickMemberPhotoUrlGeneratorComponent

		} else {

			// Round event times to the nearest hour
			def roundTime(time: LocalTime): LocalTime = {
				val floor = new LocalTime(time.getHourOfDay, 0)
				if (Minutes.minutesBetween(floor, time).getMinutes >= 30) {
					new LocalTime(time.getHourOfDay + 1, 0)
				} else {
					floor
				}
			}

			val roundedEventTimes: Map[TimetableEvent, RoundedEventTime] =
				events.map { event =>
					val roundedStartTime = roundTime(event.startTime)
					val roundedEndTime = roundTime(event.endTime)
					val duration = Hours.hoursBetween(roundedStartTime, roundedEndTime).getHours
					event -> RoundedEventTime(
						startTime = roundedStartTime,
						endTime = roundedEndTime,
						duration = duration,
						hourRange = (0 until duration).map(roundedStartTime.plusHours)
					)
				}.toMap

			// Find the earliest/latest event start/end times, with at least 09:00/17:00
			val minTime = {
				val eventsMinTime = roundedEventTimes.map { case (_, (times)) => times.startTime }.min
				val nineAm = new LocalTime(9, 0)
				if (eventsMinTime.isAfter(nineAm)) nineAm else eventsMinTime
			}
			val maxTime = {
				val eventsMaxTime = roundedEventTimes.map { case (_, (times)) => times.endTime }.max
				val fivePm = new LocalTime(17, 0)
				if (eventsMaxTime.isBefore(fivePm)) fivePm else eventsMaxTime
			}

			val hours: Seq[LocalTime] = (0 until Hours.hoursBetween(minTime, maxTime).getHours).map(minTime.plusHours)
			val nullHourMap: Map[LocalTime, TimetableEvent] = hours.map(hour => hour -> null).toMap

			def addEventToRow(event: TimetableEvent, row: Map[LocalTime, TimetableEvent]): Map[LocalTime, TimetableEvent] = {
				row.map { case (hour, cell) => hour -> {
					if (roundedEventTimes(event).hourRange.contains(hour)) {
						// Just check we're not overwriting anything; we shouldn't be trying to add the event if so
						if (cell != null) {
							throw new IllegalArgumentException(s"Tried to add event ${event.uid} at ${hour.getHourOfDay}:00 but event ${cell.uid} is already there")
						} else {
							event
						}
					} else {
						cell
					}
				}}
			}

			val eventGrid: Map[DayOfWeek, Seq[Map[LocalTime, TimetableEvent]]] = benchmarkTask("eventGrid") {
				val eventsByDay = events.groupBy(_.day)
				eventsByDay.mapValues(dayEvents => {
					// Add events to rows where they fit; start with the longest
					val sortedEvents = dayEvents.sortBy(roundedEventTimes(_).duration).reverse
					sortedEvents.foldLeft(Seq[Map[LocalTime, TimetableEvent]](nullHourMap)) { (rows, event) =>
						// Find a row with enough room for the event, keeping the rows in order
						val invalidHeadRows = rows.takeWhile(row => roundedEventTimes(event).hourRange.exists(hour => row(hour) != null))
						val validRows = rows.drop(invalidHeadRows.size).takeWhile(row => roundedEventTimes(event).hourRange.exists(hour => row(hour) == null))
						val tailRows = rows.drop(invalidHeadRows.size).drop(validRows.size)
						validRows match {
							case Nil => // Add new row
								invalidHeadRows ++ Seq(addEventToRow(event, nullHourMap)) ++ tailRows
							case validRow :: otherValidRows => // Add to existing row
								invalidHeadRows ++ Seq(addEventToRow(event, validRow)) ++ otherValidRows ++ tailRows
						}
					}
				})
			}

			new PDFView(
				s"timetable-$fileNameSuffix.pdf",
				"/WEB-INF/freemarker/profiles/timetables/export.ftl",
				Map(
					"title" -> title,
					"academicYear" -> academicYear,
					"days" -> DayOfWeek.members,
					"hours" -> hours,
					"eventGrid" -> eventGrid
				)
			) with FreemarkerXHTMLPDFGeneratorComponent
				with AutowiredTextRendererComponent
				with PhotosWarwickMemberPhotoUrlGeneratorComponent

		}
	}

}
