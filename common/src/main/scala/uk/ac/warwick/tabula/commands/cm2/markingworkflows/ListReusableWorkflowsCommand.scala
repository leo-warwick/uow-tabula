package uk.ac.warwick.tabula.commands.cm2.markingworkflows

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.services.{AutoWiringCM2MarkingWorkflowServiceComponent, CM2MarkingWorkflowService, CM2MarkingWorkflowServiceComponent}


object ListReusableWorkflowsCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new ListReusableWorkflowsCommandInternal(department, academicYear)
			with ComposableCommand[Seq[CM2MarkingWorkflow]]
			with ListReusableWorkflowsState
			with MarkingWorkflowDepartmentPermissions
			with AutoWiringCM2MarkingWorkflowServiceComponent
			with Unaudited
}

class ListReusableWorkflowsCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[Seq[CM2MarkingWorkflow]] with ListReusableWorkflowsState {

	this: CM2MarkingWorkflowServiceComponent  =>

	def applyInternal() = {
		cm2MarkingWorkflowService.getReusableWorkflows(department, academicYear)
	}
}

trait ListReusableWorkflowsState {
	def department: Department
	def academicYear: AcademicYear
}