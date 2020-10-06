package uk.ac.warwick.tabula.api.web.controllers.timetables

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.controllers.timetables.MemberTimetableController._
import uk.ac.warwick.tabula.api.web.helpers.TimetableEventToJsonConverter
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.timetables.ViewMemberTimetableCommand
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}
import uk.ac.warwick.tabula.{CurrentUser, DateFormats, RequestFailedException}

import scala.util.{Failure, Success}

object MemberTimetableController {
  type ViewMemberTimetableCommand = ViewMemberTimetableCommand.TimetableCommand
}

@Controller
@RequestMapping(Array("/v1/member/{member}/timetable"))
class MemberTimetableController extends ApiController
  with GetMemberTimetableApi
  with TimetableEventToJsonConverter
  with AutowiringProfileServiceComponent

trait GetMemberTimetableApi {
  self: ApiController with TimetableEventToJsonConverter =>

  validatesSelf[SelfValidating]

  @ModelAttribute("getTimetableCommand")
  def command(@PathVariable member: Member, currentUser: CurrentUser): ViewMemberTimetableCommand =
    ViewMemberTimetableCommand(member, currentUser)

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def showModuleTimetable(@Valid @ModelAttribute("getTimetableCommand") command: ViewMemberTimetableCommand, errors: Errors): Mav = {
    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else command.apply() match {
      case Success(result) => Mav(new JSONView(Map(
        "success" -> true,
        "status" -> "ok",
        "events" -> result.events.map(jsonTimetableEventObject),
        "lastUpdated" -> result.lastUpdated.map(DateFormats.IsoDateTime.print).orNull
      )))
      case Failure(t) =>
        logger.error("Couldn't generate member timetable", t)
        throw new RequestFailedException("The timetabling service could not be reached", t)
    }
  }
}
