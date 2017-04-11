package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.roles.Masquerader

@Component
class MasqueraderRoleProvider extends WebgroupRoleProvider(Masquerader()) {
	override var webgroup: String = Wire.property("${permissions.masquerade.group}")
}