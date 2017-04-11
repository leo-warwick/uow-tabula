package uk.ac.warwick.tabula.commands.timetables

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.timetables.ViewMemberTimetableCommand.ReturnType
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{StaffMember, StudentMember, Member}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.TimetableEvent

import scala.concurrent.Await
import scala.util.Try

object ViewMemberTimetableCommand extends Logging {
	private[timetables] type ReturnType = Try[EventList]
	type TimetableCommand = Appliable[ReturnType] with ViewMemberTimetableRequest with SelfValidating
	val RequiredPermission = Permissions.Profiles.Read.Timetable

	def apply(member: Member, currentUser: CurrentUser): TimetableCommand = member match {
		case student: StudentMember =>
			new ViewStudentTimetableCommandInternal(student, currentUser)
				with ComposableCommand[ReturnType]
				with ViewMemberTimetablePermissions
				with ViewMemberTimetableValidation
				with Unaudited with ReadOnly
				with AutowiringStudentTimetableEventSourceComponent

		case staff: StaffMember =>
			new ViewStaffTimetableCommandInternal(staff, currentUser)
				with ComposableCommand[ReturnType]
				with ViewMemberTimetablePermissions
				with ViewMemberTimetableValidation
				with Unaudited with ReadOnly
				with AutowiringStaffTimetableEventSourceComponent

		case _ =>
			logger.error(s"Don't know how to render timetables for non-student or non-staff users (${member.universityId}, ${member.userType})")
			throw new ItemNotFoundException
	}
}

abstract class ViewStudentTimetableCommandInternal(val member: StudentMember, currentUser: CurrentUser)
	extends CommandInternal[ReturnType]
		with ViewMemberTimetableRequest {

	self: StudentTimetableEventSourceComponent =>

	def applyInternal(): Try[EventList] = {
		Try(Await.result(studentTimetableEventSource.eventsFor(member, currentUser, TimetableEvent.Context.Student), ViewMemberEventsCommand.Timeout))
			.map { events => events.filter { event => event.year == academicYear }}
	}

}

abstract class ViewStaffTimetableCommandInternal(val member: StaffMember, currentUser: CurrentUser)
	extends CommandInternal[ReturnType]
	with ViewMemberTimetableRequest {

	self: StaffTimetableEventSourceComponent =>

	def applyInternal(): ReturnType = {
		Try(Await.result(staffTimetableEventSource.eventsFor(member, currentUser, TimetableEvent.Context.Staff), ViewMemberEventsCommand.Timeout))
			.map { events => events.filter { event => event.year == academicYear }}
	}

}

// State - unmodifiable pre-requisites
trait ViewMemberTimetableState {
	val member: Member
}

// Request parameters
trait ViewMemberTimetableRequest extends ViewMemberTimetableState
	with CurrentSITSAcademicYear

trait ViewMemberTimetablePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewMemberTimetableState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(ViewMemberTimetableCommand.RequiredPermission, mandatory(member))
	}
}

trait ViewMemberTimetableValidation extends SelfValidating {
	self: ViewMemberTimetableRequest =>

	override def validate(errors: Errors) {
		if (academicYear == null) {
			errors.rejectValue("academicYear", "NotEmpty")
		}
	}
}

trait ViewStaffMemberTimetableCommandFactory {
	def apply(staffMember: StaffMember): Appliable[ReturnType] with CurrentSITSAcademicYear
}

class ViewStaffMemberTimetableCommandFactoryImpl(currentUser: CurrentUser)
	extends ViewStaffMemberTimetableCommandFactory {

	def apply(staffMember: StaffMember) =
		ViewMemberTimetableCommand(
			staffMember,
			currentUser
		)
}

trait ViewStudentMemberTimetableCommandFactory {
	def apply(student: StudentMember): Appliable[ReturnType] with CurrentSITSAcademicYear
}

class ViewStudentMemberTimetableCommandFactoryImpl(currentUser: CurrentUser)
	extends ViewStudentMemberTimetableCommandFactory {

	def apply(student: StudentMember) =
		ViewMemberTimetableCommand(
			student,
			currentUser
		)
}