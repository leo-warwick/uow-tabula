package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.roles.Sysadmin

@Component
class SysadminRoleProvider extends WebgroupRoleProvider(Sysadmin()) {
	override var webgroup: String = Wire.property("${permissions.admin.group}")
}