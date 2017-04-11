package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroup}
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.roles.SmallGroupTutorRoleDefinition._

case class SmallGroupEventTutor(smallGroupEvent: SmallGroupEvent) extends BuiltInRole(SmallGroupEventTutorRoleDefinition, smallGroupEvent)
case class SmallGroupTutor(smallGroup: SmallGroup) extends BuiltInRole(SmallGroupTutorRoleDefinition, smallGroup)

case object SmallGroupEventTutorRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "SmallGroupEventTutor"

	GrantsScopedPermission(
		SmallGroupEvents.Register
	)

}

case object SmallGroupTutorRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "SmallGroupTutor"

	GrantsScopedPermission(
		SmallGroups.Read,
		SmallGroups.ReadMembership,
		SmallGroupEvents.ViewRegister,

		// Extra permissions on any students in these groups for their profile
		Profiles.Read.Usercode,
		Profiles.Read.StudentCourseDetails.Core,
		Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]),
		Profiles.Read.StudentCourseDetails.Status,
		Profiles.Read.Timetable,
		Profiles.Read.SmallGroups,
		Profiles.Read.Disability, // TAB-4386
		Profiles.Read.Photo
	)

}