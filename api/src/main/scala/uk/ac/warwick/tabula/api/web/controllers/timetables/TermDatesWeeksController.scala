package uk.ac.warwick.tabula.api.web.controllers.timetables

import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property._
import org.joda.time.{DateTime, Interval, LocalDate}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model.groups.WeekRange.Week
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.{AcademicYear, DateFormats}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.helpers.{KnowsUserNumberingSystem, WholeWeekFormatter}
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, AutowiringUserSettingsServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.web.views.{IcalView, JSONView}
import TermDatesWeeksController._
import uk.ac.warwick.tabula.web.Mav

object TermDatesWeeksController {
	case class TermWeek(weekNumber: Week, interval: Interval, name: String)
}

@Controller
@RequestMapping(Array("/v1/termdates/weeks", "/v1/termdates/weeks.*"))
class TermDatesWeeksController extends ApiController
	with GetTermDatesWeeksApi
	with DefaultAcademicYearApi
	with AutowiringTermServiceComponent
	with KnowsUserNumberingSystem
	with AutowiringUserSettingsServiceComponent

@Controller
@RequestMapping(Array("/v1/termdates/{year}/weeks", "/v1/termdates/{year}/weeks.*"))
class TermDatesWeeksForYearController extends ApiController
	with GetTermDatesWeeksApi
	with PathVariableAcademicYearScopedApi
	with AutowiringTermServiceComponent
	with KnowsUserNumberingSystem
	with AutowiringUserSettingsServiceComponent

sealed trait AcademicYearScopedApi

trait PathVariableAcademicYearScopedApi extends AcademicYearScopedApi {
	self: PermissionsCheckingMethods =>
	@ModelAttribute("academicYear") def academicYear(@PathVariable year: AcademicYear): AcademicYear = mandatory(year)
}

trait DefaultAcademicYearApi extends AcademicYearScopedApi {
	self: TermServiceComponent =>
	@ModelAttribute("academicYear") def academicYear: AcademicYear = AcademicYear.findAcademicYearContainingDate(DateTime.now)
}

trait GetTermDatesWeeksApi {
	self: ApiController
		with AcademicYearScopedApi
		with TermServiceComponent
		with KnowsUserNumberingSystem =>

	@ModelAttribute("termDates")
	def getTermDates(@ModelAttribute("academicYear") academicYear: AcademicYear, @RequestParam(value = "numberingSystem", required = false) reqNumberingSystem: String): Seq[TermWeek] = {
		val weeks = termService.getAcademicWeeksForYear(academicYear.dateInTermOne)
		val formatter = new WholeWeekFormatter(academicYear)

		val numbSystem =
			if (reqNumberingSystem.hasText) reqNumberingSystem
			else if (user.exists) numberingSystem(user, None)
			else WeekRange.NumberingSystem.Academic // Academic makes most sense for this API

		weeks.map { case (weekNumber, interval) =>
			val asString = formatter.format(Seq(WeekRange(weekNumber)), DayOfWeek.Monday, numbSystem, short = false)

			TermWeek(weekNumber, interval, asString)
		}
	}

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def jsonTermDates(@ModelAttribute("termDates") weeks: Seq[TermWeek]): Mav = {
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"weeks" -> weeks.map { w => Map(
				"weekNumber" -> w.weekNumber,
				"start" -> DateFormats.IsoDate.print(w.interval.getStart.toLocalDate),
				"end" -> DateFormats.IsoDate.print(w.interval.getEnd.toLocalDate.minusDays(1)),
				"name" -> w.name
			)}
		)))
	}

	@RequestMapping(method = Array(GET), produces = Array("text/calendar"))
	def icalTermDates(@ModelAttribute("termDates") weeks: Seq[TermWeek], @ModelAttribute("academicYear") academicYear: AcademicYear): Mav = {
		val cal: Calendar = new Calendar
		cal.getProperties.add(Version.VERSION_2_0)
		cal.getProperties.add(new ProdId("-//Tabula//University of Warwick IT Services//EN"))
		cal.getProperties.add(CalScale.GREGORIAN)
		cal.getProperties.add(Method.PUBLISH)
		cal.getProperties.add(new XProperty("X-PUBLISHED-TTL", "PT1W")) // 1 week
		cal.getProperties.add(new XProperty("X-WR-CALNAME", s"Week numbers - ${academicYear.toString}"))

		def toIcalDate(date: LocalDate): net.fortuna.ical4j.model.Date =
			new net.fortuna.ical4j.model.Date(date.toString("yyyyMMdd"))

		weeks.foreach { w =>
			val event: VEvent = new VEvent(toIcalDate(w.interval.getStart.toLocalDate), toIcalDate(w.interval.getEnd.toLocalDate), w.name.safeSubstring(0, 255))

			event.getProperties.add(new Uid(s"${academicYear.startYear}-week-${w.weekNumber}"))
			event.getProperties.add(Method.PUBLISH)
			event.getProperties.add(Transp.OPAQUE)

			val organiser: Organizer = new Organizer(s"MAILTO:no-reply@tabula.warwick.ac.uk")
			event.getProperties.add(organiser)

			event.getProperties.add(new XProperty("X-MICROSOFT-CDO-BUSYSTATUS", "FREE"))

			cal.getComponents.add(event)
		}

		Mav(new IcalView(cal), "filename" -> s"weeknumbers-${academicYear.startYear}.ics")
	}

}
