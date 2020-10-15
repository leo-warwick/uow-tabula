package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, WeekRange}
import uk.ac.warwick.tabula.data.model.{Module, StaffMember, StudentMember}
import uk.ac.warwick.tabula.helpers.ExecutionContexts.timetable
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.timetables.TimetableEvent
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.userlookup.User

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

trait SmallGroupEventTimetableEventSourceComponent {
  val studentGroupEventSource: StudentTimetableEventSource
  val staffGroupEventSource: StaffTimetableEventSource
  val moduleGroupEventSource: ModuleTimetableEventSource
}

trait SmallGroupEventTimetableEventSourceComponentImpl extends SmallGroupEventTimetableEventSourceComponent {
  self: SmallGroupServiceComponent with UserLookupComponent with SecurityServiceComponent =>

  val studentGroupEventSource: StudentTimetableEventSource = new StudentSmallGroupEventTimetableEventSource
  val staffGroupEventSource: StaffTimetableEventSource = new StaffSmallGroupEventTimetableEventSource
  val moduleGroupEventSource: ModuleTimetableEventSource = new ModuleSmallGroupEventTimetableEventSource

  class StudentSmallGroupEventTimetableEventSource extends StudentTimetableEventSource with SmallGroupEventTimetableEventSource {

    def eventsFor(student: StudentMember, currentUser: CurrentUser, context: TimetableEvent.Context): Future[EventList] =
      Future(eventsFor(userLookup.getUserByUserId(student.userId), currentUser))

  }

  class StaffSmallGroupEventTimetableEventSource extends StaffTimetableEventSource with SmallGroupEventTimetableEventSource {

    def eventsFor(staff: StaffMember, currentUser: CurrentUser, context: TimetableEvent.Context): Future[EventList] =
      Future(eventsFor(userLookup.getUserByUserId(staff.userId), currentUser))

  }

  trait SmallGroupEventTimetableEventSource extends TaskBenchmarking {

    protected def eventsFor(user: User, currentUser: CurrentUser): EventList = benchmarkTask("SGT events") {
      /* Include SGT teaching responsibilities for students (mainly PGR) and students for staff (e.g. Chemistry) */
      val allEvents = studentEvents(user, currentUser) ++ tutorEvents(user, currentUser)

      // TAB-4643 - Don't show event if it has been replaced
      val attendanceMap = smallGroupService.findStudentAttendanceInEvents(user.getWarwickId, allEvents).groupBy(_.occurrence.event)

      val autoTimetableEvents: Seq[TimetableEvent] = allEvents.map { event =>
        attendanceMap.get(event) match {
          case Some(attendanceList) =>
            val attendanceForEvent = attendanceList.groupBy(_.occurrence.week).view.mapValues(_.head.state).toMap
            val weeksToRemove = attendanceList.filter(a => a.state == AttendanceState.MissedAuthorised && !a.replacedBy.isEmpty).map(_.occurrence.week)
            TimetableEvent(event, attendanceForEvent, withoutWeeks = weeksToRemove)
          case None =>
            TimetableEvent(event, Map[WeekRange.Week, AttendanceState]())
        }
      }

      // TAB-2682 Also include events that the student has been manually added to
      val manualTimetableEvents = smallGroupService.findManuallyAddedAttendance(user.getWarwickId)
        .filter { a =>
          val groupSet = a.occurrence.event.group.groupSet

          !groupSet.deleted && groupSet.visibleToStudents
        }
        .map { a => TimetableEvent(a.occurrence, Option(a.state).getOrElse(AttendanceState.NotRecorded)) }

      EventList.fresh(autoTimetableEvents ++ manualTimetableEvents)
    }

    private def studentEvents(user: User, currentUser: CurrentUser) = benchmarkTask("studentEvents") {
      smallGroupService.findSmallGroupsByStudent(user).filter {
        group =>
          !group.groupSet.deleted &&
            group.events.nonEmpty &&
            group.groupSet.academicYear == AcademicYear.now() &&
            (
              // The set is visible to students; OR
              group.groupSet.visibleToStudents ||

                // I have permission to view the membership of the set anyway
                securityService.can(currentUser, Permissions.SmallGroups.ReadMembership, group)
              )
      }.flatMap(group => group.events).filterNot(_.isUnscheduled)
    }

    private def tutorEvents(user: User, currentUser: CurrentUser) = benchmarkTask("tutorEvents") {
      smallGroupService.findSmallGroupEventsByTutor(user).filter {
        event =>
          !event.group.groupSet.deleted &&
          event.group.groupSet.academicYear == AcademicYear.now() &&
            !event.isUnscheduled &&
            (
              // The set is visible to tutors; OR
              event.group.groupSet.releasedToTutors ||

                // I have permission to view the membership of the set anyway
                securityService.can(currentUser, Permissions.SmallGroups.ReadMembership, event)
              )
      }
    }

  }

  class ModuleSmallGroupEventTimetableEventSource extends ModuleTimetableEventSource with TaskBenchmarking {
    override def eventsFor(module: Module, academicYear: AcademicYear, currentUser: CurrentUser, sources: Seq[TimetableEventSource]): Future[EventList] = benchmarkTask("moduleEvents") {
      Future.successful {
        if (sources.contains(TimetableEventSource.SmallGroups)) {
          val sets = smallGroupService.getSmallGroupSets(module, academicYear).filterNot(_.archived)
          // Try and do as few permission checks as possible
          val filteredEvents: Seq[SmallGroupEvent] = {
            if (securityService.can(currentUser, Permissions.SmallGroups.Read, module)) {
              sets.flatMap(_.groups.asScala.flatMap(_.events))
            } else {
              sets.flatMap(_.groups.asScala).flatMap(group => if (securityService.can(currentUser, Permissions.SmallGroups.Read, group)) {
                group.events
              } else {
                group.events.filter(event => securityService.can(currentUser, Permissions.SmallGroups.Read, event))
              })
            }
          }.filterNot(_.isUnscheduled)
          EventList.fresh(filteredEvents.map(event => TimetableEvent(event, Map[WeekRange.Week, AttendanceState]())))
        } else {
          EventList.empty
        }
      }
    }
  }

}
