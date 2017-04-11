package uk.ac.warwick.tabula.web.controllers.profiles.timetables

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.timetables._
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventOccurrenceList
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, AutowiringSmallGroupServiceComponent, AutowiringTermServiceComponent, AutowiringUserLookupComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.web.views.{FullCalendarEvent, JSONView}

import scala.util.{Failure, Try}

@Controller
@RequestMapping(Array("/profiles/department/{department}/timetables/drafts/{academicYear}/{endpoint}"))
class DepartmentDraftTimetablesController extends ProfilesController
	with AutowiringUserLookupComponent with AutowiringTermServiceComponent {

	@ModelAttribute("activeDepartment")
	def activeDepartment(@PathVariable department: Department): Department = department

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable endpoint: String): DepartmentEventsCommand.CommandType = {
		val scientiaConfiguration = new ScientiaConfiguration {
			override val perYearUris: Seq[(String, AcademicYear)] = Seq(
				// FIXME hardcoded host
				s"https://timetablingmanagement.warwick.ac.uk/$endpoint" -> academicYear
			)
			override val cacheSuffix: String = s"$endpoint${academicYear.startYear}"
			override val cacheExpiryTime: Int = CachedPartialTimetableFetchingService.defaultCacheExpiryTime
		}

		val draftTimetableFetchingService = ScientiaHttpTimetableFetchingService(scientiaConfiguration)

		val moduleTimetableEventSource: ModuleTimetableEventSource =
			new CombinedModuleTimetableEventSourceComponent
			with SmallGroupEventTimetableEventSourceComponentImpl
			with AutowiringSmallGroupServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringCelcatConfigurationComponent
			with AutowiringExamTimetableConfigurationComponent
			with AutowiringSecurityServiceComponent
			with SystemClockComponent
			with ModuleTimetableFetchingServiceComponent {
				override val timetableFetchingService: ModuleTimetableFetchingService = draftTimetableFetchingService
			}.moduleTimetableEventSource

		DepartmentEventsCommand.draft(
			mandatory(department),
			academicYear,
			user,
			new ViewModuleTimetableCommandFactoryImpl(moduleTimetableEventSource),
			// Don't support students
			new ViewStudentMemberEventsCommandFactory() {
				override def apply(student: StudentMember): Appliable[Try[EventOccurrenceList]] with ViewMemberEventsRequest =
					new Appliable[Try[EventOccurrenceList]] with ViewMemberEventsRequest {
						override val member: StudentMember = student
						override def apply(): Try[EventOccurrenceList] = Failure(new IllegalArgumentException("Filtering students is not supported for draft timetables"))
					}
			},
			new ViewStaffMemberEventsCommandFactoryImpl(user)
		)
	}

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: DepartmentEventsCommand.CommandType, @PathVariable department: Department, @PathVariable academicYear: AcademicYear): Mav = {
		Mav("profiles/timetables/department_draft",
			"startDate" -> termService.getAcademicWeek(academicYear.dateInTermOne, 1).getStart.toLocalDate,
			"canFilterStudents" -> false,
			"canFilterStaff" -> securityService.can(user, DepartmentEventsCommand.FilterStaffPermission, mandatory(department)),
			"canFilterRoute" -> false,
			"canFilterYearOfStudy" -> false
		)
	}

	@RequestMapping(method = Array(POST))
	def post(
		@ModelAttribute("command") cmd: DepartmentEventsCommand.CommandType,
		@PathVariable department: Department
	): Mav = {
		val result = cmd.apply()
		val calendarEvents = FullCalendarEvent.colourEvents(result._1.events.map(FullCalendarEvent(_, userLookup)))
		Mav(new JSONView(Map("events" -> calendarEvents, "lastUpdated" -> result._1.lastUpdated, "errors" -> result._2)))
	}

}
