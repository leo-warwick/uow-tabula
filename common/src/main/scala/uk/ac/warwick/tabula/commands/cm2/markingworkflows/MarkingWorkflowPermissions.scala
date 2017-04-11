package uk.ac.warwick.tabula.commands.cm2.markingworkflows

import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

trait MarkingWorkflowPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	def markingWorkflow: CM2MarkingWorkflow

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MarkingWorkflow.Manage, markingWorkflow)
	}
}

trait MarkingWorkflowDepartmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	def department: Department

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MarkingWorkflow.Manage, department)
	}
}

