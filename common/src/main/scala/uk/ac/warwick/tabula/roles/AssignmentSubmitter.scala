package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.JavaImports

case class AssignmentSubmitter(assignment: model.Assignment) extends BuiltInRole(AssignmentSubmitterRoleDefinition, assignment)

case object AssignmentSubmitterRoleDefinition extends UnassignableBuiltInRoleDefinition {
	override def description = "Enrolled On Assignment"

	GrantsScopedPermission(
		Submission.Create,
		Extension.MakeRequest
	)
}