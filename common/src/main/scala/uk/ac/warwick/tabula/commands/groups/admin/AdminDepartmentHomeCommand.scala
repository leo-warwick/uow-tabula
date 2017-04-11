package uk.ac.warwick.tabula.commands.groups.admin

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.{Module, Department}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Unaudited, ReadOnly}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.services.SecurityServiceComponent
import uk.ac.warwick.tabula.services.ModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.services.AutowiringSecurityServiceComponent
import uk.ac.warwick.tabula.services.AutowiringModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.permissions.Permission

object AdminDepartmentHomeCommand {
	def apply(department: Department, user: CurrentUser) =
		new AdminDepartmentHomeCommand(department, user)
			with ManageGroupsPermissionDefinition
			with AdminDepartmentHomePermissions
			with AutowiringSecurityServiceComponent
			with AutowiringModuleAndDepartmentServiceComponent
			with AllModuleFilter
			with ComposableCommand[Seq[Module]]
			with ReadOnly with Unaudited
}


/**
 * Displays groups in a given department.
 */
class AdminDepartmentHomeCommand(val department: Department, val user: CurrentUser) extends CommandInternal[Seq[Module]] with AdminDepartmentHomeState {
	self: SecurityServiceComponent with ModuleAndDepartmentServiceComponent with AdminDepartmentHomePermissionDefinition with ModuleFilter =>

	lazy val modules: Seq[Module] =
		if (securityService.can(user, requiredPermission, department)) {
			department.modules.asScala
		} else {
			modulesWithPermission.toList.sorted
		}

	def applyInternal(): Seq[Module] = modules.filter(moduleFilter).sortBy { (module) => (module.groupSets.isEmpty, module.code) }

}

trait AdminDepartmentHomePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: SecurityServiceComponent with ModuleAndDepartmentServiceComponent with AdminDepartmentHomeState with AdminDepartmentHomePermissionDefinition =>

	def permissionsCheck(p:PermissionsChecking) {
		if (securityService.can(user, requiredPermission, mandatory(department))) {
			// This may seem silly because it's rehashing the above; but it avoids an assertion error where we don't have any explicit permission definitions
			p.PermissionCheck(requiredPermission, department)
		} else {
			val managedModules = modulesWithPermission.toList

			// This is implied by the above, but it's nice to check anyway. Avoid exception if there are no managed modules
			if (!managedModules.isEmpty) p.PermissionCheckAll(requiredPermission, managedModules)
			else p.PermissionCheck(requiredPermission, department)
		}
	}
}

trait AdminDepartmentHomeState {
	self: ModuleAndDepartmentServiceComponent with AdminDepartmentHomePermissionDefinition =>

	def department: Department
	def user: CurrentUser

	lazy val modulesWithPermission: Set[Module] = moduleAndDepartmentService.modulesWithPermission(user, requiredPermission, department)
}

trait AdminDepartmentHomePermissionDefinition {
	def requiredPermission: Permission
}

trait ManageGroupsPermissionDefinition extends AdminDepartmentHomePermissionDefinition {
	val requiredPermission = Permissions.Module.ManageSmallGroups
}

trait ModuleFilter {
	def moduleFilter(module: Module): Boolean
}

trait AllModuleFilter extends ModuleFilter {
	def moduleFilter(module: Module) = true
}