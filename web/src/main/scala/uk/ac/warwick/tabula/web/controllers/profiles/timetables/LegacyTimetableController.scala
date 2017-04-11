package uk.ac.warwick.tabula.web.controllers.profiles.timetables

import javax.servlet.http.HttpServletRequest

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import org.springframework.web.servlet.view.RedirectView
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(value = Array("/profiles/timetable"))
class LegacyTimetableController extends BaseController {

	@Value("${toplevel.url}") var toplevelUrl: String = _

	@RequestMapping
	def redirectCurrentUser(user: CurrentUser): Mav =
		user.profile match {
			case Some(profile) => Redirect(Routes.profiles.Profile.timetable(profile))
			case _ => Redirect(Routes.profiles.home)
		}

	@RequestMapping(value = Array("/{member}"))
	def redirectMember(@PathVariable member: Member, request: HttpServletRequest): RedirectView = {
		val r = new RedirectView(toplevelUrl + Routes.profiles.Profile.timetable(member) + request.getQueryString.maybeText.map("?" + _).getOrElse(""))
		r.setStatusCode(HttpStatus.MOVED_PERMANENTLY)
		r
	}

	@RequestMapping(value = Array("/api"))
	def redirectApi(@RequestParam(value="whoFor") whoFor: Member, request: HttpServletRequest): RedirectView = {
		val r = new RedirectView(toplevelUrl + Routes.api.timetables.calendar(whoFor) + request.getQueryString.maybeText.map("?" + _).getOrElse(""))
		r.setStatusCode(HttpStatus.MOVED_PERMANENTLY)
		r
	}

	@RequestMapping(value = Array("/ical/{timetableHash}.ics"))
	def redirectIcs(@PathVariable timetableHash: String, request: HttpServletRequest): RedirectView = {
		val r = new RedirectView(toplevelUrl + Routes.api.timetables.calendarICalForHash(mandatory(timetableHash)) + request.getQueryString.maybeText.map("?" + _).getOrElse(""))
		r.setStatusCode(HttpStatus.MOVED_PERMANENTLY)
		r
	}

	@RequestMapping(value = Array("/ical"))
	def redirectIcal(@RequestParam("timetableHash") timetableHash: String, request: HttpServletRequest): RedirectView = {
		val r = new RedirectView(toplevelUrl + Routes.api.timetables.calendarICalForHash(mandatory(timetableHash)) + request.getQueryString.maybeText.map("?" + _).getOrElse(""))
		r.setStatusCode(HttpStatus.MOVED_PERMANENTLY)
		r
	}

	@RequestMapping(value = Array("/{member}/ical, /{member}/timetable.ics"))
	def redirectIcalForMember(@PathVariable member: Member, request: HttpServletRequest): RedirectView = {
		val r = new RedirectView(toplevelUrl + Routes.api.timetables.calendarICal(mandatory(member)) + request.getQueryString.maybeText.map("?" + _).getOrElse(""))
		r.setStatusCode(HttpStatus.MOVED_PERMANENTLY)
		r
	}

}
