package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.helpers.{Futures, SystemClockComponent}
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, AutowiringSmallGroupServiceComponent, AutowiringUserLookupComponent}

import scala.concurrent.Future
import uk.ac.warwick.tabula.helpers.Futures._

sealed trait TimetableEventSource
object TimetableEventSource {
	case object Sciencia extends TimetableEventSource
	case object SmallGroups extends TimetableEventSource
}

trait ModuleTimetableEventSource {
	def eventsFor(module: Module, academicYear: AcademicYear, currentUser: CurrentUser, sources: Seq[TimetableEventSource]): Future[EventList]
}

trait ModuleTimetableEventSourceComponent {
	def moduleTimetableEventSource: ModuleTimetableEventSource
}

trait CombinedModuleTimetableEventSourceComponent extends ModuleTimetableEventSourceComponent {
	self: ModuleTimetableFetchingServiceComponent with SmallGroupEventTimetableEventSourceComponent =>

	def moduleTimetableEventSource: ModuleTimetableEventSource = new CombinedModuleTimetableEventSource

	class CombinedModuleTimetableEventSource() extends ModuleTimetableEventSource {
		def eventsFor(module: Module, academicYear: AcademicYear, currentUser: CurrentUser, sources: Seq[TimetableEventSource]): Future[EventList] = {
			val timetableEvents: Future[EventList] = if (sources.contains(TimetableEventSource.Sciencia)) {
				timetableFetchingService.getTimetableForModule(module.code, includeStudents = false)
			} else {
				Future.successful { EventList.empty }
			}
			val smallGroupEvents: Future[EventList] = if (sources.contains(TimetableEventSource.SmallGroups)) {
				moduleGroupEventSource.eventsFor(module, academicYear, currentUser, sources)
			} else {
				Future.successful { EventList.empty }
			}

			Futures.combine(Seq(timetableEvents, smallGroupEvents), EventList.combine)
		}
	}

}

trait AutowiringModuleTimetableEventSourceComponent extends ModuleTimetableEventSourceComponent {
	val moduleTimetableEventSource: ModuleTimetableEventSource = (new CombinedModuleTimetableEventSourceComponent
		with SmallGroupEventTimetableEventSourceComponentImpl
		with CombinedHttpTimetableFetchingServiceComponent
		with AutowiringSmallGroupServiceComponent
		with AutowiringUserLookupComponent
		with AutowiringScientiaConfigurationComponent
		with AutowiringCelcatConfigurationComponent
		with AutowiringExamTimetableConfigurationComponent
		with AutowiringSecurityServiceComponent
		with SystemClockComponent
		).moduleTimetableEventSource
}
