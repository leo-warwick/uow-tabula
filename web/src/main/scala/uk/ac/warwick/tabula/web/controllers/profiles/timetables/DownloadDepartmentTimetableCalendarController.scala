package uk.ac.warwick.tabula.web.controllers.profiles.timetables

import org.joda.time.{DateTime, LocalDate}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.commands.CurrentSITSAcademicYear
import uk.ac.warwick.tabula.commands.timetables._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.KnowsUserNumberingSystem
import uk.ac.warwick.tabula.services.timetables.AutowiringModuleTimetableEventSourceComponent
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringTermServiceComponent, AutowiringUserLookupComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.web.views.PDFView

@Controller
@RequestMapping(Array("/profiles/department/{department}/timetables/download-calendar"))
class DownloadDepartmentTimetableCalendarController extends ProfilesController
	with DownloadsTimetableCalendar
	with AutowiringUserLookupComponent with AutowiringTermServiceComponent
	with KnowsUserNumberingSystem with AutowiringUserSettingsServiceComponent
	with AutowiringModuleAndDepartmentServiceComponent with CurrentSITSAcademicYear
	with AutowiringModuleTimetableEventSourceComponent {

	@ModelAttribute("timetableCommand")
	def timetableCommand(@PathVariable department: Department): DepartmentEventsCommand.CommandType = {
		DepartmentEventsCommand(
			mandatory(department),
			academicYear,
			user,
			new ViewModuleTimetableCommandFactoryImpl(moduleTimetableEventSource),
			new ViewStudentMemberEventsCommandFactoryImpl(user),
			new ViewStaffMemberEventsCommandFactoryImpl(user)
		)
	}

	@RequestMapping
	def render(
		@ModelAttribute("timetableCommand") cmd: DepartmentEventsCommand.CommandType,
		@PathVariable department: Department,
		@RequestParam(value = "calendarView", required = false) calendarView: String,
		@RequestParam(value = "renderDate", required = false) renderDate: LocalDate
	): PDFView = {
		val thisRenderDate = Option(renderDate).getOrElse(DateTime.now.toLocalDate)
		val thisCalendarView = Option(calendarView).getOrElse("month")
		val (startDate, endDate) = Option(calendarView).getOrElse("month") match {
			case "agendaDay" =>
				(
					thisRenderDate,
					thisRenderDate.plusDays(1)
				)
			case "agendaWeek" =>
				(
					thisRenderDate.minusDays(thisRenderDate.getDayOfWeek - 1), // The Monday of that week
					thisRenderDate.plusDays(7).minusDays(thisRenderDate.getDayOfWeek - 1) // The Moday of the following week
				)
			case _ =>  // month
				val firstDayOfMonth = thisRenderDate.minusDays(thisRenderDate.getDayOfMonth - 1)
				val lastDayOfMonth = firstDayOfMonth.plusDays(thisRenderDate.dayOfMonth.getMaximumValue - 1)
				(
					firstDayOfMonth.minusDays(firstDayOfMonth.getDayOfWeek - 1), // The Monday of that week
					lastDayOfMonth.plusDays(7).minusDays(lastDayOfMonth.getDayOfWeek - 1) // The Moday of the following week
				)
		}

		getCalendar(
			events = cmd.apply()._1.events,
			startDate = startDate,
			endDate = endDate,
			renderDate = thisRenderDate,
			calendarView = thisCalendarView,
			user = user,
			fileNameSuffix = department.code
		)
	}



}
