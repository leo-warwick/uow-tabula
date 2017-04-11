package uk.ac.warwick.tabula.commands.timetables

import org.joda.time.Interval
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.timetables.DepartmentEventsCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.{EventList, EventOccurrenceList}
import uk.ac.warwick.tabula.services.timetables.{AutowiringTermBasedEventOccurrenceServiceComponent, EventOccurrenceServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.JavaConverters._
import scala.collection.mutable

object DepartmentEventsCommand {
	val FilterStudentPermission = ViewMemberEventsCommand.RequiredPermission
	val FilterStaffPermission = ViewMemberEventsCommand.RequiredPermission

	private[timetables] type ReturnType = (EventOccurrenceList, Seq[String])
	type CommandType = Appliable[ReturnType] with DepartmentTimetableEventsRequest

	def apply(
		department: Department,
		academicYear: AcademicYear,
		user: CurrentUser,
		moduleTimetableCommandFactory: ViewModuleTimetableCommandFactory,
		studentMemberEventsCommandFactory: ViewStudentMemberEventsCommandFactory,
		staffMemberEventsCommandFactory: ViewStaffMemberEventsCommandFactory
	) =	new DepartmentEventsCommandInternal(
		department,
		user,
		academicYear,
		moduleTimetableCommandFactory,
		studentMemberEventsCommandFactory,
		staffMemberEventsCommandFactory
	) with ComposableCommand[ReturnType]
		with AutowiringTermBasedEventOccurrenceServiceComponent
		with AutowiringProfileServiceComponent
		with AutowiringSecurityServiceComponent
		with AutowiringModuleAndDepartmentServiceComponent
		with AutowiringRelationshipServiceComponent
		with ReadOnly with Unaudited
		with DepartmentTimetableEventsPermissions
		with DepartmentTimetableEventsState
		with DepartmentTimetableEventsRequest

	def draft(
		department: Department,
		academicYear: AcademicYear,
		user: CurrentUser,
		moduleTimetableCommandFactory: ViewModuleTimetableCommandFactory,
		studentMemberEventsCommandFactory: ViewStudentMemberEventsCommandFactory,
		staffMemberEventsCommandFactory: ViewStaffMemberEventsCommandFactory
	) =	new DepartmentEventsCommandInternal(
		department,
		user,
		academicYear,
		moduleTimetableCommandFactory,
		studentMemberEventsCommandFactory,
		staffMemberEventsCommandFactory
	) with ComposableCommand[ReturnType]
		with AutowiringTermBasedEventOccurrenceServiceComponent
		with AutowiringProfileServiceComponent
		with AutowiringSecurityServiceComponent
		with AutowiringModuleAndDepartmentServiceComponent
		with AutowiringRelationshipServiceComponent
		with ReadOnly with Unaudited
		with DraftDepartmentTimetableEventsPermissions
		with DepartmentTimetableEventsState
		with DepartmentTimetableEventsRequest

}

class DepartmentEventsCommandInternal(
	val department: Department,
	val user: CurrentUser,
	academicYear: AcademicYear,
	moduleTimetableCommandFactory: ViewModuleTimetableCommandFactory,
	studentMemberEventsCommandFactory: ViewStudentMemberEventsCommandFactory,
	staffMemberEventsCommandFactory: ViewStaffMemberEventsCommandFactory
)	extends CommandInternal[ReturnType] with GetsModuleEvents {
	self: DepartmentTimetableEventsRequest
		with EventOccurrenceServiceComponent
		with SecurityServiceComponent
		with ModuleAndDepartmentServiceComponent
		with DepartmentTimetableEventsState =>

	override def applyInternal(): (EventOccurrenceList, Seq[String]) = {
		val errors: mutable.Buffer[String] = mutable.Buffer()

		val moduleEvents = getModuleEvents(
			academicYear,
			moduleTimetableCommandFactory,
			errors
		)

		errors.appendAll(students.asScala.filter(student => !studentMembers.exists(_.universityId == student)).map(student =>
			s"Could not find a student with a University ID of $student"
		))
		val studentCommands = studentMembers.map(student => student -> studentMemberEventsCommandFactory.apply(student))
		val studentEvents = EventOccurrenceList.combine(studentCommands.map { case (student, cmd) =>
			if (securityService.can(user, FilterStudentPermission, student)) {
				cmd.from = from
				cmd.to = to
				cmd.apply().getOrElse(EventOccurrenceList.empty)
			} else {
				errors.append(s"You do not have permission to view the timetable of ${student.fullName.getOrElse("")} (${student.universityId})")
				EventOccurrenceList.fresh(Nil)
			}
		}).map(_.distinct)

		errors.appendAll(staff.asScala.filter(staffMember => !staffMembers.exists(_.universityId == staffMember)).map(staffMember =>
			s"Could not find a student with a University ID of $staffMember"
		))
		val staffCommands = staffMembers.map(staffMember => staffMember -> staffMemberEventsCommandFactory.apply(staffMember))
		val staffEvents = EventOccurrenceList.combine(staffCommands.map { case (staffMember, cmd) =>
			if (securityService.can(user, FilterStaffPermission, staffMember)) {
				cmd.from = from
				cmd.to = to
				cmd.apply().getOrElse(EventOccurrenceList.empty)
			} else {
				errors.append(s"You do not have permission to view the timetable of ${staffMember.fullName.getOrElse("")} (${staffMember.universityId})")
				EventOccurrenceList.fresh(Nil)
			}
		}).map(_.distinct)

		val occurrences = eventsToOccurrences(moduleEvents) ++ studentEvents ++ staffEvents

		val filtered =
			if (eventTypes.asScala.isEmpty) occurrences
			else occurrences.filter { o => eventTypes.contains(o.eventType) }

		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		(filtered.map(_.sortBy(_.start)), errors)
	}

	private def eventsToOccurrences(events: EventList) = EventOccurrenceList(
		events.events.flatMap(eventOccurrenceService.fromTimetableEvent(_, new Interval(start.toDateTimeAtStartOfDay, end.toDateTimeAtStartOfDay))),
		events.lastUpdated
	)

}
