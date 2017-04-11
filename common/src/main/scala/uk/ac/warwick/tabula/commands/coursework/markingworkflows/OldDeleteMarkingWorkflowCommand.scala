package uk.ac.warwick.tabula.commands.coursework.markingworkflows

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.{MarkingWorkflowDaoComponent, AutowiringMarkingWorkflowDaoComponent}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object OldDeleteMarkingWorkflowCommand {
	def apply(department: Department, markingWorkflow: MarkingWorkflow) = new DeleteMarkingWorkflowCommandInternal(department, markingWorkflow)
		with ComposableCommand[Unit]
		with DeleteMarkingWorkflowCommandState
		with DeleteMarkingWorkflowCommandValidation
		with DeleteMarkingWorkflowPermissions
		with DeleteMarkingWorkflowDescription
		with AutowiringMarkingWorkflowDaoComponent

}

class DeleteMarkingWorkflowCommandInternal(val department: Department, val markingWorkflow: MarkingWorkflow) extends CommandInternal[Unit] {

	override protected def applyInternal(): Unit = {
		transactional() {{
				department.removeMarkingWorkflow(markingWorkflow)
		}}
	}
}

trait DeleteMarkingWorkflowCommandState {
	def department: Department
	def markingWorkflow: MarkingWorkflow
}

trait DeleteMarkingWorkflowCommandValidation extends SelfValidating {
	self: DeleteMarkingWorkflowCommandState with MarkingWorkflowDaoComponent =>

	override def validate(errors: Errors): Unit = {
		// can't delete a markingWorkflow that's being referenced by assignments.
		if (!markingWorkflowDao.getAssignmentsUsingMarkingWorkflow(markingWorkflow).isEmpty) {
			errors.reject("markingWorkflow.assignments.inuse")
		}
		// or exams
		if (!markingWorkflowDao.getExamsUsingMarkingWorkflow(markingWorkflow).isEmpty) {
			errors.reject("markingWorkflow.exams.inuse")
		}
	}
}

trait DeleteMarkingWorkflowPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DeleteMarkingWorkflowCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(markingWorkflow, department)
		p.PermissionCheck(Permissions.MarkingWorkflow.Manage, markingWorkflow)
	}
}

trait DeleteMarkingWorkflowDescription extends Describable[Unit] {
	self: DeleteMarkingWorkflowCommandState =>

	def describe(d: Description) {}

}