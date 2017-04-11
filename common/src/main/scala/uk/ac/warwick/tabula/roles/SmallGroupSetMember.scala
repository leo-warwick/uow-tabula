package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions

case class SmallGroupSetMember(smallGroupSet:SmallGroupSet) extends BuiltInRole(SmallGroupSetMemberRoleDefinition, smallGroupSet)

case object SmallGroupSetMemberRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "Member of a set of small groups"

	GrantsScopedPermission(
		Permissions.SmallGroups.Read,
		Permissions.SmallGroups.AllocateSelf
	)

}

case class SmallGroupSetViewer(smallGroupSet: SmallGroupSet) extends BuiltInRole(SmallGroupSetViewerRoleDefinition, smallGroupSet)

case object SmallGroupSetViewerRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "Viewer of the membership of small groups"

	GrantsScopedPermission(
		Permissions.SmallGroups.ReadMembership
	)

}