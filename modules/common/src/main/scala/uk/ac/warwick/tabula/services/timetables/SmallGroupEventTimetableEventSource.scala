package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.data.model.{StaffMember, StudentMember}
import uk.ac.warwick.tabula.services.{UserLookupComponent, SmallGroupServiceComponent}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.groups.{WeekRange, SmallGroupFormat, SmallGroupEvent}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat._
import uk.ac.warwick.tabula.timetables.TimetableEvent

trait SmallGroupEventTimetableEventSourceComponent{
	val studentGroupEventSource: StudentTimetableEventSource
	val staffGroupEventSource: StaffTimetableEventSource
}
trait SmallGroupEventTimetableEventSourceComponentImpl extends SmallGroupEventTimetableEventSourceComponent {
	this: SmallGroupServiceComponent with UserLookupComponent =>

	val studentGroupEventSource: StudentTimetableEventSource = new SmallGroupEventTimetableEventSourceImpl
	val staffGroupEventSource: StaffTimetableEventSource = new SmallGroupEventTimetableEventSourceImpl

	class SmallGroupEventTimetableEventSourceImpl extends StudentTimetableEventSource with StaffTimetableEventSource {

		def eventsFor(student: StudentMember): Seq[TimetableEvent] = {
			val user = userLookup.getUserByUserId(student.userId)
			val studentsGroups = smallGroupService.findSmallGroupsByStudent(user).filter {
				group =>
					!group.groupSet.deleted &&
					group.groupSet.visibleToStudents &&
					!group.events.asScala.isEmpty
			}

			/* Include SGT teaching responsibilities for students (mainly PGR) */
			val allEvents = studentsGroups.flatMap(group => group.events.asScala) ++ smallGroupService.findSmallGroupEventsByTutor(user)
			val autoTimetableEvents = allEvents map smallGroupEventToTimetableEvent

			// TAB-2682 Also include events that the student has been manually added to
			val manualTimetableEvents = smallGroupService.findManuallyAddedAttendance(user.getWarwickId)
				.filter { a =>
					val groupSet = a.occurrence.event.group.groupSet

					!groupSet.deleted && groupSet.visibleToStudents
				}
				.map { a =>
					TimetableEvent(a.occurrence)
				}

			autoTimetableEvents ++ manualTimetableEvents
		}

		def eventsFor(staff: StaffMember): Seq[TimetableEvent] = {
			val user = userLookup.getUserByUserId(staff.userId)
			val allEvents = smallGroupService.findSmallGroupEventsByTutor(user)
			allEvents map smallGroupEventToTimetableEvent
		}

		def smallGroupEventToTimetableEvent(sge: SmallGroupEvent): TimetableEvent = {
			TimetableEvent(sge)
		}
	}

}
