package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._

case class MarksManager() extends BuiltInRole(MarksManagerRoleDefinition, None)

case object MarksManagerRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "Tabula Marks Manager"

	GrantsScopelessPermission(
		Marks.MarksManagement
	)

}