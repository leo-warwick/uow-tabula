package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.roles.UniversityMemberRoleDefinition._
import uk.ac.warwick.userlookup.User

case class LoggedInRole(user: User) extends BuiltInRole(LoggedInRoleDefinition, None)

case object LoggedInRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "Logged in"

	GrantsGlobalPermission(
		Notification.Dismiss, // TAB-1959
		Module.ViewTimetable
	)

	GrantsScopelessPermission(
		UserPicker, // TAB-2951
		MonitoringPointTemplates.View
	)

}
