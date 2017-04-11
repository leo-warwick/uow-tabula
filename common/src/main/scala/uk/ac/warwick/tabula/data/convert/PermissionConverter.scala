package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.services.RelationshipService
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.permissions.PermissionUserType

class PermissionConverter extends TwoWayConverter[String, Permission] {

	val userType = new PermissionUserType

	override def convertLeft(permission: Permission): String = Option(permission).map(userType.convertToValue).orNull

	override def convertRight(name: String): Permission = {
		if (!name.hasText) null
		else try { userType.convertToObject(name) } catch { case e: IllegalArgumentException => null }
	}

}