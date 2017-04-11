package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.JavaImports
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

case class RouteAssistant(route: model.Route) extends BuiltInRole(RouteAssistantRoleDefinition, route)

case object RouteAssistantRoleDefinition extends BuiltInRoleDefinition {

	override def description = "Route Assistant"

	GeneratesSubRole(RouteAuditorRoleDefinition)

	GrantsScopedPermission(
		MonitoringPoints.Record,

		Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]),
		Profiles.StudentRelationship.Manage(PermissionsSelector.Any[StudentRelationshipType]),

		Profiles.Read.Tier4VisaRequirement,

		MemberNotes.Create
	)

	def canDelegateThisRolesPermissions: JavaImports.JBoolean = true
}