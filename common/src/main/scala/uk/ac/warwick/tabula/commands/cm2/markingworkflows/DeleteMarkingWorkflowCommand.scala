package uk.ac.warwick.tabula.commands.cm2.markingworkflows

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.{Describable, Description, _}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow._
import uk.ac.warwick.tabula.services.{AutoWiringCM2MarkingWorkflowServiceComponent, CM2MarkingWorkflowServiceComponent}


import scala.collection.JavaConverters._

object DeleteMarkingWorkflowCommand {

	type Command = Appliable[String] with DeleteMarkingWorkflowState

	def apply(department:Department, markingWorkflow: CM2MarkingWorkflow) =
		new DeleteMarkingWorkflowCommandInternal(department, markingWorkflow)
			with ComposableCommand[String]
			with DeleteMarkingWorkflowValidation
			with MarkingWorkflowPermissions
			with DeleteMarkingWorkflowDescription
			with DeleteMarkingWorkflowState
			with AutoWiringCM2MarkingWorkflowServiceComponent

}

class DeleteMarkingWorkflowCommandInternal(val department: Department, val markingWorkflow: CM2MarkingWorkflow)
	extends CommandInternal[String] {

	self: DeleteMarkingWorkflowState with CM2MarkingWorkflowServiceComponent =>

	def applyInternal() = {
		val name = markingWorkflow.name
		cm2MarkingWorkflowService.delete(markingWorkflow)
		name
	}
}

trait DeleteMarkingWorkflowValidation extends SelfValidating {
	self: DeleteMarkingWorkflowState =>
	def validate(errors: Errors) {
		if (markingWorkflow.assignments.asScala.nonEmpty) {
			errors.reject("workflow.cantDelete")
		}
	}
}

trait DeleteMarkingWorkflowDescription extends Describable[String] {
	self: DeleteMarkingWorkflowState =>
	def describe(d: Description) {
		d.department(department)
		d.markingWorkflow(markingWorkflow)
	}
}

trait DeleteMarkingWorkflowState {
	def department: Department
	def markingWorkflow: CM2MarkingWorkflow
}