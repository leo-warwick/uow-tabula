package uk.ac.warwick.tabula.web.controllers.cm2.marker

import javax.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.markers.{SkipAndFinishMarkingCommand, WorkflowProgressState}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.userlookup.User

/**
  * Sends the feedback straight to the admin and skips any intermediate stages
  */
@Controller
@RequestMapping(value = Array("/coursework/admin/assignments/{assignment}/marker/{marker}/{stagePosition}/skip-marking"))
class SkipAndFinishMarkingController extends CourseworkController {

  validatesSelf[SelfValidating]

  type Command = Appliable[Seq[Feedback]] with WorkflowProgressState

  @ModelAttribute("command")
  def command(@PathVariable assignment: Assignment, @PathVariable marker: User, @PathVariable stagePosition: Int, currentUser: CurrentUser): Command =
    SkipAndFinishMarkingCommand(mandatory(assignment), mandatory(marker), currentUser, mandatory(stagePosition))

  // shouldn't ever be called as anything other than POST - if it is, just redirect back to the submission list
  @RequestMapping
  def get(@ModelAttribute("command") command: Command) =
    Redirect(Routes.admin.assignment.markerFeedback(command.assignment, command.marker))

  @RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
  def showForm(
    @PathVariable assignment: Assignment,
    @PathVariable marker: User,
    @PathVariable stagePosition: Int,
    @ModelAttribute("command") command: Command,
    errors: Errors
  ): Mav = {
    Mav("cm2/admin/assignments/markers/skip_moderation",
      "formAction" -> Routes.admin.assignment.markerFeedback.skip(assignment, stagePosition, marker),
      "department" -> command.assignment.module.adminDepartment,
      "isProxying" -> command.isProxying,
      "proxyingAs" -> marker,
      "nextStagesDescription" -> "Admin"
    )
  }

  @RequestMapping(method = Array(POST), params = Array("confirmScreen"))
  def submit(
    @PathVariable assignment: Assignment,
    @PathVariable marker: User,
    @PathVariable stagePosition: Int,
    @Valid @ModelAttribute("command") command: Command,
    errors: Errors
  ): Mav = {
    if (errors.hasErrors)
      showForm(assignment, marker, stagePosition, command, errors)
    else {
      command.apply()
      Redirect(Routes.admin.assignment.markerFeedback(command.assignment, command.marker))
    }
  }


}
