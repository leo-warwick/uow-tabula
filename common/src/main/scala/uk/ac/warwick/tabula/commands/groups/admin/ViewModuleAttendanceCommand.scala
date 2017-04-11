package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.groups.ViewSmallGroupAttendanceCommand._
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._
import scala.collection.immutable.SortedMap

object ViewModuleAttendanceCommand {
	def apply(module: Module, academicYear: AcademicYear) =
		new ViewModuleAttendanceCommand(module, academicYear)
			with ComposableCommand[SortedMap[SmallGroupSet, SortedMap[SmallGroup, SmallGroupAttendanceInformation]]]
			with ViewModuleAttendancePermissions
			with ReadOnly with Unaudited {
		override lazy val eventName = "ViewModuleAttendance"
	}
}

class ViewModuleAttendanceCommand(val module: Module, val academicYear: AcademicYear)
	extends CommandInternal[SortedMap[SmallGroupSet, SortedMap[SmallGroup, SmallGroupAttendanceInformation]]] with ViewModuleAttendanceState {

	override def applyInternal(): SortedMap[SmallGroupSet, SortedMap[SmallGroup, SmallGroupAttendanceInformation]] = {
		SortedMap(module.groupSets.asScala.filter(s => s.academicYear == academicYear && s.showAttendanceReports).map(set =>
			set -> ViewSmallGroupSetAttendanceCommand(set).apply()
		).toSeq:_*)
	}

}

trait ViewModuleAttendancePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ViewModuleAttendanceState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroupEvents.ViewRegister, module)
	}
}

trait ViewModuleAttendanceState {
	def module: Module
	def academicYear: AcademicYear
}