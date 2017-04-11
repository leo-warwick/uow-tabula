package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._

case class Masquerader() extends BuiltInRole(MasqueraderRoleDefinition, None)

case object MasqueraderRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "Tabula Masquerader (Global)"

	GrantsGlobalPermission(
		Masquerade
	)

}