package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentServiceComponent, AutowiringAssessmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}


object SearchAssignmentsCommand {
	def apply(module: Module) =
		new SearchAssignmentsCommandInternal(module)
			with ComposableCommand[Seq[Assignment]]
			with SearchAssignmentCommandState
			with AutowiringAssessmentServiceComponent
			with SearchAssignmentsPermissions
			with ReadOnly
			with Unaudited

}

class SearchAssignmentsCommandInternal(val module: Module)
	extends CommandInternal[Seq[Assignment]] with SearchAssignmentCommandState {

	self: AssessmentServiceComponent =>

	override def applyInternal(): Seq[Assignment] =
		assessmentService.getAssignmentsByName(query, module.adminDepartment)
}

trait SearchAssignmentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: SearchAssignmentCommandState =>
	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.Assignment.Read, module)
	}
}


trait SearchAssignmentCommandState {
	var query: String = _

	def module: Module

}
