package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.commands.{Description, Describable, SelfValidating, CommandInternal, ComposableCommand}
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.data.model.permissions.{RoleOverride, CustomRoleDefinition}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import scala.collection.JavaConverters._

object AddCustomRoleOverrideCommand {
	def apply(department: Department, customRoleDefinition: CustomRoleDefinition) =
		new AddCustomRoleOverrideCommandInternal(department, customRoleDefinition)
			with ComposableCommand[RoleOverride]
			with AddCustomRoleOverrideCommandDescription
			with AddCustomRoleOverrideCommandValidation
			with AddCustomRoleOverrideCommandPermissions
			with AutowiringPermissionsServiceComponent
}

trait AddCustomRoleOverrideCommandState {
	def department: Department
	def customRoleDefinition: CustomRoleDefinition

	var permission: Permission = _
	var overrideType: Boolean = _
}

class AddCustomRoleOverrideCommandInternal(val department: Department, val customRoleDefinition: CustomRoleDefinition) extends CommandInternal[RoleOverride] with AddCustomRoleOverrideCommandState {
	self: PermissionsServiceComponent =>

	override def applyInternal(): RoleOverride = transactional() {
		val o = new RoleOverride
		o.customRoleDefinition = customRoleDefinition
		o.permission = permission
		o.overrideType = overrideType

		customRoleDefinition.overrides.add(o)

		permissionsService.saveOrUpdate(customRoleDefinition)
		o
	}
}

trait AddCustomRoleOverrideCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AddCustomRoleOverrideCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(mandatory(customRoleDefinition), mandatory(department))
		p.PermissionCheck(Permissions.RolesAndPermissions.Create, customRoleDefinition)
	}
}

trait AddCustomRoleOverrideCommandValidation extends SelfValidating {
	self: AddCustomRoleOverrideCommandState =>
	import RoleOverride._

	override def validate(errors: Errors) {
		// permission must be non empty
		if (permission == null) errors.rejectValue("permission", "NotEmpty")
		else {
			val allRolePermissions = customRoleDefinition.permissions(Some(null))

			// Must not be revoking an existing override
			if (customRoleDefinition.overrides.asScala.exists { o => o.permission == permission }) {
				errors.rejectValue("permission", "customRoleDefinition.override.existingOverride")
			}

			if (overrideType == Allow) {
				// If override is Add, it must not already be a permission
				if (allRolePermissions.exists { case (p, _) => p == permission }) {
					errors.rejectValue("permission", "customRoleDefinition.override.alreadyAllowed")
				}
			} else {
				// Must be an existing permission to revoke
				if (!allRolePermissions.exists { case (p, _) => p == permission }) {
					errors.rejectValue("permission", "customRoleDefinition.override.notAllowed")
				}
			}
		}
	}
}

trait AddCustomRoleOverrideCommandDescription extends Describable[RoleOverride] {
	self: AddCustomRoleOverrideCommandState =>
	// describe the thing that's happening.
	override def describe(d: Description): Unit =
		d.customRoleDefinition(customRoleDefinition)
}

