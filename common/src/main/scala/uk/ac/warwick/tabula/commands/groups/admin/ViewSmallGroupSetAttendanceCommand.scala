package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.commands.groups.ViewSmallGroupAttendanceCommand

import scala.collection.JavaConverters._
import scala.collection.immutable.SortedMap
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import ViewSmallGroupAttendanceCommand._
import uk.ac.warwick.tabula.ItemNotFoundException

object ViewSmallGroupSetAttendanceCommand {
	def apply(set: SmallGroupSet) =
		new ViewSmallGroupSetAttendanceCommand(set)
			with ComposableCommand[SortedMap[SmallGroup, SmallGroupAttendanceInformation]]
			with ViewSmallGroupSetAttendancePermissions
			with ReadOnly with Unaudited {
		override lazy val eventName = "ViewSmallGroupSetAttendance"
	}
}

class ViewSmallGroupSetAttendanceCommand(val set: SmallGroupSet)
	extends CommandInternal[SortedMap[SmallGroup, SmallGroupAttendanceInformation]] with ViewSmallGroupSetAttendanceState {

	if (!set.collectAttendance) throw new ItemNotFoundException

	override def applyInternal(): SortedMap[SmallGroup, SmallGroupAttendanceInformation] = {
		SortedMap(set.groups.asScala.map { group =>
			(group -> ViewSmallGroupAttendanceCommand(group).apply())
		}.toSeq:_*)
	}

}

trait ViewSmallGroupSetAttendancePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewSmallGroupSetAttendanceState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroupEvents.ViewRegister, set)
	}
}

trait ViewSmallGroupSetAttendanceState {
	def set: SmallGroupSet
}