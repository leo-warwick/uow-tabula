package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.commands.{Description, Describable, CommandInternal, ComposableCommand}
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.data.model.Department

object EditCustomRoleDefinitionCommand {
	def apply(department: Department, customRoleDefinition: CustomRoleDefinition) =
		new EditCustomRoleDefinitionCommandInternal(department, customRoleDefinition)
			with ComposableCommand[CustomRoleDefinition]
			with EditCustomRoleDefinitionCommandDescription
			with EditCustomRoleDefinitionCommandValidation
			with EditCustomRoleDefinitionCommandPermissions
			with AutowiringPermissionsServiceComponent
}

trait EditCustomRoleDefinitionCommandState extends AddCustomRoleDefinitionCommandState {
	def customRoleDefinition: CustomRoleDefinition
}

class EditCustomRoleDefinitionCommandInternal(val department: Department, val customRoleDefinition: CustomRoleDefinition) extends CommandInternal[CustomRoleDefinition] with EditCustomRoleDefinitionCommandState {
	self: PermissionsServiceComponent =>

	name = customRoleDefinition.name
	baseDefinition = customRoleDefinition.baseRoleDefinition

	override def applyInternal(): CustomRoleDefinition = transactional() {
		customRoleDefinition.name = name
		customRoleDefinition.baseRoleDefinition = baseDefinition

		permissionsService.saveOrUpdate(customRoleDefinition)
		customRoleDefinition
	}
}

trait EditCustomRoleDefinitionCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditCustomRoleDefinitionCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(mandatory(customRoleDefinition), mandatory(department))
		p.PermissionCheck(Permissions.RolesAndPermissions.Update, customRoleDefinition)
	}
}

trait EditCustomRoleDefinitionCommandValidation extends AddCustomRoleDefinitionCommandValidation {
	self: EditCustomRoleDefinitionCommandState =>

	override def validate(errors: Errors) {
		super.validate(errors)

		if (baseDefinition == customRoleDefinition) {
			errors.rejectValue("baseDefinition", "customRoleDefinition.baseIsSelf")
		}
	}
}

trait EditCustomRoleDefinitionCommandDescription extends Describable[CustomRoleDefinition] {
	self: EditCustomRoleDefinitionCommandState =>
	// describe the thing that's happening.
	override def describe(d: Description): Unit =
		d.customRoleDefinition(customRoleDefinition)
}