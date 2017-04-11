package uk.ac.warwick.tabula.commands.admin.department

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.{Route, Module, Department}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited, ReadOnly}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object AdminDepartmentHomeCommand {
	def apply(department: Department, user: CurrentUser) =
		new AdminDepartmentHomeCommandInternal(department, user)
				with AutowiringSecurityServiceComponent
				with AutowiringModuleAndDepartmentServiceComponent
				with AutowiringCourseAndRouteServiceComponent
				with AdminDepartmentHomeCommandPermissions
				with ComposableCommand[(Seq[Module], Seq[Route])]
				with ReadOnly with Unaudited
}

class AdminDepartmentHomeCommandInternal(val department: Department, val user: CurrentUser)
	extends CommandInternal[(Seq[Module], Seq[Route])] with AdminDepartmentHomeCommandState {
	self: SecurityServiceComponent with ModuleAndDepartmentServiceComponent with CourseAndRouteServiceComponent =>

	lazy val modules: Seq[Module] =
		if (securityService.can(user, Permissions.Module.Administer, department)) {
			department.modules.asScala
		} else {
			modulesWithPermission.toList
		}

	lazy val routes: Seq[Route] =
		if (securityService.can(user, Permissions.Route.Administer, department)) {
			department.routes.asScala
		} else {
			routesWithPermission.toList
		}

	def applyInternal(): (Seq[Module], Seq[Route]) = (modules.sorted, routes.sorted)
}

trait AdminDepartmentHomeCommandState {
	self: ModuleAndDepartmentServiceComponent with CourseAndRouteServiceComponent =>

	def department: Department
	def user: CurrentUser

	lazy val modulesWithPermission: Set[Module] = moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.Administer, department)
	lazy val routesWithPermission: Set[Route] = courseAndRouteService.routesWithPermission(user, Permissions.Route.Administer, department)
}

trait AdminDepartmentHomeCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: SecurityServiceComponent with ModuleAndDepartmentServiceComponent with AdminDepartmentHomeCommandState =>

	def permissionsCheck(p:PermissionsChecking) {
		val allDeptPermission = Seq(Permissions.Module.Administer, Permissions.Route.Administer).find { requiredPermission =>
			securityService.can(user, requiredPermission, mandatory(department))
		}

		allDeptPermission match {
			case Some(requiredPermission) =>
				// This may seem silly because it's rehashing the above; but it avoids an assertion error where we don't have any explicit permission definitions
				p.PermissionCheck(requiredPermission, department)
			case None =>
				// User has no dept-level permission, check modules and routes
				if (modulesWithPermission.nonEmpty) {
					p.PermissionCheckAll(Permissions.Module.Administer, modulesWithPermission)
				} else if (routesWithPermission.nonEmpty) {
					p.PermissionCheckAll(Permissions.Route.Administer, routesWithPermission)
				} else {
					// The user doesn't have permissions. Just require it on the dept, we know it'll fail
					p.PermissionCheck(Permissions.Module.Administer, department)
				}
		}
	}
}
