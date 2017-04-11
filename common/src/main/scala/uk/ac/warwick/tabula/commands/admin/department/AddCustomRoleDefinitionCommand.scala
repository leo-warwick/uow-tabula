package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.commands.{Description, Describable, SelfValidating, CommandInternal, ComposableCommand}
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.helpers.StringUtils._

object AddCustomRoleDefinitionCommand {
	def apply(department: Department) =
		new AddCustomRoleDefinitionCommandInternal(department)
			with ComposableCommand[CustomRoleDefinition]
			with AddCustomRoleDefinitionCommandDescription
			with AddCustomRoleDefinitionCommandValidation
			with AddCustomRoleDefinitionCommandPermissions
			with AutowiringPermissionsServiceComponent
}

trait AddCustomRoleDefinitionCommandState {
	def department: Department

	var name: String = _
	var baseDefinition: RoleDefinition = _
}

class AddCustomRoleDefinitionCommandInternal(val department: Department) extends CommandInternal[CustomRoleDefinition] with AddCustomRoleDefinitionCommandState {
	self: PermissionsServiceComponent =>

	override def applyInternal(): CustomRoleDefinition = transactional() {
		val definition = new CustomRoleDefinition
		definition.department = department
		definition.name = name
		definition.baseRoleDefinition = baseDefinition
		definition.canDelegateThisRolesPermissions = true

		permissionsService.saveOrUpdate(definition)
		definition
	}
}

trait AddCustomRoleDefinitionCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AddCustomRoleDefinitionCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.RolesAndPermissions.Create, mandatory(department))
	}
}

trait AddCustomRoleDefinitionCommandValidation extends SelfValidating {
	self: AddCustomRoleDefinitionCommandState =>

	override def validate(errors: Errors) {
		// name must be non-empty
		if (!name.hasText) {
			errors.rejectValue("name", "customRoleDefinition.name.empty")
		} else if (name.length > 200) {
			errors.rejectValue("name", "customRoleDefinition.name.tooLong", Array(200: java.lang.Integer), "Name must be 200 characters or fewer")
		}

		if (baseDefinition == null) errors.rejectValue("baseDefinition", "NotEmpty")
	}
}

trait AddCustomRoleDefinitionCommandDescription extends Describable[CustomRoleDefinition] {
	self: AddCustomRoleDefinitionCommandState =>
	// describe the thing that's happening.
	override def describe(d: Description): Unit =
		d.department(department)
}

