package uk.ac.warwick.tabula.web.controllers.coursework.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.coursework.feedback.{OldGenerateGradesFromMarkCommand, OldOnlineFeedbackCommand, OldOnlineFeedbackFormCommand}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User

@Profile(Array("cm1Enabled"))
@Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/feedback/online"))
class OldOnlineFeedbackController extends OldCourseworkController with AutowiringUserLookupComponent {

  @ModelAttribute
  def command(@PathVariable module: Module, @PathVariable assignment: Assignment, submitter: CurrentUser) =
    OldOnlineFeedbackCommand(mandatory(module), mandatory(assignment), submitter)

  @RequestMapping
  def showTable(@ModelAttribute command: OldOnlineFeedbackCommand, errors: Errors): Mav = {

    val feedbackGraphs = command.apply()
    val (assignment, module) = (command.assignment, command.assignment.module)

    Mav("coursework/admin/assignments/feedback/online_framework",
      "showMarkingCompleted" -> false,
      "showGenericFeedback" -> true,
      "assignment" -> assignment,
      "command" -> command,
      "studentFeedbackGraphs" -> feedbackGraphs,
      "onlineMarkingUrls" -> feedbackGraphs.map { graph =>
        graph.student.getUserId -> Routes.admin.assignment.onlineFeedback(assignment)
      }.toMap
    ).crumbs(
      Breadcrumbs.Department(module.adminDepartment),
      Breadcrumbs.Module(module)
    )
  }

}

@Profile(Array("cm1Enabled"))
@Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/feedback/online/{student}"))
class OldOnlineFeedbackFormController extends OldCourseworkController {

  validatesSelf[OldOnlineFeedbackFormCommand]

  @ModelAttribute("command")
  def command(@PathVariable student: User, @PathVariable module: Module, @PathVariable assignment: Assignment, currentUser: CurrentUser) =
    OldOnlineFeedbackFormCommand(module, assignment, student, currentUser.apparentUser, currentUser, OldGenerateGradesFromMarkCommand(mandatory(module), mandatory(assignment)))

  @RequestMapping(method = Array(GET, HEAD))
  def showForm(@ModelAttribute("command") command: OldOnlineFeedbackFormCommand, errors: Errors): Mav = {

    Mav("coursework/admin/assignments/feedback/online_feedback",
      "command" -> command,
      "isGradeValidation" -> command.module.adminDepartment.assignmentGradeValidation
    ).noLayout()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("command") @Valid command: OldOnlineFeedbackFormCommand, errors: Errors): Mav = {
    if (errors.hasErrors) {
      showForm(command, errors)
    } else {
      command.apply()
      Mav("ajax_success").noLayout()
    }
  }

}
