package uk.ac.warwick.tabula.commands.timetables

import java.util.concurrent.TimeoutException

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.timetables.ViewModuleTimetableCommand.{CommandType, ReturnType}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.concurrent.Await
import scala.util.Try

object ViewModuleTimetableCommand {
	private[timetables] type ReturnType = Try[EventList]
	type CommandType = Appliable[ReturnType] with ViewModuleTimetableRequest

	def apply(module: Module, user: CurrentUser): CommandType =
		new ViewModuleTimetableCommandInternal(module, user)
			with ComposableCommand[ReturnType]
			with ViewModuleTimetablePermissions
			with ViewModuleTimetableValidation
			with ViewModuleTimetableDescription with ReadOnly
			with AutowiringScientiaConfigurationComponent
			with SystemClockComponent
			with AutowiringModuleTimetableEventSourceComponent

	// Re-usable service
	def apply(module: Module, user: CurrentUser, source: ModuleTimetableEventSource): CommandType =
		new ViewModuleTimetableCommandInternal(module, user)
			with ComposableCommand[ReturnType]
			with ViewModuleTimetablePermissions
			with ViewModuleTimetableValidation
			with Unaudited with ReadOnly
			with ModuleTimetableEventSourceComponent {
			val moduleTimetableEventSource: ModuleTimetableEventSource = source
		}
}

trait ViewModuleTimetableCommandFactory {
	def apply(module: Module, user: CurrentUser): CommandType
}
class ViewModuleTimetableCommandFactoryImpl(source: ModuleTimetableEventSource) extends ViewModuleTimetableCommandFactory {
	def apply(module: Module, user: CurrentUser) = ViewModuleTimetableCommand(module, user, source)
}

abstract class ViewModuleTimetableCommandInternal(val module: Module, user: CurrentUser)
	extends CommandInternal[ReturnType]
		with ViewModuleTimetableRequest {

	self: ModuleTimetableEventSourceComponent =>

	def applyInternal(): ReturnType = {
		Try(Await.result(moduleTimetableEventSource.eventsFor(module, academicYear, user, sourcesToShow), ViewModuleEventsCommand.Timeout))
			.recover { case _: TimeoutException | _: TimetableEmptyException => EventList.empty }
			.map { events => events.filter { event => event.year == academicYear }}
	}
}

// State - unmodifiable pre-requisites
trait ViewModuleTimetableState {
	val module: Module
}

// Request parameters
trait ViewModuleTimetableRequest extends ViewModuleTimetableState
	with CurrentSITSAcademicYear {
	var showTimetableEvents: Boolean = true
	var showSmallGroupEvents: Boolean = false
	def sourcesToShow: Seq[TimetableEventSource] = if (showTimetableEvents == showSmallGroupEvents) {
		Seq(TimetableEventSource.Sciencia, TimetableEventSource.SmallGroups)
	} else if (showTimetableEvents) {
		Seq(TimetableEventSource.Sciencia)
	} else {
		Seq(TimetableEventSource.SmallGroups)
	}
}

trait ViewModuleTimetablePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewModuleTimetableState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Module.ViewTimetable, mandatory(module))
	}
}

trait ViewModuleTimetableValidation extends SelfValidating {
	self: ViewModuleTimetableRequest =>

	override def validate(errors: Errors) {
		if (academicYear == null) {
			errors.rejectValue("academicYear", "NotEmpty")
		}
	}
}

/**
	* This won't be audited, but it is included in things like stopwatch task names
	*/
trait ViewModuleTimetableDescription extends Describable[ReturnType] with Unaudited {
	self: ViewModuleTimetableRequest =>

	override def describe(d: Description): Unit = d.module(module).properties("academicYear" -> academicYear.toString)
}