package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.cm2.turnitin.SubmitToTurnitinCommand
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment}
import uk.ac.warwick.tabula.services.turnitinlti.{AutowiringTurnitinLtiQueueServiceComponent, TurnitinLtiService}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.web.views.JSONView

import scala.collection.JavaConverters._
import scala.collection.mutable

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/turnitin"))
class TurnitinController extends CourseworkController with AutowiringTurnitinLtiQueueServiceComponent {

  type SubmitToTurnitinCommand = SubmitToTurnitinCommand.CommandType

  validatesSelf[SelfValidating]

  @ModelAttribute("command")
  def model(@PathVariable assignment: Assignment, user: CurrentUser) =
    SubmitToTurnitinCommand(assignment, user)

  @ModelAttribute("incompatibleFiles")
  def incompatibleFiles(@PathVariable assignment: Assignment): mutable.Buffer[FileAttachment] = {
    val allAttachments = mandatory(assignment).submissions.asScala.flatMap(_.allAttachments)
    allAttachments.filterNot(a =>
      TurnitinLtiService.validFileType(a) && TurnitinLtiService.validFileSize(a)
    )
  }

  @RequestMapping
  def confirm(@PathVariable assignment: Assignment): Mav =
    Mav("cm2/admin/assignments/turnitin/form")
      .crumbsList(Breadcrumbs.assignment(assignment))

  @RequestMapping(method = Array(POST))
  def submit(@Valid @ModelAttribute("command") command: SubmitToTurnitinCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
    if (errors.hasErrors) {
      confirm(assignment)
    } else {
      command.apply()
      Redirect(Routes.admin.assignment.turnitin.status(command.assignment))
    }

  @RequestMapping(value = Array("/status"))
  def status(@PathVariable assignment: Assignment): Mav = {
    val assignmentStatus = turnitinLtiQueueService.getAssignmentStatus(assignment)
    if (ajax) {
      Mav(new JSONView(assignmentStatus.toMap))
    } else {
      Mav("cm2/admin/assignments/turnitin/status", "status" -> assignmentStatus)
        .crumbsList(Breadcrumbs.assignment(assignment))
    }
  }

}