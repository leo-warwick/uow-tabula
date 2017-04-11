package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.permissions.Permissions._

case class SmallGroupMember(smallGroup: SmallGroup) extends BuiltInRole(SmallGroupMemberRoleDefinition, smallGroup)

case object SmallGroupMemberRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "Small group member"

	GrantsScopedPermission(
		SmallGroups.Read,
		SmallGroups.ReadMembership
	)

}