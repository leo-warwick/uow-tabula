package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.helpers.{Futures, SystemClockComponent}
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, AutowiringUserLookupComponent, AutowiringSmallGroupServiceComponent}
import uk.ac.warwick.tabula.timetables.TimetableEvent

import scala.concurrent.Future

trait StaffTimetableEventSource extends MemberTimetableEventSource[StaffMember] {
	override def eventsFor(staff: StaffMember, currentUser: CurrentUser, context: TimetableEvent.Context): Future[EventList]
}

trait StaffTimetableEventSourceComponent {
	def staffTimetableEventSource: StaffTimetableEventSource
}

trait CombinedStaffTimetableEventSourceComponent extends StaffTimetableEventSourceComponent {
	this: StaffTimetableFetchingServiceComponent with SmallGroupEventTimetableEventSourceComponent =>

	def staffTimetableEventSource: StaffTimetableEventSource = new CombinedStaffTimetableEventSource

	class CombinedStaffTimetableEventSource() extends StaffTimetableEventSource {

		def eventsFor(staff: StaffMember, currentUser: CurrentUser, context: TimetableEvent.Context): Future[EventList] = {
			val timetableEvents: Future[EventList] = timetableFetchingService.getTimetableForStaff(staff.universityId)
			val smallGroupEvents: Future[EventList] = staffGroupEventSource.eventsFor(staff, currentUser, context)

			Futures.combine(Seq(timetableEvents, smallGroupEvents), EventList.combine)
		}

	}
}

trait AutowiringStaffTimetableEventSourceComponent extends StaffTimetableEventSourceComponent {
	val staffTimetableEventSource: StaffTimetableEventSource = (new CombinedStaffTimetableEventSourceComponent
		with SmallGroupEventTimetableEventSourceComponentImpl
		with CombinedHttpTimetableFetchingServiceComponent
		with AutowiringSmallGroupServiceComponent
		with AutowiringUserLookupComponent
		with AutowiringScientiaConfigurationComponent
		with AutowiringCelcatConfigurationComponent
		with AutowiringExamTimetableConfigurationComponent
		with AutowiringSecurityServiceComponent
		with SystemClockComponent
	).staffTimetableEventSource
}