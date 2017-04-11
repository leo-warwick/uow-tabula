package uk.ac.warwick.tabula.commands.timetables

import org.joda.time.{DateTime, Interval}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.timetables.ViewMemberEventsCommand.ReturnType
import uk.ac.warwick.tabula.data.model.{Member, StaffMember, StudentMember}
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.helpers.{Futures, Logging}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.{EventList, EventOccurrenceList}
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent}
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
	val RequiredPermission = Permissions.Profiles.Read.Timetable

	def apply(member: Member, currentUser: CurrentUser): TimetableCommand = member match {
		case student: StudentMember =>
			new ViewStudentEventsCommandInternal(student, currentUser)
				with ComposableCommand[ReturnType]
				with ViewMemberEventsPermissions
				with ViewMemberEventsValidation
				with Unaudited with ReadOnly
				with AutowiringStudentTimetableEventSourceComponent
				with AutowiringScheduledMeetingEventSourceComponent
				with AutowiringTermServiceComponent
				with AutowiringTermBasedEventOccurrenceServiceComponent

		case staff: StaffMember =>
			new ViewStaffEventsCommandInternal(staff, currentUser)
				with ComposableCommand[ReturnType]
				with ViewMemberEventsPermissions
				with ViewMemberEventsValidation
				with Unaudited with ReadOnly
				with AutowiringStaffTimetableEventSourceComponent
				with AutowiringScheduledMeetingEventSourceComponent
				with AutowiringTermServiceComponent
				with AutowiringTermBasedEventOccurrenceServiceComponent

		case _ =>
			logger.error(s"Don't know how to render timetables for non-student or non-staff users (${member.universityId}, ${member.userType})")
			throw new ItemNotFoundException
	}

	def public(member: Member, currentUser: CurrentUser): TimetableCommand = member match {
		case student: StudentMember =>
			new ViewStudentEventsCommandInternal(student, currentUser)
				with ComposableCommand[ReturnType]
				with PubliclyVisiblePermissions
				with ViewMemberEventsValidation
				with Unaudited with ReadOnly
				with AutowiringStudentTimetableEventSourceComponent
				with AutowiringScheduledMeetingEventSourceComponent
				with AutowiringTermServiceComponent
				with AutowiringTermBasedEventOccurrenceServiceComponent

		case staff: StaffMember =>
			new ViewStaffEventsCommandInternal(staff, currentUser)
				with ComposableCommand[ReturnType]
				with PubliclyVisiblePermissions
				with ViewMemberEventsValidation
				with Unaudited with ReadOnly
				with AutowiringStaffTimetableEventSourceComponent
				with AutowiringScheduledMeetingEventSourceComponent
				with AutowiringTermServiceComponent
				with AutowiringTermBasedEventOccurrenceServiceComponent

		case _ =>
			logger.error(s"Don't know how to render timetables for non-student or non-staff users (${member.universityId}, ${member.userType})")
			throw new ItemNotFoundException
	}
}

trait MemberTimetableCommand {
	self: ViewMemberEventsRequest with TermServiceComponent with EventOccurrenceServiceComponent =>

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

	private def createDateRange(): Interval = {
		val startDate: DateTime =
			Option(start).map(_.toDateTimeAtStartOfDay).getOrElse(termService.getTermFromDate(academicYear.dateInTermOne).getStartDate)

		val endDate: DateTime =
			Option(end).map(_.toDateTimeAtStartOfDay).getOrElse(termService.getTermFromDate((academicYear + 1).dateInTermOne).getStartDate.minusDays(1))

		new Interval(startDate, endDate)
	}

	protected def sorted(result: EventOccurrenceList): EventOccurrenceList = {
		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		result.map(_.sortBy(_.start))
	}

}

abstract class ViewStudentEventsCommandInternal(val member: StudentMember, currentUser: CurrentUser)
	extends CommandInternal[ReturnType]
		with ViewMemberEventsRequest with MemberTimetableCommand {

	self: StudentTimetableEventSourceComponent with ScheduledMeetingEventSourceComponent with TermServiceComponent with EventOccurrenceServiceComponent =>

	def applyInternal(): ReturnType = {
		val timetableOccurrences =
			studentTimetableEventSource.eventsFor(member, currentUser, TimetableEvent.Context.Student)
				.map(eventsToOccurrences)

		val meetingOccurrences = scheduledMeetingEventSource.occurrencesFor(member, currentUser, TimetableEvent.Context.Student)
			.map(_.filterNot { event =>
				event.end.toLocalDate.isBefore(start) || event.start.toLocalDate.isAfter(end)
			})

		Try(Await.result(
			Futures.combine(Seq(timetableOccurrences, meetingOccurrences), EventOccurrenceList.combine), ViewMemberEventsCommand.Timeout
		)).map(sorted)
	}

}

abstract class ViewStaffEventsCommandInternal(val member: StaffMember, currentUser: CurrentUser)
	extends CommandInternal[ReturnType]
		with ViewMemberEventsRequest with MemberTimetableCommand {

	self: StaffTimetableEventSourceComponent with ScheduledMeetingEventSourceComponent with TermServiceComponent with EventOccurrenceServiceComponent =>

	def applyInternal(): ReturnType = {
		val timetableOccurrences =
			staffTimetableEventSource.eventsFor(member, currentUser, TimetableEvent.Context.Staff)
				.map(eventsToOccurrences)

		val meetingOccurrences =
			scheduledMeetingEventSource.occurrencesFor(member, currentUser, TimetableEvent.Context.Staff)

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

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(ViewMemberEventsCommand.RequiredPermission, mandatory(member))
	}
}

trait ViewMemberEventsValidation extends SelfValidating {
	self: ViewMemberEventsRequest =>

	override def validate(errors: Errors) {
		// Must have either an academic year or a start and an end
		if (academicYear == null && (start == null || end == null)) {
			errors.rejectValue("academicYear", "NotEmpty")
		}
	}
}

trait ViewStaffMemberEventsCommandFactory {
	def apply(staffMember: StaffMember): Appliable[ReturnType] with ViewMemberEventsRequest
}

class ViewStaffMemberEventsCommandFactoryImpl(currentUser: CurrentUser)
	extends ViewStaffMemberEventsCommandFactory {

	def apply(staffMember: StaffMember) =
		ViewMemberEventsCommand(
			staffMember,
			currentUser
		)
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
