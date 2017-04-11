package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{Department, Route}
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringCourseAndRouteServiceComponent, AutowiringSecurityServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods

import scala.collection.JavaConverters._

trait PermissionsAwareRoutes {
	def routesForPermission(user: CurrentUser, p: Permission, dept: Department): Set[Route]
}

/**
 * When including this trait, make sure it is with'd BEFORE ComposableCommand
 * Otherwise all the Wire'd objects are null
 */
trait AutowiringSecurityServicePermissionsAwareRoutes
	extends PermissionsAwareRoutes
		with PermissionsCheckingMethods
		with AutowiringSecurityServiceComponent
		with AutowiringCourseAndRouteServiceComponent {

	def routesForPermission(user: CurrentUser, p: Permission, dept: Department): Set[Route] = {
		if (securityService.can(user, p, mandatory(dept))) dept.routes.asScala.toSet
		else courseAndRouteService.routesWithPermission(user, p, dept)
	}

}