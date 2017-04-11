package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.timetables.TimetableEvent

import scala.concurrent.Future

trait MemberTimetableEventSource[A <: Member] {
	def eventsFor(member: A, currentUser: CurrentUser, context: TimetableEvent.Context): Future[EventList]
}