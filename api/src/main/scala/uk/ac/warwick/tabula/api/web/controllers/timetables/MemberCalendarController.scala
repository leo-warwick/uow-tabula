package uk.ac.warwick.tabula.api.web.controllers.timetables

import javax.validation.Valid

import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property._
import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.controllers.timetables.MemberCalendarController._
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.timetables.ViewMemberEventsCommand
import uk.ac.warwick.tabula.data.model.{Member, StaffMember, StudentMember}
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.{AutowiringTermBasedEventOccurrenceServiceComponent, EventOccurrenceServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.timetables.EventOccurrence
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{FullCalendarEvent, IcalView, JSONErrorView, JSONView}

import scala.util.{Failure, Success}

object MemberCalendarController {
	type TimetableCommand = ViewMemberEventsCommand.TimetableCommand
}

@Controller
@RequestMapping(Array("/v1/member/{member}/timetable/calendar", "/v1/member/{member}/timetable/calendar.*"))
class MemberCalendarController extends ApiController
	with GetMemberCalendarJsonApi
	with GetMemberCalendarIcalApi
	with PathVariableMemberCalendarApi
	with GeneratesTimetableIcalFeed
	with AutowiringUserLookupComponent
	with AutowiringTermBasedEventOccurrenceServiceComponent
	with AutowiringTermServiceComponent {
	validatesSelf[SelfValidating]
}

@Controller
@RequestMapping(Array("/v1/timetable/calendar/{timetableHash}.ics"))
class MemberTimetableHashCalendarController extends ApiController
	with GetMemberCalendarIcalApi
	with PathVariableTimetableHashMemberCalendarApi
	with GeneratesTimetableIcalFeed
	with AutowiringUserLookupComponent
	with AutowiringTermBasedEventOccurrenceServiceComponent
	with AutowiringTermServiceComponent
	with AutowiringProfileServiceComponent {
	validatesSelf[SelfValidating]
}

sealed trait MemberCalendarApi

trait PathVariableMemberCalendarApi extends MemberCalendarApi {
	self: PermissionsCheckingMethods =>

	@ModelAttribute("getTimetableCommand")
	def getTimetableCommand(@PathVariable member: Member, user: CurrentUser): TimetableCommand =
		ViewMemberEventsCommand(mandatory(member), user)

}

trait PathVariableTimetableHashMemberCalendarApi extends MemberCalendarApi {
	self: ProfileServiceComponent =>

	@ModelAttribute("getTimetableCommand")
	def getTimetableCommand(@PathVariable timetableHash: String): TimetableCommand = {
		// Use a mocked up CurrentUser, as the actual current user is probably not logged in
		def currentUser(m: Member) =
			new CurrentUser(
				realUser = m.asSsoUser.tap { _.setIsLoggedIn(true) },
				apparentUser = m.asSsoUser.tap { _.setIsLoggedIn(true) },
				profile = Some(m),
				sysadmin = false,
				masquerader = false,
				god = false
			)

		profileService.getMemberByTimetableHash(timetableHash).map {
			case student: StudentMember => ViewMemberEventsCommand.public(student, currentUser(student))
			case staff: StaffMember => ViewMemberEventsCommand.public(staff, currentUser(staff))
		}.getOrElse(throw new ItemNotFoundException)
	}

}

trait GetMemberCalendarJsonApi {
	self: ApiController
		with MemberCalendarApi
		with UserLookupComponent =>

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def showMemberTimetable(@Valid @ModelAttribute("getTimetableCommand") command: TimetableCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else command.apply() match {
			case Success(result) => Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"events" -> FullCalendarEvent.colourEvents(result.events.map(FullCalendarEvent(_, userLookup))),
				"lastUpdated" -> result.lastUpdated.map(DateFormats.IsoDateTime.print).orNull
			)))
			case Failure(t) => throw new RequestFailedException("The timetabling service could not be reached", t)
		}
	}
}

trait GetMemberCalendarIcalApi {
	self: ApiController
		with MemberCalendarApi
		with UserLookupComponent
		with GeneratesTimetableIcalFeed
		with TermServiceComponent =>

	@RequestMapping(method = Array(GET), produces = Array("text/calendar"))
	def icalMemberTimetable(@Valid @ModelAttribute("getTimetableCommand") command: TimetableCommand): Mav = {
		val member = command.member

		val year = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		// Start from either 1 week ago, or the start of the current academic year, whichever is earlier
		val start = {
			val startOfYear = termService.getTermFromAcademicWeek(1, year).getStartDate
			val oneWeekAgo = DateTime.now.minusWeeks(1)

			if (startOfYear.isBefore(oneWeekAgo)) startOfYear else oneWeekAgo
		}

		// End either at the end of the current academic year, or in 15 weeks time, whichever is later
		val end = {
			val endOfYear = termService.getTermFromAcademicWeek(1, year + 1).getStartDate
			val fifteenWeeksTime = DateTime.now.plusWeeks(15)

			if (endOfYear.isAfter(fifteenWeeksTime)) endOfYear else fifteenWeeksTime
		}

		command.from = start.getMillis
		command.to = end.getMillis

		command.apply() match {
			case Success(result) =>
				val cal = getIcalFeed(result.events, member)
				Mav(new IcalView(cal), "filename" -> s"${member.universityId}.ics")
			case Failure(t) =>
				throw new RequestFailedException("The timetabling service could not be reached", t)
		}

	}

}

trait GeneratesTimetableIcalFeed {
	self: EventOccurrenceServiceComponent =>

	def getIcalFeed(timetableEvents: Seq[EventOccurrence], member: Member): Calendar = {
		val cal: Calendar = new Calendar
		cal.getProperties.add(Version.VERSION_2_0)
		cal.getProperties.add(new ProdId("-//Tabula//University of Warwick IT Services//EN"))
		cal.getProperties.add(CalScale.GREGORIAN)
		cal.getProperties.add(Method.PUBLISH)
		cal.getProperties.add(new XProperty("X-PUBLISHED-TTL", "PT12H"))
		cal.getProperties.add(new XProperty("X-WR-CALNAME", s"Tabula timetable - ${member.universityId}"))

		for (event <- timetableEvents) {
			val vEvent: VEvent = eventOccurrenceService.toVEvent(event)
			cal.getComponents.add(vEvent)
		}

		// TAB-2722 Empty calendars throw a validation exception
		// Add Xmas day to get around this
		if (timetableEvents.isEmpty) {
			val xmasVEvent = new VEvent(
				new net.fortuna.ical4j.model.DateTime(new DateTime(DateTime.now.getYear, 12, 25, 0, 0).getMillis),
				new net.fortuna.ical4j.model.DateTime(new DateTime(DateTime.now.getYear, 12, 25, 0, 0).getMillis),
				"Christmas day"
			)
			xmasVEvent.getProperties.add(new Organizer("MAILTO:no-reply@tabula.warwick.ac.uk"))
			xmasVEvent.getProperties.add(new Uid("Tabula-Stub-Xmas"))
			cal.getComponents.add(xmasVEvent)
		}

		cal
	}

}