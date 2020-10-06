package uk.ac.warwick.tabula.api.web.controllers.timetables

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.controllers.timetables.ModuleEventsController._
import uk.ac.warwick.tabula.api.web.helpers.EventOccurrenceToJsonConverter
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.timetables.ViewModuleEventsCommand
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}
import uk.ac.warwick.tabula.{DateFormats, RequestFailedException}

import scala.util.{Failure, Success}

object ModuleEventsController {
  type ViewModuleEventsCommand = ViewModuleEventsCommand.CommandType
}

@Controller
@RequestMapping(Array("/v1/module/{module}/timetable/events"))
class ModuleEventsController extends ApiController
  with GetModuleEventsApi
  with EventOccurrenceToJsonConverter
  with AutowiringProfileServiceComponent

trait GetModuleEventsApi {
  self: ApiController with EventOccurrenceToJsonConverter =>

  validatesSelf[SelfValidating]

  @ModelAttribute("getTimetableCommand")
  def command(@PathVariable module: Module): ViewModuleEventsCommand =
    ViewModuleEventsCommand(module)

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def showModuleEvents(@Valid @ModelAttribute("getTimetableCommand") command: ViewModuleEventsCommand, errors: Errors): Mav = {
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
        logger.error("Couldn't generate timetable module events", t)
        throw new RequestFailedException("The timetabling service could not be reached", t)
    }
  }
}
