package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringSecurityServiceComponent, ModuleAndDepartmentServiceComponent, SecurityServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.JavaConverters._

object ViewDepartmentAttendanceCommand {
	val RequiredPermission = Permissions.SmallGroupEvents.ViewRegister

	def apply(department: Department, academicYear: AcademicYear, user: CurrentUser) =
		new ViewDepartmentAttendanceCommandInternal(department, academicYear, user)
			with AutowiringSecurityServiceComponent
			with AutowiringModuleAndDepartmentServiceComponent
			with ComposableCommand[Seq[Module]]
			with ViewDepartmentAttendancePermission
			with ViewDepartmentAttendanceCommandState
			with ReadOnly with Unaudited {
		override lazy val eventName = "ViewDepartmentAttendance"
	}
}

class ViewDepartmentAttendanceCommandInternal(val department: Department, val academicYear: AcademicYear, val user: CurrentUser)
	extends CommandInternal[Seq[Module]] {

	self: SecurityServiceComponent with ModuleAndDepartmentServiceComponent  =>

	override def applyInternal(): Seq[Module] = {
		val modules =
			if (securityService.can(user, Permissions.SmallGroupEvents.ViewRegister, department)) {
				department.modules.asScala
			} else {
				moduleAndDepartmentService.modulesWithPermission(user, Permissions.SmallGroupEvents.ViewRegister, department).toSeq
			}

		modules
			.filter(m => m.groupSets.asScala.exists(g => g.academicYear == academicYear && g.showAttendanceReports ))
			.sortBy(m => (m.groupSets.isEmpty, m.code))
	}
}

trait ViewDepartmentAttendancePermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ViewDepartmentAttendanceCommandState =>

	override def permissionsCheck(p:PermissionsChecking): Unit = {
		p.PermissionCheckAny(department.modules.asScala.map(
			CheckablePermission(ViewDepartmentAttendanceCommand.RequiredPermission, _)
		))
	}
}

trait ViewDepartmentAttendanceCommandState {
	def department: Department
	def academicYear: AcademicYear
	def user: CurrentUser
}