package uk.ac.warwick.tabula.api.web.controllers.timetables

import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property._
import org.joda.time.LocalDate
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{IcalView, JSONView}
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl

import scala.collection.JavaConverters._

object HolidayDatesController {

}

@Controller
@RequestMapping(Array("/v1/holidaydates", "/v1/holidaydates.*"))
class HolidayDatesController extends ApiController
	with GetHolidayDatesApi

trait GetHolidayDatesApi {
	self: ApiController =>

	lazy val holidayDates: Seq[LocalDate] = new WorkingDaysHelperImpl().getHolidayDates.asScala.toSeq.sorted

	@ModelAttribute("holidayDates")
	def holidayDatesModelAttribute: Seq[LocalDate] = holidayDates

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def jsonTermDates(@ModelAttribute("holidayDates") dates: Seq[LocalDate]): Mav = {
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"dates" -> dates.map(DateFormats.IsoDate.print)
		)))
	}

	@RequestMapping(method = Array(GET), produces = Array("text/calendar"))
	def icalTermDates(@ModelAttribute("holidayDates") dates: Seq[LocalDate]): Mav = {
		val cal: Calendar = new Calendar
		cal.getProperties.add(Version.VERSION_2_0)
		cal.getProperties.add(new ProdId("-//Tabula//University of Warwick IT Services//EN"))
		cal.getProperties.add(CalScale.GREGORIAN)
		cal.getProperties.add(Method.PUBLISH)
		cal.getProperties.add(new XProperty("X-PUBLISHED-TTL", "PT1W")) // 1 week
		cal.getProperties.add(new XProperty("X-WR-CALNAME", "Statutory and Customary Holiday Days"))

		def toIcalDate(date: LocalDate): net.fortuna.ical4j.model.Date =
			new net.fortuna.ical4j.model.Date(date.toString("yyyyMMdd"))

		// Join consecutive dates into ranges
		val dateRanges = dates.tail.toList.foldLeft(List(dates.take(1).toList)) {
			case (acc @ (lst @ hd :: _) :: tl, el) =>
				if (el == hd.plusDays(1)) (el :: lst) :: tl
				else (el :: Nil) :: acc
			case _ => Nil
		}.map(_.reverse).reverse

		dateRanges.foreach { range =>
			val event: VEvent = new VEvent(toIcalDate(range.head), toIcalDate(range.last.plusDays(1)), "University Holiday")

			event.getProperties.add(new Uid(s"holidayday-${range.map(DateFormats.IsoDate.print).mkString("-")}"))
			event.getProperties.add(Method.PUBLISH)
			event.getProperties.add(Transp.OPAQUE)

			val organiser: Organizer = new Organizer(s"MAILTO:no-reply@tabula.warwick.ac.uk")
			event.getProperties.add(organiser)

			event.getProperties.add(new XProperty("X-MICROSOFT-CDO-BUSYSTATUS", "OOF"))

			cal.getComponents.add(event)
		}

		Mav(new IcalView(cal), "filename" -> s"holidaydates.ics")
	}
}
