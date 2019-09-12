package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.cm2.turnitin.{TurnitinReportErrorWithMessage, ViewPlagiarismReportCommand}
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment, Module}
import uk.ac.warwick.tabula.web.Mav

/**
  * Provides access to the Turnitin Document Viewer for a submission
  * that's been submitted to Turnitin.
  */
@Profile(Array("cm1Enabled"))
@Controller
@RequestMapping(value = Array(
  "/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/turnitin/report/{attachment}",
  "/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/turnitin/lti-report/{attachment}"
))
class OldTurnitinReportController extends OldCourseworkController {

  type ViewPlagiarismReportCommand = ViewPlagiarismReportCommand.CommandType

  @ModelAttribute("command") def command(
    @PathVariable module: Module,
    @PathVariable assignment: Assignment,
    @PathVariable attachment: FileAttachment
  ): ViewPlagiarismReportCommand = ViewPlagiarismReportCommand(mandatory(assignment), mandatory(attachment), user)

  @RequestMapping
  def goToReport(@ModelAttribute("command") command: ViewPlagiarismReportCommand): Mav = command.apply() match {
    case Left(uri) =>
      if (command.ltiParams.nonEmpty) Mav("coursework/admin/assignments/turnitin/lti_report_forward", "turnitin_report_url" -> uri, "params" -> command.ltiParams)
      else Mav("redirect:" + uri.toString)

    case Right(error: TurnitinReportErrorWithMessage) =>
      Mav("coursework/admin/assignments/turnitin/report_error", "problem" -> error.code, "message" -> error.message)

    case Right(error) =>
      Mav("coursework/admin/assignments/turnitin/report_error", "problem" -> error.code)
  }

}