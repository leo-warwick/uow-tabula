package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.helpers.{Futures, SystemClockComponent}
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, AutowiringUserLookupComponent, AutowiringSmallGroupServiceComponent}
import uk.ac.warwick.tabula.timetables.TimetableEvent

import scala.concurrent.Future

trait StudentTimetableEventSource extends MemberTimetableEventSource[StudentMember] {
	override def eventsFor(student: StudentMember, currentUser: CurrentUser, context: TimetableEvent.Context): Future[EventList]
}

trait StudentTimetableEventSourceComponent {
	def studentTimetableEventSource: StudentTimetableEventSource
}

trait CombinedStudentTimetableEventSourceComponent extends StudentTimetableEventSourceComponent {
	self: StaffAndStudentTimetableFetchingServiceComponent with SmallGroupEventTimetableEventSourceComponent =>

	def studentTimetableEventSource: StudentTimetableEventSource = new CombinedStudentTimetableEventSource

	class CombinedStudentTimetableEventSource() extends StudentTimetableEventSource {
		def eventsFor(student: StudentMember, currentUser: CurrentUser, context: TimetableEvent.Context): Future[EventList] = {
			val timetableEvents: Future[EventList] = timetableFetchingService.getTimetableForStudent(student.universityId)
			val smallGroupEvents: Future[EventList] = studentGroupEventSource.eventsFor(student, currentUser, context)

			val staffEvents: Future[EventList] =
				if (student.isPGR) { timetableFetchingService.getTimetableForStaff(student.universityId) }
				else Future.successful(EventList.fresh(Nil))

			Futures.combine(Seq(timetableEvents, smallGroupEvents, staffEvents), EventList.combine)
		}
	}

}

trait AutowiringStudentTimetableEventSourceComponent extends StudentTimetableEventSourceComponent {
	val studentTimetableEventSource: StudentTimetableEventSource = (new CombinedStudentTimetableEventSourceComponent
		with SmallGroupEventTimetableEventSourceComponentImpl
		with CombinedHttpTimetableFetchingServiceComponent
		with AutowiringSmallGroupServiceComponent
		with AutowiringUserLookupComponent
		with AutowiringScientiaConfigurationComponent
		with AutowiringCelcatConfigurationComponent
		with AutowiringExamTimetableConfigurationComponent
		with AutowiringSecurityServiceComponent
		with SystemClockComponent
	).studentTimetableEventSource
}
