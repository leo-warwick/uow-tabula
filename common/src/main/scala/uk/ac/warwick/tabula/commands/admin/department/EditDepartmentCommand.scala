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

object EditDepartmentCommand {
	def apply(department: Department) =
		new EditDepartmentCommandInternal(department)
			with ComposableCommand[Department]
			with EditDepartmentCommandPermissions
			with EditDepartmentCommandDescription
			with EditDepartmentCommandValidation
			with AutowiringModuleAndDepartmentServiceComponent
}

class EditDepartmentCommandInternal(val department: Department) extends CommandInternal[Department] with EditDepartmentCommandState {
	self: ModuleAndDepartmentServiceComponent =>

	fullName = department.fullName
	shortName = department.shortName

	if (department.hasParent) {
		code = department.code
		filterRule = department.filterRule
	}

	def applyInternal(): Department = transactional() {
		department.fullName = fullName
		department.shortName = shortName

		if (department.hasParent) {
			department.code = code
			department.filterRule = filterRule
		}

		moduleAndDepartmentService.saveOrUpdate(department)
		department
	}
}

trait EditDepartmentCommandValidation extends SelfValidating {
	self: EditDepartmentCommandState with ModuleAndDepartmentServiceComponent =>

	def validate(errors: Errors) {
		// Code must be non-empty and start with parent code
		if (department.hasParent) {
			if (!code.hasText) {
				errors.rejectValue("code", "department.code.empty")
			} else if (code != department.code) {
				if (code.endsWith("-")) {
					errors.rejectValue("code", "department.code.mustNotEndWithHypen")
				} else if (!code.startsWith(department.parent.code + "-")) {
					errors.rejectValue("code", "department.code.mustStartWithParent", Array(department.parent.code), "")
				}

				// Code must not exceed 20 characters
				if (code.length > 20) {
					errors.rejectValue("code", "department.code.tooLong", Array(20: java.lang.Integer), "")
				}

				// Code must only include lowercase and hyphen
				if (!code.matches( """[a-z0-9\-]+""")) {
					errors.rejectValue("code", "department.code.badFormat")
				}

				// Code must not already exist
				if (moduleAndDepartmentService.getDepartmentByCode(code).isDefined) {
					errors.rejectValue("code", "department.code.exists")
				}
			}
		}

		// Name must be non-empty and start with parent name
		if (!fullName.hasText) {
			errors.rejectValue("fullName", "department.name.empty")
		} else if (fullName != department.fullName) {
			if (department.hasParent) {
				if (fullName == department.parent.fullName) {
					errors.rejectValue("fullName", "department.name.mustDifferFromParent", Array(department.parent.fullName), "")
				}

				if (!fullName.startsWith(department.parent.fullName)) {
					errors.rejectValue("fullName", "department.name.mustStartWithParent", Array(department.parent.fullName), "")
				}
			}

			// Name must not exceed 100 characters
			if (fullName.length > 100) {
				errors.rejectValue("fullName", "department.name.tooLong", Array(100: java.lang.Integer), "")
			}
		}

		if (shortName.hasText && shortName != department.shortName) {
			if (department.hasParent) {
				if (shortName == department.parent.name) {
					errors.rejectValue("shortName", "department.name.mustDifferFromParent", Array(department.parent.name), "")
				}

				if (!shortName.startsWith(department.parent.name)) {
					errors.rejectValue("shortName", "department.name.mustStartWithParent", Array(department.parent.name), "")
				}
			}

			// Short name must not exceed 100 characters
			if (shortName.length > 100) {
				errors.rejectValue("shortName", "department.name.tooLong", Array(100: java.lang.Integer), "")
			}
		}

		// Filter rule must not be null for sub-departments
		if (department.hasParent) {
			if (filterRule == null) {
				errors.rejectValue("filterRule", "department.filterRule.empty")
			} else if (filterRule != department.filterRule && department.parent.filterRule != null) {
				// Filter rule must not contradict parent rule
				if (filterRule == AllMembersFilterRule && department.parent.filterRule != AllMembersFilterRule) {
					errors.rejectValue("filterRule", "department.filterRule.contradictory")
				}

				if ((filterRule == UndergraduateFilterRule && department.parent.filterRule == PostgraduateFilterRule) ||
					(filterRule == PostgraduateFilterRule && department.parent.filterRule == UndergraduateFilterRule)) {
					errors.rejectValue("filterRule", "department.filterRule.contradictory")
				}
			}
		}
	}
}

trait EditDepartmentCommandState {
	def department: Department

	var code: String = department.code
	var fullName: String = department.fullName
	var shortName: String = department.shortName
	var filterRule: FilterRule = Option(department.filterRule).getOrElse(AllMembersFilterRule)
}

trait EditDepartmentCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditDepartmentCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.Manage, mandatory(department))
	}
}

trait EditDepartmentCommandDescription extends Describable[Department] {
	self: EditDepartmentCommandState =>

	def describe(d: Description) {
		d.department(department)
	}
}