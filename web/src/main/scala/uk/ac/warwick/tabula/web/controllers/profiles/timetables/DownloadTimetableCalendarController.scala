package uk.ac.warwick.tabula.web.controllers.profiles.timetables

import org.joda.time.{DateTime, LocalDate}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.commands.timetables.ViewMemberEventsCommand
import uk.ac.warwick.tabula.commands.timetables.ViewMemberEventsCommand.TimetableCommand
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.KnowsUserNumberingSystem
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringUserLookupComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.web.views.PDFView
import uk.ac.warwick.tabula.{CurrentUser, RequestFailedException}

import scala.util.{Failure, Success}

@Controller
@RequestMapping(Array("/profiles/view/{member}/timetable/download-calendar"))
class DownloadTimetableCalendarController extends ProfilesController
  with DownloadsTimetableCalendar
  with AutowiringUserLookupComponent
  with KnowsUserNumberingSystem with AutowiringUserSettingsServiceComponent
  with AutowiringModuleAndDepartmentServiceComponent {

  @ModelAttribute("timetableCommand")
  def timetableCommand(@PathVariable member: Member, currentUser: CurrentUser) =
    ViewMemberEventsCommand(mandatory(member), currentUser)

  @RequestMapping
  def render(
    @ModelAttribute("timetableCommand") cmd: TimetableCommand,
    @PathVariable member: Member,
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
      case _ => // month
        val firstDayOfMonth = thisRenderDate.minusDays(thisRenderDate.getDayOfMonth - 1)
        val lastDayOfMonth = firstDayOfMonth.plusDays(thisRenderDate.dayOfMonth.getMaximumValue - 1)
        (
          firstDayOfMonth.minusDays(firstDayOfMonth.getDayOfWeek - 1), // The Monday of that week
          lastDayOfMonth.plusDays(7).minusDays(lastDayOfMonth.getDayOfWeek - 1) // The Moday of the following week
        )
    }

    cmd.from = startDate.toDateTimeAtStartOfDay.getMillis
    cmd.to = endDate.toDateTimeAtStartOfDay.getMillis

    cmd.apply() match {
      case Success(result) =>
        getCalendar(
          events = result.events,
          startDate = startDate,
          endDate = endDate,
          renderDate = thisRenderDate,
          calendarView = thisCalendarView,
          user = user,
          fileNameSuffix = member.universityId
        )

      case Failure(t) =>
        logger.error("Couldn't generate timetable member events for PDF", t)
        throw new RequestFailedException("The timetabling service could not be reached-render", t)
    }
  }


}
