package uk.ac.warwick.tabula.commands.cm2.assignments

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}


object EditAssignmentDetailsCommand {
	def apply(assignment: Assignment) =
		new EditAssignmentDetailsCommandInternal(assignment)
			with ComposableCommand[Assignment]
			with BooleanAssignmentProperties
			with EditAssignmentPermissions
			with EditAssignmentDetailsDescription
			with EditAssignmentDetailsValidation
			with ModifyAssignmentScheduledNotifications
			with AutowiringAssessmentServiceComponent
			with ModifyAssignmentsDetailsTriggers
			with PopulateOnForm
}

class EditAssignmentDetailsCommandInternal(override val assignment: Assignment)
	extends CommandInternal[Assignment] with EditAssignmentDetailsCommandState with EditAssignmentDetailsValidation with SharedAssignmentProperties with PopulateOnForm  with AssignmentDetailsCopy {

	self: AssessmentServiceComponent =>

	override def applyInternal(): Assignment = {
		copyTo(assignment)
		assessmentService.save(assignment)
		assignment
	}

	override def populate(): Unit = {
		name = assignment.name
		openDate = assignment.openDate
		openEndedReminderDate = assignment.openEndedReminderDate
		closeDate = assignment.closeDate
		workflowCategory = assignment.workflowCategory.getOrElse(WorkflowCategory.NotDecided)

	}

}


trait EditAssignmentDetailsCommandState extends ModifyAssignmentDetailsCommandState {

	self: AssessmentServiceComponent =>

	def assignment: Assignment

	def module: Module = assignment.module

}


trait EditAssignmentDetailsValidation extends ModifyAssignmentDetailsValidation {
	self: EditAssignmentDetailsCommandState with BooleanAssignmentProperties with AssessmentServiceComponent =>

	override def validate(errors: Errors): Unit = {
		if (name != null && name.length < 3000) {
			val duplicates = assessmentService.getAssignmentByNameYearModule(name, academicYear, module).filter { existing => existing.isAlive && !(existing eq assignment) }
			for (duplicate <- duplicates.headOption) {
				errors.rejectValue("name", "name.duplicate.assignment", Array(name), "")
			}
		}
		genericValidate(errors)
	}
}


trait EditAssignmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditAssignmentDetailsCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.Assignment.Update, module)
	}
}

trait EditAssignmentDetailsDescription extends Describable[Assignment] {
	self: EditAssignmentDetailsCommandState =>

	override def describe(d: Description) {
		d.assignment(assignment).properties(
			"name" -> name,
			"openDate" -> openDate,
			"closeDate" -> closeDate)
	}

}
