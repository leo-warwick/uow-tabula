package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.permissions.PermissionsTarget

class ViewViewableCommand[A <: PermissionsTarget](val permission: Permission, val value: A) extends Command[A] with ReadOnly with Unaudited {
	PermissionCheck(permission, value)

	override def applyInternal(): A = value
}