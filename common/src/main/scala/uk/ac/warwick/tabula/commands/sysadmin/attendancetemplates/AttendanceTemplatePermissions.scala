package uk.ac.warwick.tabula.commands.sysadmin.attendancetemplates

import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

trait AttendanceTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPointTemplates.Manage)
	}

}
