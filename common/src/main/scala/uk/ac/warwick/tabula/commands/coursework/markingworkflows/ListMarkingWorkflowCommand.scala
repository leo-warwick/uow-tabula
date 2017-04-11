package uk.ac.warwick.tabula.commands.coursework.markingworkflows

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.MarkingWorkflowDaoComponent
import uk.ac.warwick.tabula.data.model.{MarkingWorkflow, Department}
import uk.ac.warwick.tabula.services.{AutowiringMarkingWorkflowServiceComponent, MarkingWorkflowServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.Errors

object ListMarkingWorkflowCommand {
	def apply(department: Department, isExam: Boolean) =
		new ListMarkingWorkflowCommandInternal(department, isExam)
			with AutowiringMarkingWorkflowServiceComponent
			with ComposableCommand[Seq[ListMarkingWorkflowCommandResult]]
			with ListMarkingWorkflowPermissions
			with ListMarkingWorkflowCommandState
			with ReadOnly with Unaudited
}

case class ListMarkingWorkflowCommandResult(
	markingWorkflow: MarkingWorkflow,
	assignmentCount: Int,
	examCount: Int
)


class ListMarkingWorkflowCommandInternal(val department: Department, val isExam: Boolean) extends CommandInternal[Seq[ListMarkingWorkflowCommandResult]] {

	self: MarkingWorkflowServiceComponent =>

	override def applyInternal(): Seq[ListMarkingWorkflowCommandResult] = {
		val validWorkflows = if (isExam) department.markingWorkflows.filter(_.validForExams) else department.markingWorkflows
		validWorkflows.map { markingWorkflow =>
			val assignments = markingWorkflowService.getAssignmentsUsingMarkingWorkflow(markingWorkflow)
			val exams = markingWorkflowService.getExamsUsingMarkingWorkflow(markingWorkflow)
			ListMarkingWorkflowCommandResult(markingWorkflow, assignments.size, exams.size)
		}
	}

}

trait ListMarkingWorkflowPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ListMarkingWorkflowCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MarkingWorkflow.Read, department)
	}

}

trait ListMarkingWorkflowCommandState {
	def department: Department
	def isExam: Boolean
}
