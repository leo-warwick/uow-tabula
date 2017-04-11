package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.JavaImports

case class ModuleAuditor(module: model.Module) extends BuiltInRole(ModuleAuditorRoleDefinition, module)

case object ModuleAuditorRoleDefinition extends BuiltInRoleDefinition {

	override def description = "Module Auditor"

	GrantsScopedPermission(
		Module.Administer,
		Module.ManageAssignments,
		Module.ManageSmallGroups,

		Assignment.Read,

		Submission.ViewPlagiarismStatus,
		Submission.Read,
		Extension.Read,

		AssignmentFeedback.Read,
		ExamFeedback.Read,

		SmallGroups.Read,
		SmallGroups.ReadMembership,
		SmallGroupEvents.ViewRegister
	)

	def canDelegateThisRolesPermissions: JavaImports.JBoolean = false
}