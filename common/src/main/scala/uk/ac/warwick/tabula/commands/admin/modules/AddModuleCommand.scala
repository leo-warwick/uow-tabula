package uk.ac.warwick.tabula.commands.admin.modules

import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import collection.JavaConversions._
import org.springframework.validation.{ ValidationUtils, Errors }
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.helpers.StringUtils._

object AddModuleCommand {
	def apply(department: Department) =
		new AddModuleCommandInternal(department)
				with ComposableCommand[Module]
				with AddModuleCommandPermissions
				with AddModuleCommandValidation
				with AddModuleCommandDescription
				with AutowiringModuleAndDepartmentServiceComponent
}

class AddModuleCommandInternal(val department: Department) extends CommandInternal[Module] with AddModuleCommandState {
	self: ModuleAndDepartmentServiceComponent =>

	def applyInternal(): Module = transactional() {
		val module = new Module()
		module.adminDepartment = department
		module.name = name
		module.code = sanitisedCode

		moduleAndDepartmentService.saveOrUpdate(module)
		module
	}

}

trait AddModuleCommandValidation extends SelfValidating {
	self: AddModuleCommandState with ModuleAndDepartmentServiceComponent =>

	def validate(errors: Errors) {
		// TODO proper department checks before we open this up to non-sysadmins

		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "code", "NotEmpty")
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "NotEmpty")

		if (code.hasText && !code.matches("^[a-z0-9][a-z0-9\\-]*[a-z0-9]$")) {
			errors.rejectValue("code", "code.invalid.module")
		}

		// check for duplicate name or code
		if (!errors.hasErrors) {
			if (moduleAndDepartmentService.getModuleByCode(sanitisedCode).isDefined) {
				errors.rejectValue("code", "code.duplicate.module", Array(code.toUpperCase), "")
			}
			if (department.modules.exists { _.name equalsIgnoreCase name }) {
				errors.rejectValue("name", "name.duplicate.module", Array(name), "")
			}
		}
	}
}

trait AddModuleCommandState {
	def department: Department

	var code: String = _
	var name: String = _

	def sanitisedCode: String = Option(code).map(_.toLowerCase.trim).orNull
}

trait AddModuleCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AddModuleCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Module.Create, mandatory(department))
	}
}

trait AddModuleCommandDescription extends Describable[Module] {
	self: AddModuleCommandState =>

	def describe(d: Description): Unit =
		d.department(department)
		 .properties(
				"moduleCode" -> sanitisedCode,
				"moduleName" -> name
			)
}