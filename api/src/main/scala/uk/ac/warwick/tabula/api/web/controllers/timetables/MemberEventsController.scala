package uk.ac.warwick.tabula.api.web.controllers.timetables

import javax.validation.Valid

import org.joda.time.LocalDate
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.controllers.timetables.MemberEventsController._
import uk.ac.warwick.tabula.api.web.helpers.EventOccurrenceToJsonConverter
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.timetables.ViewMemberEventsCommand
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}
import uk.ac.warwick.tabula.{CurrentUser, DateFormats, RequestFailedException}

import scala.util.{Failure, Success}

object MemberEventsController {
  type ViewMemberEventsCommand = ViewMemberEventsCommand.TimetableCommand
}

@Controller
@RequestMapping(Array("/v1/member/{member}/timetable/events"))
class MemberEventsController extends ApiController
  with GetMemberEventsApi
  with EventOccurrenceToJsonConverter
  with AutowiringProfileServiceComponent

trait GetMemberEventsApi {
  self: ApiController with EventOccurrenceToJsonConverter =>

  validatesSelf[SelfValidating]

  @ModelAttribute("getTimetableCommand")
  def command(@PathVariable member: Member, currentUser: CurrentUser): ViewMemberEventsCommand =
    ViewMemberEventsCommand(mandatory(member), currentUser)

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def showMemberTimetable(
    @Valid @ModelAttribute("getTimetableCommand") command: ViewMemberEventsCommand,
    errors: Errors,
    @RequestParam(required = false) start: LocalDate,
    @RequestParam(required = false) end: LocalDate
  ): Mav = {
    for (from <- Option(start); to <- Option(end)) {
      command.from = from.toDateTimeAtStartOfDay.getMillis
      command.to = to.toDateTimeAtStartOfDay.getMillis
    }

    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else command.apply() match {
      case Success(result) => Mav(new JSONView(Map(
        "success" -> true,
        "status" -> "ok",
        "events" -> result.events.map(jsonEventOccurrenceObject),
        "lastUpdated" -> result.lastUpdated.map(DateFormats.IsoDateTime.print).orNull
      )))
      case Failure(t) =>
        logger.error("Couldn't generate timetable member events", t)
        throw new RequestFailedException("The timetabling service could not be reached-showMemberTimetable", t)
    }
  }
}
