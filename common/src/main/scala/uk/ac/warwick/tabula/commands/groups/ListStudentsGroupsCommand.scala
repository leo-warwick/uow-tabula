package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.groups.web.views.GroupsDisplayHelper
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.ViewModules
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

object ListStudentsGroupsCommand {
	def apply(member: Member, user: CurrentUser, academicYearOption: Option[AcademicYear]) =
		new ListStudentsGroupsCommandInternal(member, user, academicYearOption)
			with ComposableCommand[ViewModules]
			with ListStudentsGroupsCommandPermissions
			with AutowiringSmallGroupServiceComponent
			with Unaudited with ReadOnly
}

/**
 * Gets the data for a students view of all small groups they're a member of.
 */
class ListStudentsGroupsCommandInternal(val member: Member, val currentUser: CurrentUser, val academicYearOption: Option[AcademicYear])
	extends CommandInternal[ViewModules] with ListStudentsGroupsCommandState {
	self: SmallGroupServiceComponent =>

	import GroupsDisplayHelper._

	def applyInternal(): ViewModules = {
		val user = member.asSsoUser
		val memberGroupSets = (smallGroupService.findSmallGroupSetsByMember(user) ++ smallGroupService.findSmallGroupsByStudent(user).map { _.groupSet }).distinct
		val filteredmemberGroupSets = academicYearOption.map(academicYear => memberGroupSets.filter(_.academicYear == academicYear)).getOrElse(memberGroupSets)
		val releasedMemberGroupSets = getGroupSetsReleasedToStudents(filteredmemberGroupSets)
		val isTutor = !(currentUser.apparentUser == user)
		val nonEmptyMemberViewModules = getViewModulesForStudent(releasedMemberGroupSets, getGroupsToDisplay(_, user, isTutor))

		ViewModules(nonEmptyMemberViewModules.sortBy(_.module.code), canManageDepartment = false)
	}

}

trait ListStudentsGroupsCommandState {
	def member: Member
}

trait ListStudentsGroupsCommandPermissions extends RequiresPermissionsChecking {
	self: ListStudentsGroupsCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.Read.SmallGroups, member)
	}
}
