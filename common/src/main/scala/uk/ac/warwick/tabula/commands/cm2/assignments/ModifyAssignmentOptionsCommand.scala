package uk.ac.warwick.tabula.commands.cm2.assignments

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}


object ModifyAssignmentOptionsCommand {
  def apply(assignment: Assignment) =
    new ModifyAssignmentOptionsCommandInternal(assignment)
      with ComposableCommand[Assignment]
      with ModifyAssignmentOptionsPermissions
      with ModifyAssignmentOptionsDescription
      with ModifyAssignmentOptionsCommandState
      with ModifyAssignmentOptionsValidation
      with AutowiringAssessmentServiceComponent
      with SharedAssignmentOptionsProperties
}

class ModifyAssignmentOptionsCommandInternal(override val assignment: Assignment)
  extends CommandInternal[Assignment] with PopulateOnForm {
  self: AssessmentServiceComponent with ModifyAssignmentOptionsCommandState with SharedAssignmentOptionsProperties =>

  override def applyInternal(): Assignment = {
    this.copyTo(assignment)
    assessmentService.save(assignment)
    assignment
  }

  override def populate(): Unit = {
    copySharedOptionsFrom(assignment)
  }

}

trait ModifyAssignmentOptionsCommandState {
  self: AssessmentServiceComponent with SharedAssignmentOptionsProperties =>

  def assignment: Assignment

  def copyTo(assignment: Assignment): Unit = {
    copySharedOptionsTo(assignment)
  }
}

trait ModifyAssignmentOptionsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ModifyAssignmentOptionsCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    notDeleted(assignment)
    p.PermissionCheck(Permissions.Assignment.Update, assignment.module)
  }
}

trait ModifyAssignmentOptionsDescription extends Describable[Assignment] {
  self: ModifyAssignmentOptionsCommandState with SharedAssignmentOptionsProperties =>

  override lazy val eventName: String = "ModifyAssignmentOptions"

  override def describe(d: Description): Unit = {
    d.assignment(assignment)
    d.properties(
      "minimumFileAttachmentLimit" -> minimumFileAttachmentLimit,
      "maximumFileAttachmentLimit" -> fileAttachmentLimit,
      "individualFileSizeLimit" -> individualFileSizeLimit,
      "fileAttachmentTypes" -> fileAttachmentTypes,
      "wordCountMax" -> wordCountMax,
      "wordCountMin" -> wordCountMin
    )
  }
}

trait ModifyAssignmentOptionsValidation extends SelfValidating {
  self: ModifyAssignmentOptionsCommandState with AssessmentServiceComponent with SharedAssignmentOptionsProperties =>

  override def validate(errors: Errors): Unit = {
    validateSharedOptions(errors)
  }
}
