package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.{ModifyAssignmentFeedbackCommand, ModifyAssignmentFeedbackCommandState}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkBreadcrumbs


abstract class AbstractAssignmentFeedbackController extends AbstractAssignmentController {

  type ModifyAssignmentFeedbackCommand = Appliable[Assignment] with ModifyAssignmentFeedbackCommandState with PopulateOnForm

  @ModelAttribute("command")
  def modifyAssignmentFeedbackCommand(@PathVariable assignment: Assignment) =
    ModifyAssignmentFeedbackCommand(mandatory(assignment))

  def showForm(form: ModifyAssignmentFeedbackCommand, mode: String): Mav = {
    val module = form.module
    Mav(s"$urlPrefix/admin/assignments/assignment_feedback",
      "module" -> module,
      "department" -> module.adminDepartment,
      "mode" -> mode
    ).crumbs(CourseworkBreadcrumbs.Assignment.AssignmentManagement())
  }

  def submit(cmd: ModifyAssignmentFeedbackCommand, errors: Errors, path: String, mode: String) = {
    if (errors.hasErrors) {
      showForm(cmd, mode)
    } else {
      cmd.apply()
      Redirect(path)
    }
  }

}


@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}"))
class ModifyAssignmentFeedbackController extends AbstractAssignmentFeedbackController {

  @RequestMapping(method = Array(GET), value = Array("/new/feedback"))
  def form(
    @PathVariable("assignment") assignment: Assignment,
    @ModelAttribute("command") cmd: ModifyAssignmentFeedbackCommand
  ): Mav = {
    cmd.populate()
    showForm(cmd, createMode)
  }

  @RequestMapping(method = Array(GET), value = Array("/edit/feedback"))
  def formEdit(
    @PathVariable("assignment") assignment: Assignment,
    @ModelAttribute("command") cmd: ModifyAssignmentFeedbackCommand
  ): Mav = {
    cmd.populate()
    showForm(cmd, editMode)
  }


  @RequestMapping(method = Array(POST), value = Array("/new/feedback"), params = Array(ManageAssignmentMappingParameters.createAndAddFeedback, "action!=refresh", "action!=update"))
  def saveAndExit(@ModelAttribute("command") cmd: ModifyAssignmentFeedbackCommand, errors: Errors): Mav =
    submit(cmd, errors, Routes.home, createMode)

  @RequestMapping(method = Array(POST), value = Array("/new/feedback"), params = Array(ManageAssignmentMappingParameters.createAndAddStudents, "action!=refresh", "action!=update, action=submit"))
  def submitAndAddStudents(@Valid @ModelAttribute("command") cmd: ModifyAssignmentFeedbackCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
    submit(cmd, errors, Routes.admin.assignment.createOrEditStudents(assignment, createMode), createMode)

  @RequestMapping(method = Array(POST), value = Array("/edit/feedback"), params = Array(ManageAssignmentMappingParameters.editAndAddFeedback, "action!=refresh", "action!=update"))
  def saveAndExitForEdit(@ModelAttribute("command") cmd: ModifyAssignmentFeedbackCommand, errors: Errors): Mav =
    submit(cmd, errors, Routes.home, editMode)

  @RequestMapping(method = Array(POST), value = Array("/edit/feedback"), params = Array(ManageAssignmentMappingParameters.editAndAddStudents, "action!=refresh", "action!=update, action=submit"))
  def submitAndAddStudentsForEdit(@Valid @ModelAttribute("command") cmd: ModifyAssignmentFeedbackCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
    submit(cmd, errors, Routes.admin.assignment.createOrEditStudents(assignment, editMode), editMode)

}

