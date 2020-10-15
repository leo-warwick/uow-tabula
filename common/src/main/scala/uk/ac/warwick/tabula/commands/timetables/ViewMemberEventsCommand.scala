package uk.ac.warwick.tabula.commands.timetables

import com.google.common.collect.{Range => GRange}
import org.joda.time.LocalDate
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.timetables.ViewMemberEventsCommand.ReturnType
import uk.ac.warwick.tabula.data.model.{Member, StaffMember, StudentMember}
import uk.ac.warwick.tabula.helpers.ExecutionContexts.timetable
import uk.ac.warwick.tabula.helpers.{DateRange, Futures, Logging}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.{EventList, EventOccurrenceList}
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, PubliclyVisiblePermissions, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.TimetableEvent
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, ItemNotFoundException}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

object ViewMemberEventsCommand extends Logging {
  val Timeout: FiniteDuration = 15.seconds

  private[timetables] type ReturnType = Try[EventOccurrenceList]
  type TimetableCommand = Appliable[ReturnType] with ViewMemberEventsRequest with SelfValidating
  val RequiredPermission: Permission = Permissions.Profiles.Read.Timetable

  def apply(member: Member, currentUser: CurrentUser): TimetableCommand = member match {
    case student: StudentMember =>
      new ViewStudentEventsCommandInternal(student, currentUser)
        with ComposableCommand[ReturnType]
        with ViewMemberEventsPermissions
        with ViewMemberEventsValidation
        with Unaudited with ReadOnly
        with AutowiringStudentTimetableEventSourceComponent
        with AutowiringEventOccurrenceSourceComponent
        with AutowiringTermBasedEventOccurrenceServiceComponent

    case staff: StaffMember =>
      new ViewStaffEventsCommandInternal(staff, currentUser)
        with ComposableCommand[ReturnType]
        with ViewMemberEventsPermissions
        with ViewMemberEventsValidation
        with Unaudited with ReadOnly
        with AutowiringStaffTimetableEventSourceComponent
        with AutowiringEventOccurrenceSourceComponent
        with AutowiringTermBasedEventOccurrenceServiceComponent

    case _ =>
      logger.error(s"Don't know how to render timetables for non-student or non-staff users (${member.universityId}, ${member.userType})")
      throw new ItemNotFoundException
  }

  // Re-usable service
  def apply(staff: StaffMember, currentUser: CurrentUser, source: StaffTimetableEventSource): TimetableCommand =
    new ViewStaffEventsCommandInternal(staff, currentUser)
      with ComposableCommand[ReturnType]
      with ViewMemberEventsPermissions
      with ViewMemberEventsValidation
      with Unaudited with ReadOnly
      with AutowiringEventOccurrenceSourceComponent
      with AutowiringTermBasedEventOccurrenceServiceComponent
      with StaffTimetableEventSourceComponent {
      val staffTimetableEventSource: StaffTimetableEventSource = source
    }

  def public(member: Member, currentUser: CurrentUser): TimetableCommand = member match {
    case student: StudentMember =>
      new ViewStudentEventsCommandInternal(student, currentUser)
        with ComposableCommand[ReturnType]
        with PubliclyVisiblePermissions
        with ViewMemberEventsValidation
        with Unaudited with ReadOnly
        with AutowiringStudentTimetableEventSourceComponent
        with AutowiringEventOccurrenceSourceComponent
        with AutowiringTermBasedEventOccurrenceServiceComponent

    case staff: StaffMember =>
      new ViewStaffEventsCommandInternal(staff, currentUser)
        with ComposableCommand[ReturnType]
        with PubliclyVisiblePermissions
        with ViewMemberEventsValidation
        with Unaudited with ReadOnly
        with AutowiringStaffTimetableEventSourceComponent
        with AutowiringEventOccurrenceSourceComponent
        with AutowiringTermBasedEventOccurrenceServiceComponent

    case _ =>
      logger.error(s"Don't know how to render timetables for non-student or non-staff users (${member.universityId}, ${member.userType})")
      throw new ItemNotFoundException
  }
}

trait MemberTimetableCommand {
  self: ViewMemberEventsRequest with EventOccurrenceServiceComponent =>

  protected def eventsToOccurrences(events: EventList): EventOccurrenceList = {
    val dateRange = createDateRange()
    val lastUpdated = events.lastUpdated

    if (academicYear != null) {
      EventOccurrenceList(events.events.filter { event => event.year == academicYear }
        .flatMap(eventOccurrenceService.fromTimetableEvent(_, dateRange)), lastUpdated)
    } else {
      EventOccurrenceList(events.events.flatMap(eventOccurrenceService.fromTimetableEvent(_, dateRange)), lastUpdated)
    }
  }

  private def createDateRange(): GRange[LocalDate] = {
    val startDate: LocalDate =
      Option(start).getOrElse(academicYear.firstDay)

    val endDate: LocalDate =
      Option(end).getOrElse(academicYear.lastDay)

    DateRange(startDate, endDate)
  }

  protected def sorted(result: EventOccurrenceList): EventOccurrenceList = {
    import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
    result.map(_.sortBy(_.start))
  }

}

abstract class ViewStudentEventsCommandInternal(val member: StudentMember, currentUser: CurrentUser)
  extends CommandInternal[ReturnType]
    with ViewMemberEventsRequest with MemberTimetableCommand {

  self: StudentTimetableEventSourceComponent with EventOccurrenceSourceComponent with EventOccurrenceServiceComponent =>

  def applyInternal(): ReturnType = {
    val timetableOccurrences =
      studentTimetableEventSource.eventsFor(member, currentUser, TimetableEvent.Context.Student)
        .map(eventsToOccurrences)

    val meetingOccurrences =
      eventOccurrenceSource.occurrencesFor(member, currentUser, TimetableEvent.Context.Student, start, end)

    Try(Await.result(
      Futures.combine(Seq(timetableOccurrences, meetingOccurrences), EventOccurrenceList.combine), ViewMemberEventsCommand.Timeout
    )).map(sorted)
  }


}

abstract class ViewStaffEventsCommandInternal(val member: StaffMember, currentUser: CurrentUser)
  extends CommandInternal[ReturnType]
    with ViewMemberEventsRequest with MemberTimetableCommand with TaskBenchmarking {

  self: StaffTimetableEventSourceComponent with EventOccurrenceSourceComponent with EventOccurrenceServiceComponent =>

  def applyInternal(): ReturnType = {
    val timetableOccurrences = benchmark("timetableOccurrences") {
      staffTimetableEventSource.eventsFor(member, currentUser, TimetableEvent.Context.Staff)
        .map(eventsToOccurrences)
    }

    val meetingOccurrences = benchmark("meetingOccurrences") {
      eventOccurrenceSource.occurrencesFor(member, currentUser, TimetableEvent.Context.Staff, start, end)
    }

    Try(Await.result(
      Futures.combine(Seq(timetableOccurrences, meetingOccurrences), EventOccurrenceList.combine), ViewMemberEventsCommand.Timeout
    )).map(sorted)
  }

}

// State - unmodifiable pre-requisites
trait ViewMemberEventsState {
  val member: Member
}

// Request parameters
trait ViewMemberEventsRequest extends ViewMemberEventsState with TimetableEventsRequest {
  var academicYear: AcademicYear = _
}

trait ViewMemberEventsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ViewMemberEventsState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(ViewMemberEventsCommand.RequiredPermission, mandatory(member))
  }
}

trait ViewMemberEventsValidation extends SelfValidating {
  self: ViewMemberEventsRequest =>

  override def validate(errors: Errors): Unit = {
    // Must have either an academic year or a start and an end
    if (academicYear == null && (start == null || end == null)) {
      errors.rejectValue("academicYear", "NotEmpty")
    }
  }
}

trait ViewStaffMemberEventsCommandFactory {
  def apply(staffMember: StaffMember): Appliable[ReturnType] with ViewMemberEventsRequest
}

class ViewStaffMemberEventsCommandFactoryImpl(currentUser: CurrentUser, source: Option[StaffTimetableEventSource] = None)
  extends ViewStaffMemberEventsCommandFactory {

  def this(currentUser: CurrentUser, source: StaffTimetableEventSource) = {
    this(currentUser, Some(source))
  }

  def apply(staffMember: StaffMember) = source match {
    case Some(staffTimetableEventSource) =>
      ViewMemberEventsCommand(
        staffMember,
        currentUser,
        staffTimetableEventSource
      )

    case _ =>
      ViewMemberEventsCommand(
        staffMember,
        currentUser
      )
  }
}

trait ViewStudentMemberEventsCommandFactory {
  def apply(student: StudentMember): Appliable[ReturnType] with ViewMemberEventsRequest
}

class ViewStudentMemberEventsCommandFactoryImpl(currentUser: CurrentUser)
  extends ViewStudentMemberEventsCommandFactory {

  def apply(student: StudentMember) =
    ViewMemberEventsCommand(
      student,
      currentUser
    )
}
