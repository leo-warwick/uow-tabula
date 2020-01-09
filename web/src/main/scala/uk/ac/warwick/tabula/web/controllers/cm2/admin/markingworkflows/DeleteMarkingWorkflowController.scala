package uk.ac.warwick.tabula.web.controllers.cm2.admin.markingworkflows

import javax.validation.Valid

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire

import scala.jdk.CollectionConverters._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.cm2.markingworkflows.{DeleteMarkingWorkflowCommand, DeleteMarkingWorkflowState}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.{Mav, Routes}


@Controller
@RequestMapping(Array("/coursework/admin/department/{department}/{academicYear}/markingworkflows/{workflow}/delete"))
class DeleteMarkingWorkflowController extends CM2MarkingWorkflowController {

  type DeleteMarkingWorkflowCommand = Appliable[CM2MarkingWorkflow] with DeleteMarkingWorkflowState

  validatesSelf[SelfValidating]

  var messageSource: MessageSource = Wire.auto[MessageSource]

  @ModelAttribute("deleteMarkingWorkflowCommand")
  def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable workflow: CM2MarkingWorkflow) =
    DeleteMarkingWorkflowCommand(mandatory(department), mandatory(workflow))

  @RequestMapping
  def confirm(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable workflow: CM2MarkingWorkflow): Mav =
    commonCrumbs(
      Mav("cm2/admin/workflows/delete_workflow"),
      department,
      academicYear
    )

  @RequestMapping(method = Array(POST))
  def submitForm(
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
    @Valid @ModelAttribute("deleteMarkingWorkflowCommand") cmd: DeleteMarkingWorkflowCommand,
    errors: Errors
  ): Mav = {
    val model = if (errors.hasErrors) {
      "actionErrors" -> errors.getAllErrors.asScala.map(
        e => messageSource.getMessage(e.getCode, e.getArguments, null)
      ).mkString(", ")
    } else {
      "deletedWorkflow" -> cmd.apply()
    }
    Redirect(Routes.cm2.admin.workflows(department, academicYear), model)
  }

}
