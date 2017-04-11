package uk.ac.warwick.tabula.commands.timetables

import java.util.concurrent.TimeoutException

import org.joda.time.{Interval, LocalDate}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.timetables.ViewModuleEventsCommand.{CommandType, ReturnType}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventOccurrenceList
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

object ViewModuleEventsCommand {
	val Timeout: FiniteDuration = 15.seconds

	private[timetables] type ReturnType = Try[EventOccurrenceList]
	type CommandType = Appliable[ReturnType]

	def apply(module: Module): CommandType =
		new ViewModuleEventsCommandInternal(module)
			with ComposableCommand[ReturnType]
			with ViewModuleEventsPermissions
			with ViewModuleEventsValidation
			with Unaudited with ReadOnly
			with AutowiringScientiaConfigurationComponent
			with SystemClockComponent
			with ScientiaHttpTimetableFetchingServiceComponent // Only include Scientia events for now. If we ever include from other sources, they should be opt-in via params
			with AutowiringTermServiceComponent
			with AutowiringTermBasedEventOccurrenceServiceComponent

	// Re-usable service
	def apply(module: Module, service: ModuleTimetableFetchingService): CommandType =
		new ViewModuleEventsCommandInternal(module)
			with ComposableCommand[ReturnType]
			with ViewModuleEventsPermissions
			with ViewModuleEventsValidation
			with Unaudited with ReadOnly
			with ModuleTimetableFetchingServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringTermBasedEventOccurrenceServiceComponent {
			val timetableFetchingService: ModuleTimetableFetchingService = service
		}
}

trait ViewModuleEventsCommandFactory {
	def apply(module: Module): CommandType
}
class ViewModuleEventsCommandFactoryImpl(service: ModuleTimetableFetchingService) extends ViewModuleEventsCommandFactory {
	def apply(module: Module) = ViewModuleEventsCommand(module, service)
}

abstract class ViewModuleEventsCommandInternal(val module: Module)
	extends CommandInternal[ReturnType]
		with ViewModuleEventsRequest {

	self: ModuleTimetableFetchingServiceComponent with TermServiceComponent with EventOccurrenceServiceComponent =>

	def applyInternal(): ReturnType = {
		val timetableEvents = timetableFetchingService.getTimetableForModule(module.code.toUpperCase, includeStudents = false)
			.map { events =>
				if (academicYear != null) {
					events.filter { event => event.year == academicYear }
				} else {
					events
				}
			}

		if (start == null) {
			start = termService.getTermFromDate(academicYear.dateInTermOne).getStartDate.toLocalDate
		}

		if (end == null) {
			end = termService.getTermFromDate((academicYear + 1).dateInTermOne).getStartDate.toLocalDate.minusDays(1)
		}

		Try(Await.result(timetableEvents.map { events =>
			// Converter to make localDates sortable
			import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

			EventOccurrenceList(events.events.flatMap { event =>
				eventOccurrenceService.fromTimetableEvent(event, new Interval(start.toDateTimeAtStartOfDay, end.toDateTimeAtStartOfDay))
			}.sortBy(_.start), events.lastUpdated)
		}, ViewModuleEventsCommand.Timeout)).recover {
			case _: TimeoutException | _: TimetableEmptyException => EventOccurrenceList(Nil, None)
		}
	}
}

// State - unmodifiable pre-requisites
trait ViewModuleEventsState {
	val module: Module
}

// Request parameters
trait ViewModuleEventsRequest extends ViewModuleEventsState {
	var academicYear: AcademicYear = _
	var start: LocalDate = LocalDate.now.minusMonths(12)
	var end: LocalDate = start.plusMonths(13)
}

trait ViewModuleEventsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewModuleEventsState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Module.ViewTimetable, mandatory(module))
	}
}

trait ViewModuleEventsValidation extends SelfValidating {
	self: ViewModuleEventsRequest =>

	override def validate(errors: Errors) {
		// Must have either an academic year or a start and an end
		if (academicYear == null && (start == null || end == null)) {
			errors.rejectValue("academicYear", "NotEmpty")
		}
	}
}