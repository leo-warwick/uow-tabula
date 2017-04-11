package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.commands.{SelfValidating, Description, Describable, CommandInternal, ComposableCommand}
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.data.model.Department

object DeleteCustomRoleDefinitionCommand {
	def apply(department: Department, customRoleDefinition: CustomRoleDefinition) =
		new DeleteCustomRoleDefinitionCommandInternal(department, customRoleDefinition)
			with ComposableCommand[CustomRoleDefinition]
			with DeleteCustomRoleDefinitionCommandDescription
			with DeleteCustomRoleDefinitionCommandValidation
			with DeleteCustomRoleDefinitionCommandPermissions
			with AutowiringPermissionsServiceComponent
}

trait DeleteCustomRoleDefinitionCommandState {
	def department: Department
	def customRoleDefinition: CustomRoleDefinition
}

class DeleteCustomRoleDefinitionCommandInternal(val department: Department, val customRoleDefinition: CustomRoleDefinition) extends CommandInternal[CustomRoleDefinition] with DeleteCustomRoleDefinitionCommandState {
	self: PermissionsServiceComponent =>

	override def applyInternal(): CustomRoleDefinition = transactional() {
		permissionsService.delete(customRoleDefinition)
		customRoleDefinition
	}
}

trait DeleteCustomRoleDefinitionCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DeleteCustomRoleDefinitionCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(mandatory(customRoleDefinition), mandatory(department))
		p.PermissionCheck(Permissions.RolesAndPermissions.Delete, customRoleDefinition)
	}
}

trait DeleteCustomRoleDefinitionCommandValidation extends SelfValidating {
	self: DeleteCustomRoleDefinitionCommandState with PermissionsServiceComponent =>

	override def validate(errors: Errors) {
		val granted = permissionsService.getAllGrantedRolesForDefinition(customRoleDefinition)
		val derived = permissionsService.getCustomRoleDefinitionsBasedOn(customRoleDefinition)

		// Must not have any derived roles
		if (!granted.isEmpty) errors.reject("customRoleDefinition.delete.hasGrantedRoles")

		// Must not have any active granted roles
		if (!derived.isEmpty) errors.reject("customRoleDefinition.delete.hasDerivedRoles")
	}
}

trait DeleteCustomRoleDefinitionCommandDescription extends Describable[CustomRoleDefinition] {
	self: DeleteCustomRoleDefinitionCommandState =>
	// describe the thing that's happening.
	override def describe(d: Description): Unit =
		d.customRoleDefinition(customRoleDefinition)
}