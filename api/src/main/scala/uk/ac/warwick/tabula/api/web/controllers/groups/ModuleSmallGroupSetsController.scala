package uk.ac.warwick.tabula.api.web.controllers.groups

import javax.servlet.http.HttpServletResponse
import javax.validation.Valid
import org.springframework.http.{HttpStatus, MediaType}
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.api.commands.JsonApiRequest
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.controllers.groups.ModuleSmallGroupSetsController._
import uk.ac.warwick.tabula.api.web.helpers._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.admin.{AdminSmallGroupsHomeCommand, _}
import uk.ac.warwick.tabula.commands.scheduling.UpdateLinkedSmallGroupSetsCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupFormat, SmallGroupSet}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{ViewGroup, ViewSet}
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._


object ModuleSmallGroupSetsController {
  type AdminSmallGroupsHomeCommand = Appliable[AdminSmallGroupsHomeInformation] with AdminSmallGroupsHomeCommandState
  type CreateSmallGroupSetCommand = ModifySmallGroupSetCommand.CreateCommand
}

abstract class ModuleSmallGroupSetsController extends ApiController
  with SmallGroupSetToJsonConverter
  with SmallGroupToJsonConverter
  with SmallGroupEventToJsonConverter
  with SmallGroupAttendanceToJsonConverter
  with AssessmentMembershipInfoToJsonConverter
  with SmallGroupEventAttendanceNoteToJsonConverter
  with FileAttachmentToJsonConverter {

  hideDeletedItems

}


@Controller
@RequestMapping(Array("/v1/module/{module}/groups"))
class ListSmallGroupSetsForModule extends ModuleSmallGroupSetsController with ListSmallGroupSetsForModuleApi

trait ListSmallGroupSetsForModuleApi {
  self: ApiController with SmallGroupSetToJsonConverter =>

  @ModelAttribute("listCommand")
  def command(@PathVariable module: Module, @RequestParam(required = false) academicYear: AcademicYear, user: CurrentUser): AdminSmallGroupsHomeCommand = {
    val year = Option(academicYear).getOrElse(AcademicYear.now())
    AdminSmallGroupsHomeCommand(mandatory(module).adminDepartment, year, user, calculateProgress = false)
  }

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def list(@ModelAttribute("listCommand") command: AdminSmallGroupsHomeCommand, errors: Errors, @PathVariable module: Module): Mav = {
    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else {
      val info = command.apply()

      Mav(new JSONView(Map(
        "success" -> true,
        "status" -> "ok",
        "academicYear" -> command.academicYear.toString,
        "groups" -> info.setsWithPermission.filter(_.set.module == module).map { viewSet => jsonSmallGroupSetObject(viewSet) }
      )))
    }
  }
}

@Controller
@RequestMapping(Array("/v1/module/{module}/groups"))
class CreateSmallGroupSetControllerForModuleApi extends ModuleSmallGroupSetsController {

  @ModelAttribute("createSmallGroupSetCommand")
  def command(@PathVariable module: Module): CreateSmallGroupSetCommand =
    ModifySmallGroupSetCommand.create(module)

  @RequestMapping(method = Array(POST), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
  def create(@Valid @ModelAttribute("createSmallGroupSetCommand") command: CreateSmallGroupSetCommand, @RequestBody request: CreateSmallGroupSetRequest, errors: Errors)(implicit response: HttpServletResponse): Mav = {
    request.copyTo(command, errors)
    globalValidator.validate(command, errors)
    command.validate(errors)

    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else {
      val smallGroupSet: SmallGroupSet = command.apply()
      val set = if (smallGroupSet.generateLinkedSitsQueryMembers) {
        UpdateLinkedSmallGroupSetsCommand.updateIndividualSmallGroupSet(smallGroupSet).apply()
      } else smallGroupSet

      response.setStatus(HttpStatus.CREATED.value())
      response.addHeader("Location", toplevelUrl + Routes.api.groupSet(set))

      val viewSet = new ViewSet(set, ViewGroup.fromGroups(set.groups.asScala.toSeq.sorted), GroupsViewModel.Tutor)
      Mav(new JSONView(Map(
        "success" -> true,
        "status" -> "ok",
        "academicYear" -> command.academicYear.toString,
        "groupSet" -> jsonSmallGroupSetObject(viewSet)
      )))
    }
  }
}

trait SmallGroupSetPropertiesRequest[A <: ModifySmallGroupSetCommandState] extends JsonApiRequest[A] {
  @BeanProperty var name: String = null
  @BeanProperty var format: SmallGroupFormat = null
  @BeanProperty var allocationMethod: SmallGroupAllocationMethod = null

  @BeanProperty var studentsCanSeeTutorName: JBoolean = false
  @BeanProperty var studentsCanSeeOtherMembers: JBoolean = false
  @BeanProperty var collectAttendance: JBoolean = true
  @BeanProperty var allowSelfGroupSwitching: JBoolean = true

  override def copyTo(state: A, errors: Errors): Unit = {
    Option(name).foreach(state.name = _)
    Option(format).foreach(state.format = _)
    Option(allocationMethod).foreach(state.allocationMethod = _)
    Option(studentsCanSeeTutorName).foreach(state.studentsCanSeeTutorName = _)
    Option(studentsCanSeeOtherMembers).foreach(state.studentsCanSeeOtherMembers = _)
    Option(collectAttendance).foreach(state.collectAttendance = _)
    Option(allowSelfGroupSwitching).foreach(state.allowSelfGroupSwitching = _)
  }
}

class CreateSmallGroupSetRequest extends SmallGroupSetPropertiesRequest[CreateSmallGroupSetCommand] {
  // Default values
  format = SmallGroupFormat.Seminar
}
