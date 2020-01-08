package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.assignments.markers.DownloadMarkersSubmissionsCommand
import uk.ac.warwick.tabula.commands.cm2.assignments.{AdminGetSingleSubmissionCommand, DownloadAllSubmissionsCommand, _}
import uk.ac.warwick.tabula.data.model.{Assignment, Submission}
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.system.RenderableFileView
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(value = Array("/coursework/admin/assignments/{assignment}/submissions.zip"))
class DownloadSubmissionsController extends CourseworkController {

  @ModelAttribute("command")
  def getSingleSubmissionCommand(@PathVariable assignment: Assignment): DownloadSubmissionsCommand.Command =
    DownloadSubmissionsCommand(mandatory(assignment), user)

  @RequestMapping
  def download(@ModelAttribute("command") command: DownloadSubmissionsCommand.Command, @PathVariable assignment: Assignment): Mav = {
    command.apply().output match {
      case Left(renderable) =>
        Mav(new RenderableFileView(renderable))
      case Right(jobInstance) =>
        Redirect(Routes.zipFileJob(jobInstance), "returnTo" -> Routes.admin.assignment.submissionsandfeedback(assignment))
    }
  }
}

@Controller
@RequestMapping(value = Array("/coursework/admin/assignments/{assignment}/marker/{marker}/submissions.zip"))
class DownloadMarkerSubmissionsController extends CourseworkController {

  @ModelAttribute("command")
  def getMarkersSubmissionCommand(@PathVariable assignment: Assignment, @PathVariable marker: User, submitter: CurrentUser) =
    DownloadMarkersSubmissionsCommand(assignment, marker, submitter)

  // shouldn't ever be called as a GET - if it is, just redirect back to the submission list
  @RequestMapping(method = Array(GET))
  def get(@PathVariable assignment: Assignment, @PathVariable marker: User) = Redirect(Routes.admin.assignment.markerFeedback(assignment, marker))

  @RequestMapping(method = Array(POST))
  def downloadMarkersSubmissions(@ModelAttribute("command") command: Appliable[RenderableFile]): RenderableFile = {
    command.apply()
  }

}

@Controller
@RequestMapping(value = Array("/coursework/admin/assignments/{assignment}/marker/submissions.zip"))
class DownloadMarkerSubmissionsControllerCurrentUser extends CourseworkController {
  @RequestMapping
  def redirect(@PathVariable assignment: Assignment, currentUser: CurrentUser): Mav = {
    Redirect(Routes.admin.assignment.markerFeedback.submissions(assignment, currentUser.apparentUser))
  }
}

@Controller
@RequestMapping(value = Array("/coursework/admin/assignments/{assignment}/submissions/download-zip/{filename}"))
class DownloadAllSubmissionsController extends CourseworkController {

  @ModelAttribute("downloadAllSubmissionsCommand")
  def getAllSubmissionsSubmissionCommand(@PathVariable assignment: Assignment, @PathVariable filename: String): DownloadAllSubmissionsCommand.Command =
    DownloadAllSubmissionsCommand(assignment, filename)

  @RequestMapping
  def downloadAll(@ModelAttribute("downloadAllSubmissionsCommand") command: DownloadAllSubmissionsCommand.Command): RenderableFile = {
    command.apply()
  }
}

@Controller
@RequestMapping(
  value = Array("/coursework/admin/assignments/{assignment}/submissions/download/{submission}/{filename}.zip"),
  params = Array("!single"))
class DownloadSingleSubmissionController extends CourseworkController {

  @ModelAttribute("adminSingleSubmissionCommand")
  def getSingleSubmissionCommand(@PathVariable assignment: Assignment, @PathVariable submission: Submission): AdminGetSingleSubmissionCommand.Command =
    AdminGetSingleSubmissionCommand.zip(mandatory(assignment), mandatory(submission))

  @RequestMapping
  def downloadSingle(@ModelAttribute("adminSingleSubmissionCommand") cmd: AdminGetSingleSubmissionCommand.Command): RenderableFile =
    cmd.apply()
}

@Controller
@RequestMapping(value = Array("/coursework/admin/assignments/{assignment}/submissions/download/{submission}/{filename}"))
class DownloadSingleSubmissionFileController extends CourseworkController {
  @ModelAttribute("adminSingleSubmissionCommand")
  def getSingleSubmissionCommand(@PathVariable assignment: Assignment, @PathVariable submission: Submission, @PathVariable filename: String): AdminGetSingleSubmissionCommand.Command =
    AdminGetSingleSubmissionCommand.single(mandatory(assignment), mandatory(submission), filename)

  @RequestMapping
  def downloadSingle(@ModelAttribute("adminSingleSubmissionCommand") cmd: AdminGetSingleSubmissionCommand.Command): RenderableFile =
    cmd.apply()

}

@Controller
@RequestMapping(value = Array("/coursework/admin/assignments/{assignment}/feedback-templates.zip"))
class DownloadFeedbackSheetsController extends CourseworkController {

  @ModelAttribute("downloadFeedbackSheetsCommand")
  def feedbackSheetsCommand(@PathVariable assignment: Assignment): DownloadFeedbackSheetsCommand.Command =
    DownloadFeedbackSheetsCommand(assignment)

  @RequestMapping
  def downloadFeedbackTemplatesOnly(@ModelAttribute("downloadFeedbackSheetsCommand") command: DownloadFeedbackSheetsCommand.Command): RenderableFile = {
    command.apply()
  }

}

@Controller
@RequestMapping(value = Array("/coursework/admin/assignments/{assignment}/marker-templates.zip"))
class DownloadMarkerTemplatesAsCurrentUserController extends CourseworkController {
  @RequestMapping
  def downloadFeedbackTemplates(@PathVariable assignment: Assignment): Mav = {
    Redirect(Routes.admin.assignment.markerTemplates(assignment, user.apparentUser))
  }
}

@Controller
@RequestMapping(value = Array("/coursework/admin/assignments/{assignment}/marker/{marker}/marker-templates.zip"))
class DownloadMarkerTemplatesController extends CourseworkController {

  @ModelAttribute("downloadFeedbackSheetsCommand")
  def feedbackSheetsCommand(@PathVariable assignment: Assignment, @PathVariable marker: User): DownloadFeedbackSheetsCommand.Command = {
    mandatory(marker)
    val students = assignment.cm2MarkerAllocations(marker).flatMap(_.students).distinct
    DownloadFeedbackSheetsCommand.marker(assignment, students)
  }

  @RequestMapping
  def downloadMarkerFeedbackTemplates(@ModelAttribute("downloadFeedbackSheetsCommand") command: DownloadFeedbackSheetsCommand.Command): RenderableFile = {
    command.apply()
  }

}