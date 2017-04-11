package uk.ac.warwick.tabula.web.controllers.profiles.timetables

import org.joda.time.{DateTime, DateTimeConstants, Days, LocalDate}
import uk.ac.warwick.tabula.commands.profiles.PhotosWarwickMemberPhotoUrlGeneratorComponent
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.helpers.{IntervalFormatter, KnowsUserNumberingSystem, WeekRangesFormatter}
import uk.ac.warwick.tabula.pdf.FreemarkerXHTMLPDFGeneratorComponent
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentServiceComponent, TermServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.timetables.EventOccurrence
import uk.ac.warwick.tabula.web.views.{AutowiredTextRendererComponent, FullCalendarEvent, PDFView}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

trait DownloadsTimetableCalendar {

	self: TermServiceComponent with KnowsUserNumberingSystem
		with ModuleAndDepartmentServiceComponent with UserLookupComponent =>

	def getCalendar(
		events: Seq[EventOccurrence],
		startDate: LocalDate,
		endDate: LocalDate,
		renderDate: LocalDate,
		calendarView: String,
		user: CurrentUser,
		fileNameSuffix: String
	): PDFView = {
		val fullCalendarEvents = FullCalendarEvent.colourEvents(events.map(FullCalendarEvent(_, userLookup)))
		val allDays: Seq[LocalDate] = (0 until Days.daysBetween(startDate, endDate).getDays).map(startDate.plusDays)

		calendarView match {
			case "agendaDay" | "agendaWeek" =>
				agendaView(fullCalendarEvents, calendarView, renderDate, startDate, endDate, allDays, user, fileNameSuffix)
			case _ =>
				monthView(fullCalendarEvents, calendarView, renderDate, startDate, endDate, allDays, fileNameSuffix)
		}
	}

	private def agendaView(
		fullCalendarEvents: Seq[FullCalendarEvent],
		thisCalendarView: String,
		thisRenderDate: LocalDate,
		startDate: LocalDate,
		endDate: LocalDate,
		allDays: Seq[LocalDate],
		user: CurrentUser,
		fileNameSuffix: String
	): PDFView = {
		val groupedEvents: Map[LocalDate, Map[Int, Seq[FullCalendarEvent]]] = fullCalendarEvents
			.groupBy(e => new DateTime(e.start*1000).toLocalDate)
			.mapValues(_.groupBy(e => new DateTime(e.start*1000).getHourOfDay))

		val title = {
			if (thisCalendarView == "agendaWeek") {
				val thisWeek = termService.getTermFromDateIncludingVacations(thisRenderDate.toDateTimeAtStartOfDay)
					.getAcademicWeekNumber(thisRenderDate.toDateTimeAtStartOfDay)
				val system = numberingSystem(user, moduleAndDepartmentService.getDepartmentByCode(user.departmentCode))
				val description = WeekRangesFormatter.format(
					Seq(WeekRange(thisWeek)),
					DayOfWeek(thisRenderDate.dayOfWeek.get),
					AcademicYear.findAcademicYearContainingDate(thisRenderDate.toDateTimeAtStartOfDay),
					system
				)
				if (system == WeekRange.NumberingSystem.Academic || description.startsWith("Term")) {
					description
				} else {
					IntervalFormatter.format(startDate.toDateTimeAtStartOfDay, endDate.toDateTimeAtStartOfDay, includeTime = false, includeDays = false)
				}
			} else {
				""
			}
		}

		createPdfView(fileNameSuffix, Map(
			"calendarView" -> thisCalendarView,
			"allDays" -> allDays,
			"groupedEvents" -> groupedEvents,
			"renderDate" -> thisRenderDate,
			"title" -> title
		))
	}

	private def monthView(
		fullCalendarEvents: Seq[FullCalendarEvent],
		thisCalendarView: String,
		thisRenderDate: LocalDate,
		startDate: LocalDate,
		endDate: LocalDate,
		allDays: Seq[LocalDate],
		fileNameSuffix: String
	): PDFView = {
		// Include year so week 52 of 2016 is before week 1 2017 (zero-pad the week number)
		// Ensure to roll back to the Monday of that week
		def formattedWeekNumber(d: LocalDate): Int = {
			val monday = d.withDayOfWeek(DateTimeConstants.MONDAY)
			(monday.year.get.toString + f"${monday.weekOfWeekyear.get}%02d").toInt
		}
		val groupedEvents: Map[Int, Map[LocalDate, Seq[FullCalendarEvent]]] = fullCalendarEvents
			.groupBy(e => formattedWeekNumber(new DateTime(e.start*1000).toLocalDate))
			.mapValues(_.groupBy(e => new DateTime(e.start*1000).toLocalDate))
		val firstDayOfMonth = thisRenderDate.minusDays(thisRenderDate.getDayOfMonth - 1)
		val lastDayOfMonth = firstDayOfMonth.plusDays(thisRenderDate.dayOfMonth.getMaximumValue - 1)
		createPdfView(fileNameSuffix, Map(
			"calendarView" -> thisCalendarView,
			"groupedAllDays" -> allDays.groupBy(formattedWeekNumber),
			"groupedEvents" -> groupedEvents,
			"renderDate" -> thisRenderDate,
			"firstDayOfMonth" -> firstDayOfMonth,
			"lastDayOfMonth" -> lastDayOfMonth
		))
	}

	private def createPdfView(fileNameSuffix: String, modelMap: Map[String, Any]): PDFView =
		new PDFView(
			s"timetable-calendar-$fileNameSuffix.pdf",
			"/WEB-INF/freemarker/profiles/timetables/export-calendar.ftl",
			modelMap
		) with FreemarkerXHTMLPDFGeneratorComponent
			with AutowiredTextRendererComponent
			with PhotosWarwickMemberPhotoUrlGeneratorComponent
}
