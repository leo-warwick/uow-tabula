package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.commands.{ComposableCommand, ReadOnly, Unaudited, CommandInternal}
import uk.ac.warwick.tabula.data.model.permissions.{RoleOverride, CustomRoleDefinition}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import ListCustomRoleOverridesCommand._
import uk.ac.warwick.tabula.roles.{Role, RoleBuilder}
import scala.collection.JavaConverters._

object ListCustomRoleOverridesCommand {
	case class CustomRoleOverridesInfo(role: Role, overrides: Seq[RoleOverride])

	def apply(department: Department, customRoleDefinition: CustomRoleDefinition) =
		new ListCustomRoleOverridesCommandInternal(department, customRoleDefinition)
			with ComposableCommand[CustomRoleOverridesInfo]
			with ListCustomRoleOverridesCommandPermissions
			with ReadOnly with Unaudited
}

trait ListCustomRoleOverridesCommandState {
	def department: Department
	def customRoleDefinition: CustomRoleDefinition
}

class ListCustomRoleOverridesCommandInternal(val department: Department, val customRoleDefinition: CustomRoleDefinition) extends CommandInternal[CustomRoleOverridesInfo] with ListCustomRoleOverridesCommandState {
	override def applyInternal(): CustomRoleOverridesInfo = {
		// Use Some(null) instead of None so we show scoped permissions too
		val role = RoleBuilder.build(customRoleDefinition, Some(null), customRoleDefinition.name)
		val overrides = customRoleDefinition.overrides.asScala

		CustomRoleOverridesInfo(role, overrides)
	}
}

trait ListCustomRoleOverridesCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ListCustomRoleOverridesCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(mandatory(customRoleDefinition), mandatory(department))
		p.PermissionCheck(Permissions.RolesAndPermissions.Read, customRoleDefinition)
	}
}