package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

case class DepartmentRouteManager(department: model.Department) extends BuiltInRole(RouteManagerRoleDefinition, department)
case class RouteManager(route: model.Route) extends BuiltInRole(RouteManagerRoleDefinition, route)

case object RouteManagerRoleDefinition extends BuiltInRoleDefinition {

	override def description = "Route Manager"

	GeneratesSubRole(RouteAssistantRoleDefinition)

	GrantsScopedPermission(
		MonitoringPoints.Manage
	)
	def canDelegateThisRolesPermissions: JBoolean = true
}