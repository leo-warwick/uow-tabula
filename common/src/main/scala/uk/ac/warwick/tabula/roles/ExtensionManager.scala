package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.JavaImports

case class ExtensionManager(department: Department) extends BuiltInRole(ExtensionManagerRoleDefinition, department)

case object ExtensionManagerRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "Extension Manager"

	GeneratesSubRole(ModuleAuditorRoleDefinition)

	GrantsScopedPermission(
		Extension.Create,
		Extension.Update,
		Extension.Read
	)

}