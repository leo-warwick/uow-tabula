package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.roles.ModuleManagerRoleDefinition._

case class UniversityMemberRole(member: model.Member) extends BuiltInRole(UniversityMemberRoleDefinition, member)

case object UniversityMemberRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "University Member"

	GeneratesSubRole(PreviousUniversityMemberRoleDefinition)

	// As per discussion in TAB-753, anyone at the University can see anyone else's core information
	GrantsGlobalPermission(
		Profiles.Read.Core
	)

	GrantsScopedPermission(
		Profiles.Read.Photo,
    Profiles.MeetingRecord.Manage(PermissionsSelector.Any[StudentRelationshipType]),
		Profiles.ScheduledMeetingRecord.Manage(PermissionsSelector.Any[StudentRelationshipType])
	)

	GrantsScopelessPermission(
		UserPicker
	)
}
