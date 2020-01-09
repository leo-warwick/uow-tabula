package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.{ModifyFeedbackCommand, ModifyFeedbackCommandState}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav

abstract class AbstractAssignmentFeedbackController extends AbstractAssignmentController {

  type ModifyAssignmentFeedbackCommand = Appliable[Assignment] with ModifyFeedbackCommandState with PopulateOnForm

  @ModelAttribute("command")
  def modifyAssignmentFeedbackCommand(@PathVariable assignment: Assignment) =
    ModifyFeedbackCommand(mandatory(assignment))

  def showForm(form: ModifyAssignmentFeedbackCommand, mode: String): Mav = {
    val module = form.assignment.module
    Mav("cm2/admin/assignments/assignment_feedback",
      "module" -> module,
      "department" -> module.adminDepartment,
      "mode" -> mode)
      .crumbsList(Breadcrumbs.assignment(form.assignment))
  }

  def submit(cmd: ModifyAssignmentFeedbackCommand, errors: Errors, mav: Mav, mode: String): Mav = {
    if (errors.hasErrors) {
      showForm(cmd, mode)
    } else {
      cmd.apply()
      mav
    }
  }

}


@Controller
@RequestMapping(value = Array("/coursework/admin/assignments/{assignment}"))
class ModifyAssignmentFeedbackController extends AbstractAssignmentFeedbackController {

  @RequestMapping(method = Array(GET), value = Array("/new/feedback"))
  def form(
    @PathVariable assignment: Assignment,
    @ModelAttribute("command") cmd: ModifyAssignmentFeedbackCommand
  ): Mav = {
    cmd.populate()
    showForm(cmd, createMode)
  }

  @RequestMapping(method = Array(GET), value = Array("/edit/feedback"))
  def formEdit(
    @PathVariable assignment: Assignment,
    @ModelAttribute("command") cmd: ModifyAssignmentFeedbackCommand
  ): Mav = {
    cmd.populate()
    showForm(cmd, editMode)
  }


  @RequestMapping(method = Array(POST), value = Array("/new/feedback"), params = Array(ManageAssignmentMappingParameters.createAndAddFeedback, "action!=refresh", "action!=update"))
  def saveAndExit(@ModelAttribute("command") cmd: ModifyAssignmentFeedbackCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
    submit(cmd, errors, Redirect(Routes.admin.assignment.submissionsandfeedback(assignment)), createMode)

  @RequestMapping(method = Array(POST), value = Array("/new/feedback"), params = Array(ManageAssignmentMappingParameters.createAndAddStudents, "action!=refresh", "action!=update, action=submit"))
  def submitAndAddStudents(@Valid @ModelAttribute("command") cmd: ModifyAssignmentFeedbackCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
    submit(cmd, errors, RedirectForce(Routes.admin.assignment.createOrEditStudents(assignment, createMode)), createMode)

  @RequestMapping(method = Array(POST), value = Array("/edit/feedback"), params = Array(ManageAssignmentMappingParameters.editAndAddFeedback, "action!=refresh", "action!=update"))
  def saveAndExitForEdit(@ModelAttribute("command") cmd: ModifyAssignmentFeedbackCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
    submit(cmd, errors, Redirect(Routes.admin.assignment.submissionsandfeedback(assignment)), editMode)

  @RequestMapping(method = Array(POST), value = Array("/edit/feedback"), params = Array(ManageAssignmentMappingParameters.editAndAddStudents, "action!=refresh", "action!=update, action=submit"))
  def submitAndAddStudentsForEdit(@Valid @ModelAttribute("command") cmd: ModifyAssignmentFeedbackCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
    submit(cmd, errors, RedirectForce(Routes.admin.assignment.createOrEditStudents(assignment, editMode)), editMode)

}

