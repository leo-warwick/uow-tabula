package uk.ac.warwick.tabula.api.web.controllers.timetables

import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property._
import org.joda.time.LocalDate
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.{AcademicYear, DateFormats}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermService, TermServiceComponent}
import uk.ac.warwick.tabula.web.views.{IcalView, JSONView}
import TermDatesController._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.util.termdates.Term

object TermDatesController {
	case class NamedTerm(name: String, term: Term, weekRange: WeekRange)
}

@Controller
@RequestMapping(Array("/v1/termdates", "/v1/termdates.*"))
class TermDatesController extends ApiController
	with GetTermDatesApi
	with DefaultAcademicYearApi
	with AutowiringTermServiceComponent

@Controller
@RequestMapping(Array("/v1/termdates/{year}", "/v1/termdates/{year}.*"))
class TermDatesForYearController extends ApiController
	with GetTermDatesApi
	with PathVariableAcademicYearScopedApi
	with AutowiringTermServiceComponent

trait GetTermDatesApi {
	self: ApiController
		with AcademicYearScopedApi
		with TermServiceComponent =>

	@ModelAttribute("terms")
	def terms(@ModelAttribute("academicYear") academicYear: AcademicYear): Seq[NamedTerm] = {
		val weeks = termService.getAcademicWeeksForYear(academicYear.dateInTermOne).toMap

		val terms =
			weeks
				.map { case (weekNumber, dates) =>
					(weekNumber, termService.getTermFromAcademicWeekIncludingVacations(weekNumber, academicYear))
				}
				.groupBy { _._2 }
				.map { case (term, weekNumbersAndTerms) =>
					(term, WeekRange(weekNumbersAndTerms.keys.min, weekNumbersAndTerms.keys.max))
				}
				.toSeq
				.sortBy { case (_, weekRange) => weekRange.minWeek }

		TermService.orderedTermNames.zip(terms).map { case (name, (term, weekRange)) => NamedTerm(name, term, weekRange) }
	}

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def jsonTermDates(@ModelAttribute("terms") terms: Seq[NamedTerm]): Mav = {
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"terms" -> terms.map { t => Map(
				"name" -> t.name,
				"start" -> DateFormats.IsoDate.print(t.term.getStartDate.toLocalDate),
				"end" -> DateFormats.IsoDate.print(t.term.getEndDate.toLocalDate),
				"weekRange" -> Map(
					"minWeek" -> t.weekRange.minWeek,
					"maxWeek" -> t.weekRange.maxWeek
				)
			)}
		)))
	}

	@RequestMapping(method = Array(GET), produces = Array("text/calendar"))
	def icalTermDates(@ModelAttribute("terms") terms: Seq[NamedTerm], @ModelAttribute("academicYear") academicYear: AcademicYear): Mav = {
		val cal: Calendar = new Calendar
		cal.getProperties.add(Version.VERSION_2_0)
		cal.getProperties.add(new ProdId("-//Tabula//University of Warwick IT Services//EN"))
		cal.getProperties.add(CalScale.GREGORIAN)
		cal.getProperties.add(Method.PUBLISH)
		cal.getProperties.add(new XProperty("X-PUBLISHED-TTL", "PT1W")) // 1 week
		cal.getProperties.add(new XProperty("X-WR-CALNAME", s"Term dates - ${academicYear.toString}"))

		def toIcalDate(date: LocalDate): net.fortuna.ical4j.model.Date =
			new net.fortuna.ical4j.model.Date(date.toString("yyyyMMdd"))

		terms.zipWithIndex.foreach { case (t, termNumber) =>
			val event: VEvent = new VEvent(toIcalDate(t.term.getStartDate.toLocalDate), toIcalDate(t.term.getEndDate.toLocalDate.plusDays(1)), t.name.safeSubstring(0, 255))

			event.getProperties.add(new Uid(s"${academicYear.startYear}-term-$termNumber"))
			event.getProperties.add(Method.PUBLISH)
			event.getProperties.add(Transp.OPAQUE)

			val organiser: Organizer = new Organizer(s"MAILTO:no-reply@tabula.warwick.ac.uk")
			event.getProperties.add(organiser)

			event.getProperties.add(new XProperty("X-MICROSOFT-CDO-BUSYSTATUS", "FREE"))

			cal.getComponents.add(event)
		}

		Mav(new IcalView(cal), "filename" -> s"termdates-${academicYear.startYear}.ics")
	}

}
