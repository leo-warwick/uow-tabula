package uk.ac.warwick.tabula.api.web.controllers.groups

import javax.servlet.http.HttpServletResponse
import javax.validation.Valid
import org.joda.time.LocalTime
import org.springframework.http.{HttpStatus, MediaType}
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports.{JMap, _}
import uk.ac.warwick.tabula.api.commands.JsonApiRequest
import uk.ac.warwick.tabula.api.web.controllers.groups.SmallGroupEventController.{DeleteSmallGroupEventCommand, ModifySmallGroupEventCommand}
import uk.ac.warwick.tabula.commands.{Appliable, MemberOrUser}
import uk.ac.warwick.tabula.commands.groups.RecordAttendanceCommand.UniversityId
import uk.ac.warwick.tabula.commands.groups.{RecordAttendanceCommand, SmallGroupAttendanceState}
import uk.ac.warwick.tabula.commands.groups.admin._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

import scala.beans.BeanProperty

object SmallGroupEventController {
  type DeleteSmallGroupEventCommand = DeleteSmallGroupEventCommand.Command
  type ModifySmallGroupEventCommand = ModifySmallGroupEventCommand.Command
}


@Controller
@RequestMapping(Array("/v1/module/{module}/groups/{smallGroupSet}/groups/{smallGroup}/events"))
class CreateSmallGroupEventControllerForApi extends SmallGroupSetController with CreateSmallGroupEventApi

trait CreateSmallGroupEventApi {
  self: SmallGroupSetController =>

  @ModelAttribute("createCommand")
  def createCommand(@PathVariable module: Module, @PathVariable smallGroupSet: SmallGroupSet, @PathVariable smallGroup: SmallGroup): ModifySmallGroupEventCommand =
    ModifySmallGroupEventCommand.create(module, smallGroupSet, smallGroup)


  @RequestMapping(method = Array(POST), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
  def createEvent(@RequestBody request: SmallGroupEventRequest, @ModelAttribute("createCommand") command: ModifySmallGroupEventCommand, errors: Errors, @PathVariable smallGroupSet: SmallGroupSet)(implicit response: HttpServletResponse): Mav = {
    request.copyTo(command, errors)
    globalValidator.validate(command, errors)
    command.validate(errors)

    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else {
      val event = command.apply()
      response.setStatus(HttpStatus.CREATED.value())
      response.addHeader("Location", toplevelUrl + Routes.api.event(event))

      getSmallGroupSetMav(smallGroupSet)
    }
  }
}

@Controller
@RequestMapping(Array("/v1/module/{module}/groups/{smallGroupSet}/groups/{smallGroup}/events/{smallGroupEvent}"))
class EditSmallGroupEventControllerForApi extends SmallGroupSetController with EditSmallGroupEventApi

trait EditSmallGroupEventApi {
  self: SmallGroupSetController =>

  @ModelAttribute("editCommand")
  def editCommand(@PathVariable module: Module, @PathVariable smallGroupSet: SmallGroupSet, @PathVariable smallGroup: SmallGroup, @PathVariable smallGroupEvent: SmallGroupEvent): ModifySmallGroupEventCommand =
    ModifySmallGroupEventCommand.edit(module, smallGroupSet, smallGroup, smallGroupEvent)


  @RequestMapping(method = Array(PUT), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
  def editEvent(@RequestBody request: SmallGroupEventRequest, @ModelAttribute("editCommand") command: ModifySmallGroupEventCommand, errors: Errors, @PathVariable smallGroupSet: SmallGroupSet, @PathVariable smallGroupEvent: SmallGroupEvent): Mav = {
    request.copyTo(command, errors)
    globalValidator.validate(command, errors)
    command.validate(errors)
    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else {
      command.apply()
      getSmallGroupSetMav(smallGroupSet)
    }
  }
}


@Controller
@RequestMapping(Array("/v1/module/{module}/groups/{smallGroupSet}/groups/{smallGroup}/events/{smallGroupEvent}"))
class DeleteSmallGroupEventControllerForApi extends SmallGroupSetController with DeleteSmallGroupEventApi

trait DeleteSmallGroupEventApi {
  self: SmallGroupSetController =>

  @ModelAttribute("deleteCommand")
  def deleteCommand(@PathVariable module: Module, @PathVariable smallGroupSet: SmallGroupSet, @PathVariable smallGroup: SmallGroup, @PathVariable smallGroupEvent: SmallGroupEvent): DeleteSmallGroupEventCommand =
    DeleteSmallGroupEventCommand(smallGroup, smallGroupEvent)


  @RequestMapping(method = Array(DELETE), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
  def deleteEvent(@Valid @ModelAttribute("deleteCommand") command: DeleteSmallGroupEventCommand, errors: Errors): Mav = {
    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else {
      command.apply()
      Mav(new JSONView(Map(
        "success" -> true,
        "status" -> "ok"
      )))
    }
  }
}

@Controller
@RequestMapping(Array("/v1/module/{module}/groups/{smallGroupSet}/groups/{smallGroup}/events/{smallGroupEvent}/register"))
class RecordSmallGroupEventAttendanceControllerForApi extends SmallGroupSetController with RecordSmallGroupAttendanceApi

trait RecordSmallGroupAttendanceApi {
  self: SmallGroupSetController =>

  @ModelAttribute("recordAttendanceCommand")
  def recordAttendanceCommand(@PathVariable smallGroupEvent: SmallGroupEvent, @RequestParam week: Int, user: CurrentUser): RecordAttendanceCommand.Command =
    RecordAttendanceCommand(mandatory(smallGroupEvent), week, user)

  @RequestMapping(method = Array(PUT), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
  def recordAttendance(@RequestBody request: SmallGroupAttendanceRequest, @Valid @ModelAttribute("recordAttendanceCommand") command: RecordAttendanceCommand.Command, errors: Errors): Mav = {
    request.copyTo(command, errors)
    globalValidator.validate(command, errors)
    command.validate(errors)
    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else {
      command.apply()
      Mav(new JSONView(Map(
        "success" -> true,
        "status" -> "ok"
      )))
    }
  }
}

@Controller
@RequestMapping(Array("/v1/module/{module}/groups/{smallGroupSet}/groups/{smallGroup}/events/{smallGroupEvent}/members"))
class AddAdditionalSmallGroupStudentControllerForApi extends SmallGroupSetController with AddAdditionalSmallGroupStudentApi

trait AddAdditionalSmallGroupStudentApi {
  self: SmallGroupSetController =>

  @ModelAttribute("recordAttendanceCommand")
  def recordAttendanceCommand(@PathVariable smallGroupEvent: SmallGroupEvent, @RequestParam week: Int, user: CurrentUser): RecordAttendanceCommand.Command =
    RecordAttendanceCommand(mandatory(smallGroupEvent), week, user)

  @RequestMapping(method = Array(PUT), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
  def addStudent(@RequestBody request: SmallGroupAddStudentRequest, @Valid @ModelAttribute("recordAttendanceCommand") command: RecordAttendanceCommand.Command, errors: Errors): Mav = {
    request.copyTo(command, errors)
    globalValidator.validate(command, errors)
    command.validate(errors)
    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else {
      command.addAdditionalStudent(command.members)
      Mav(new JSONView(Map(
        "success" -> true,
        "status" -> "ok"
      )))
    }
  }
}


class SmallGroupEventRequest extends JsonApiRequest[ModifySmallGroupEventCommand] {
  @BeanProperty var title: String = _
  @BeanProperty var tutors: JList[String] = _
  @BeanProperty var weeks: JSet[JInteger] = _
  @BeanProperty var day: DayOfWeek = null
  @BeanProperty var startTime: LocalTime = _
  @BeanProperty var endTime: LocalTime = _
  @BeanProperty var location: String = _

  override def copyTo(state: ModifySmallGroupEventCommand, errors: Errors): Unit = {
    Option(title).foreach(state.title = _)
    Option(tutors).foreach(state.tutors = _)
    Option(weeks).foreach(state.weeks = _)
    Option(day).foreach(state.day = _)
    Option(startTime).foreach(state.startTime = _)
    Option(endTime).foreach(state.endTime = _)
    Option(location).foreach(state.location = _)
    state.useNamedLocation = true
  }
}

class SmallGroupAttendanceRequest extends JsonApiRequest[RecordAttendanceCommand.Command] {
  @BeanProperty var attendance: JMap[UniversityId, SmallGroupAttendanceState] = _

  override def copyTo(state: RecordAttendanceCommand.Command, errors: Errors): Unit = {
    state.studentsState.putAll(attendance)
  }
}

class SmallGroupAddStudentRequest extends JsonApiRequest[RecordAttendanceCommand.Command] {
  @BeanProperty var member: Member = _
  @BeanProperty var replacedWeek: JInteger = _
  @BeanProperty var replacedEvent: SmallGroupEvent = _

  override def copyTo(state: RecordAttendanceCommand.Command, errors: Errors): Unit = {
    state.additionalStudent = member
    state.replacedWeek = replacedWeek
    state.replacedEvent = replacedEvent
  }
}