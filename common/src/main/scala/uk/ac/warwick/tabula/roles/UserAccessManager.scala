package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data.model.{StudentRelationshipType, Department}
import uk.ac.warwick.tabula.JavaImports
import uk.ac.warwick.tabula.permissions.PermissionsSelector

case class UserAccessManager(department:Department)
	extends BuiltInRole(UserAccessMgrRoleDefinition, department)

case object  UserAccessMgrRoleDefinition
	extends BuiltInRoleDefinition{

	def description: String = "User Access Manager"

	GeneratesSubRole(DepartmentalAdministratorRoleDefinition)
	GeneratesSubRole(StudentRelationshipAgentRoleDefinition(PermissionsSelector.Any[StudentRelationshipType]))

	def canDelegateThisRolesPermissions: JavaImports.JBoolean = true

}