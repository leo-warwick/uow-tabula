package uk.ac.warwick.tabula.commands.timetables

import com.google.common.collect.{Range => GRange}
import org.joda.time._
import uk.ac.warwick.tabula.commands.timetables.ViewMemberEventsCommand.ReturnType
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.helpers.DateRange
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.{EventList, EventOccurrenceList}
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent, TimetableEventType}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, Mockito, TestBase}

import scala.concurrent.Future

class ViewMemberEventsCommandTest extends TestBase with Mockito {

  private trait CommandTestSupport
    extends StudentTimetableEventSourceComponent
      with EventOccurrenceSourceComponent
      with EventOccurrenceServiceComponent {
    val studentTimetableEventSource: StudentTimetableEventSource = mock[StudentTimetableEventSource]
    val eventOccurrenceSource: EventOccurrenceSource = mock[EventOccurrenceSource]
    val eventOccurrenceService: EventOccurrenceService = mock[EventOccurrenceService]
  }

  private trait Fixture {
    val testStudent = new StudentMember
    val user: CurrentUser = mock[CurrentUser]

    val event: TimetableEvent = {
      TimetableEvent("", "", "", "", TimetableEventType.Induction, Nil, DayOfWeek.Monday, LocalTime.now, LocalTime.now, None, None, TimetableEvent.Parent(), None, Nil, Nil, AcademicYear(2012), None, Map())
    }
    val timetableEvents = Seq(event)

    val occurrence = EventOccurrence("", "", "", "", TimetableEventType.Meeting, LocalDateTime.now, LocalDateTime.now, None, None, TimetableEvent.Parent(), None, Nil, None, None)
    val meetingOccurrences = Seq(occurrence)

    val earlierEvent = EventOccurrence("", "", "", "", TimetableEventType.Induction, LocalDateTime.now.minusHours(1), LocalDateTime.now, None, None, TimetableEvent.Parent(), None, Nil, None, None)
    val laterEvent = EventOccurrence("", "", "", "", TimetableEventType.Induction, LocalDateTime.now.plusHours(1), LocalDateTime.now.plusHours(1), None, None, TimetableEvent.Parent(), None, Nil, None, None)
    val eventOccurences = Seq(laterEvent, earlierEvent) // deliberately put them the wrong way round so we can check sorting

    val command = new ViewStudentEventsCommandInternal(testStudent, user) with CommandTestSupport

    val start = LocalDate.now()
    val end = start.plusDays(2)
    command.from = start.toDateTimeAtStartOfDay.getMillis
    command.to = end.toDateTimeAtStartOfDay.getMillis
    command.studentTimetableEventSource.eventsFor(testStudent, user, TimetableEvent.Context.Student) returns Future.successful(EventList.fresh(timetableEvents))
    command.eventOccurrenceSource.occurrencesFor(testStudent, user, TimetableEvent.Context.Student, start, end) returns Future.successful(EventOccurrenceList.fresh(meetingOccurrences))
    command.eventOccurrenceService.fromTimetableEvent(any[TimetableEvent], any[GRange[LocalDate]]) returns eventOccurences
  }

  @Test
  def fetchesEventsFromEventSource(): Unit = {
    new Fixture {
      command.applyInternal()
      verify(command.studentTimetableEventSource, times(1)).eventsFor(testStudent, user, TimetableEvent.Context.Student)
    }
  }

  @Test
  def transformsEventsIntoOccurrences(): Unit = {
    new Fixture {
      command.applyInternal()
      verify(command.eventOccurrenceService, times(1)).fromTimetableEvent(event, DateRange(command.start, command.end))
    }
  }

  @Test
  def sortsOccurencesByDate(): Unit = {
    new Fixture {
      val sortedEvents: ReturnType = command.applyInternal()
      sortedEvents.toOption.map(_.events) should be(Some(Seq(earlierEvent, occurrence, laterEvent)))
    }
  }

  @Test
  def requiresReadTimetablePermissions(): Unit = {
    new Fixture {
      val perms = new ViewMemberEventsPermissions with ViewMemberEventsState {
        val member: StudentMember = testStudent
      }

      val checking: PermissionsChecking = mock[PermissionsChecking]
      perms.permissionsCheck(checking)
      verify(checking, times(1)).PermissionCheck(Permissions.Profiles.Read.Timetable, testStudent)
    }
  }

  @Test
  def mixesCorrectPermissionsIntoCommand(): Unit = {
    new Fixture {
      val composedCommand = ViewMemberEventsCommand(testStudent, user)
      composedCommand should be(anInstanceOf[ViewMemberEventsPermissions])
    }
  }

}
