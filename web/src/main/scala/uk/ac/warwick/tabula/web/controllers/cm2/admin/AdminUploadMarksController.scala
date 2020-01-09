package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.assignments.{AdminAddMarksCommand, AdminAddMarksState}
import uk.ac.warwick.tabula.commands.cm2.assignments.markers.AddMarksCommandBindListener
import uk.ac.warwick.tabula.commands.cm2.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController


@Controller
@RequestMapping(value = Array("/coursework/admin/assignments/{assignment}/marks"))
class AdminUploadMarksController extends CourseworkController {

  type Command = Appliable[Seq[Feedback]] with AdminAddMarksState with AddMarksCommandBindListener

  @ModelAttribute("command")
  def command(@PathVariable assignment: Assignment, currentUser: CurrentUser) =
    AdminAddMarksCommand(mandatory(assignment), currentUser, GenerateGradesFromMarkCommand(assignment))

  @RequestMapping
  def viewForm(@PathVariable assignment: Assignment, @Valid @ModelAttribute("command") cmd: Command, errors: Errors): Mav = {
    Mav("cm2/admin/assignments/upload_marks",
      "isGradeValidation" -> cmd.assignment.module.adminDepartment.assignmentGradeValidation,
      "templateUrl" -> Routes.admin.assignment.marksTemplate(assignment),
      "formUrl" -> Routes.admin.assignment.marks(assignment),
      "cancelUrl" -> Routes.admin.assignment.submissionsandfeedback(assignment)
    ).crumbsList(Breadcrumbs.assignment(assignment))
  }

  @RequestMapping(method = Array(POST), params = Array("!confirm"))
  def preview(@PathVariable assignment: Assignment, @Valid @ModelAttribute("command") cmd: Command, errors: Errors): Mav = {
    if (errors.hasErrors) viewForm(assignment, cmd, errors)
    else {
      cmd.postBindValidation(errors)
      Mav("cm2/admin/assignments/upload_marks_preview",
        "formUrl" -> Routes.admin.assignment.marks(assignment),
        "cancelUrl" -> Routes.admin.assignment.submissionsandfeedback(assignment)
      ).crumbsList(Breadcrumbs.assignment(assignment))
    }
  }

  @RequestMapping(method = Array(POST), params = Array("confirm=true"))
  def upload(@PathVariable assignment: Assignment, @Valid @ModelAttribute("command") cmd: Command, errors: Errors): Mav = {
    cmd.apply()
    Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))
  }


}
