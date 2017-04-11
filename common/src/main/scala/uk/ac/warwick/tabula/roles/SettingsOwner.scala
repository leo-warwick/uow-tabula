package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._

import uk.ac.warwick.tabula.permissions.Permissions._

case class SettingsOwner(settings: model.UserSettings) extends BuiltInRole(SettingsOwnerRoleDefinition, settings)

object SettingsOwnerRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "User Settings Owner"

	GrantsScopedPermission(
		UserSettings.Update
	)

}