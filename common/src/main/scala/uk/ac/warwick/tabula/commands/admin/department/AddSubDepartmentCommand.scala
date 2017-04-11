package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Department._
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.Describable
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services.ModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.services.AutowiringModuleAndDepartmentServiceComponent
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.helpers.StringUtils._

object AddSubDepartmentCommand {
	def apply(parent: Department) =
		new AddSubDepartmentCommandInternal(parent)
			with ComposableCommand[Department]
			with AddSubDepartmentCommandPermissions
			with AddSubDepartmentCommandDescription
			with AddSubDepartmentCommandValidation
			with AutowiringModuleAndDepartmentServiceComponent
}

class AddSubDepartmentCommandInternal(val parent: Department) extends CommandInternal[Department] with AddSubDepartmentCommandState {
	self: ModuleAndDepartmentServiceComponent =>

	def applyInternal(): Department = transactional() {
		val d = new Department
		d.code = code
		d.fullName = name
		d.filterRule = filterRule
		d.parent = parent

		// save settings first
		d.copySettingsFrom(parent)
		moduleAndDepartmentService.saveOrUpdate(d)

		// now that the new department has been persisted add the extension managers built in role
		d.copyExtensionManagersFrom(parent)
		moduleAndDepartmentService.saveOrUpdate(d)
		d
	}
}

trait AddSubDepartmentCommandValidation extends SelfValidating {
	self: AddSubDepartmentCommandState with ModuleAndDepartmentServiceComponent =>

	def validate(errors: Errors) {
		// Code must be non-empty and start with parent code
		if (!code.hasText) {
			errors.rejectValue("code", "department.code.empty")
		} else {
			if (code.endsWith("-")) {
				errors.rejectValue("code", "department.code.mustNotEndWithHypen")
			} else if (!code.startsWith(parent.code + "-")) {
				errors.rejectValue("code", "department.code.mustStartWithParent", Array(parent.code), "")
			}

			// Code must not exceed 20 characters
			if (code.length > 20) {
				errors.rejectValue("code", "department.code.tooLong", Array(20: java.lang.Integer), "")
			}

			// Code must only include lowercase and hyphen
			if (!code.matches("""[a-z0-9\-]+""")) {
				errors.rejectValue("code", "department.code.badFormat")
			}

			// Code must not already exist
			if (moduleAndDepartmentService.getDepartmentByCode(code).isDefined) {
				errors.rejectValue("code", "department.code.exists")
			}
		}

		// Name must be non-empty and start with parent name
		if (!name.hasText) {
			errors.rejectValue("name", "department.name.empty")
		} else {
			if (name == parent.name){
				errors.rejectValue("name", "department.name.mustDifferFromParent", Array(parent.name), "")
			}

			// Remove this requirement TAB-2498
//			if (!name.startsWith(parent.name)) {
//				errors.rejectValue("name", "department.name.mustStartWithParent", Array(parent.name), "")
//			}

			// Name must not exceed 100 characters
			if (name.length > 100) {
				errors.rejectValue("name", "department.name.tooLong", Array(100: java.lang.Integer), "")
			}
		}

		// Filter rule must not be null
		if (filterRule == null) {
			errors.rejectValue("filterRule", "department.filterRule.empty")
		} else if (parent.filterRule != null) {
			// Filter rule must not contradict parent rule
			if (filterRule == AllMembersFilterRule && parent.filterRule != AllMembersFilterRule) {
				errors.rejectValue("filterRule", "department.filterRule.contradictory")
			}

			if ((filterRule == UndergraduateFilterRule && parent.filterRule == PostgraduateFilterRule) ||
				(filterRule == PostgraduateFilterRule && parent.filterRule == UndergraduateFilterRule)) {
				errors.rejectValue("filterRule", "department.filterRule.contradictory")
			}
		}
	}
}

trait AddSubDepartmentCommandState {
	def parent: Department

	var code: String = parent.code + "-"
	var name: String = parent.name + " "
	var filterRule: FilterRule = Option(parent.filterRule).getOrElse(AllMembersFilterRule)
}

trait AddSubDepartmentCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AddSubDepartmentCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.Manage, mandatory(parent))
	}
}

trait AddSubDepartmentCommandDescription extends Describable[Department] {
	self: AddSubDepartmentCommandState =>

	def describe(d: Description) {
		d.department(parent)
	}
}