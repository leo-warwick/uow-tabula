package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User


object SmallGroupSetTimetableClashCommand {
	def apply(smallGroupSet: SmallGroupSet): SmallGroupSetTimetableClashCommandInternal with ComposableCommand[Seq[(SmallGroup, Seq[User])]] with SmallGroupSetTimetableClashCommandPermissions with AutowiringSmallGroupServiceComponent with Unaudited with ReadOnly = {
		new SmallGroupSetTimetableClashCommandInternal(smallGroupSet)
			with ComposableCommand[Seq[(SmallGroup, Seq[User])]]
			with SmallGroupSetTimetableClashCommandPermissions
			with AutowiringSmallGroupServiceComponent
			with Unaudited with ReadOnly
	}
}

class SmallGroupSetTimetableClashCommandInternal(val smallGroupSet: SmallGroupSet)
	extends CommandInternal[Seq[(SmallGroup, Seq[User])]] with SmallGroupSetTimetableClashCommandState {
	self: SmallGroupServiceComponent =>

	override def applyInternal(): Seq[(SmallGroup, Seq[User])] = {
		smallGroupService.findPossibleTimetableClashesForGroupSet(smallGroupSet)
	}

}

trait SmallGroupSetTimetableClashCommandState {
	val smallGroupSet: SmallGroupSet
}

trait SmallGroupSetTimetableClashCommandPermissions extends RequiresPermissionsChecking {
	self: SmallGroupSetTimetableClashCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.ReadMembership, smallGroupSet)
	}
}