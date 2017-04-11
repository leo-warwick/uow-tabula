package uk.ac.warwick.tabula.commands.timetables

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.timetables.DepartmentTimetableCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.services.timetables.{AutowiringTermBasedEventOccurrenceServiceComponent, EventOccurrenceServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.JavaConverters._
import scala.collection.mutable

object DepartmentTimetableCommand {
	val FilterStudentPermission = ViewMemberTimetableCommand.RequiredPermission
	val FilterStaffPermission = ViewMemberTimetableCommand.RequiredPermission

	private[timetables] type ReturnType = (EventList, Seq[String])
	type CommandType = Appliable[ReturnType] with DepartmentTimetableEventsRequest

	def apply(
		department: Department,
		academicYear: AcademicYear,
		user: CurrentUser,
		moduleTimetableCommandFactory: ViewModuleTimetableCommandFactory,
		studentMemberTimetableCommandFactory: ViewStudentMemberTimetableCommandFactory,
		staffMemberTimetableCommandFactory: ViewStaffMemberTimetableCommandFactory
	) =	new DepartmentTimetableCommandInternal(
		department,
		user,
		academicYear,
		moduleTimetableCommandFactory,
		studentMemberTimetableCommandFactory,
		staffMemberTimetableCommandFactory
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
		studentMemberTimetableCommandFactory: ViewStudentMemberTimetableCommandFactory,
		staffMemberTimetableCommandFactory: ViewStaffMemberTimetableCommandFactory
	) =	new DepartmentTimetableCommandInternal(
		department,
		user,
		academicYear,
		moduleTimetableCommandFactory,
		studentMemberTimetableCommandFactory,
		staffMemberTimetableCommandFactory
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

class DepartmentTimetableCommandInternal(
	val department: Department,
	val user: CurrentUser,
	academicYear: AcademicYear,
	moduleTimetableCommandFactory: ViewModuleTimetableCommandFactory,
	studentMemberTimetableCommandFactory: ViewStudentMemberTimetableCommandFactory,
	staffMemberTimetableCommandFactory: ViewStaffMemberTimetableCommandFactory
)	extends CommandInternal[ReturnType] with GetsModuleEvents {
	self: DepartmentTimetableEventsRequest
		with EventOccurrenceServiceComponent
		with SecurityServiceComponent
		with ModuleAndDepartmentServiceComponent
		with DepartmentTimetableEventsState =>

	override def applyInternal(): (EventList, Seq[String]) = {
		val errors: mutable.Buffer[String] = mutable.Buffer()

		val moduleEvents = getModuleEvents(
			academicYear,
			moduleTimetableCommandFactory,
			errors
		)

		errors.appendAll(students.asScala.filter(student => !studentMembers.exists(_.universityId == student)).map(student =>
			s"Could not find a student with a University ID of $student"
		))
		val studentCommands = studentMembers.map(student => student -> studentMemberTimetableCommandFactory.apply(student))
		val studentEvents = EventList.combine(studentCommands.map { case (student, cmd) =>
			if (securityService.can(user, FilterStudentPermission, student)) {
				cmd.academicYear = academicYear
				cmd.apply().getOrElse(EventList.empty)
			} else {
				errors.append(s"You do not have permission to view the timetable of ${student.fullName.getOrElse("")} (${student.universityId})")
				EventList.fresh(Nil)
			}
		}).map(_.distinct)

		errors.appendAll(staff.asScala.filter(staffMember => !staffMembers.exists(_.universityId == staffMember)).map(staffMember =>
			s"Could not find a student with a University ID of $staffMember"
		))
		val staffCommands = staffMembers.map(staffMember => staffMember -> staffMemberTimetableCommandFactory.apply(staffMember))
		val staffEvents = EventList.combine(staffCommands.map { case (staffMember, cmd) =>
			if (securityService.can(user, FilterStaffPermission, staffMember)) {
				cmd.academicYear = academicYear
				cmd.apply().getOrElse(EventList.empty)
			} else {
				errors.append(s"You do not have permission to view the timetable of ${staffMember.fullName.getOrElse("")} (${staffMember.universityId})")
				EventList.fresh(Nil)
			}
		}).map(_.distinct)

		val allEvents = EventList.combine(Seq(moduleEvents, studentEvents, staffEvents))

		val filtered =
			if (eventTypes.asScala.isEmpty) allEvents
			else allEvents.filter { o => eventTypes.contains(o.eventType) }

		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		(filtered, errors)
	}

}
