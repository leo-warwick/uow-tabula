package uk.ac.warwick.tabula.data.model.permissions

import org.hibernate.`type`.StandardBasicTypes
import uk.ac.warwick.tabula.data.model.{AbstractBasicUserType, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions, PermissionsSelector, SelectorPermission}
import java.sql.Types

import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.MutablePromise
import uk.ac.warwick.tabula.helpers.Promises._
import uk.ac.warwick.tabula.helpers.StringUtils._

class PermissionUserType extends AbstractBasicUserType[Permission, String] {

	val relationshipService: MutablePromise[RelationshipService] = promise { Wire[RelationshipService] }

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String): Permission = {
		string match {
			case r"([A-Za-z\.]+)${permissionName}\(\*\)" => {
				SelectorPermission.of(permissionName, PermissionsSelector.Any[StudentRelationshipType]) // FIXME hard-wired
			}
			case r"([A-Za-z\.]+)${permissionName}\(([^\)]+)${id}\)" => {
				val selector = relationshipService.get.getStudentRelationshipTypeById(id) match {
					case Some(selector) => selector
					case _ => relationshipService.get.getStudentRelationshipTypeByUrlPart(id).get // Fall back to url, just in case
				}

				SelectorPermission.of(permissionName, selector) // FIXME hard-wired
			}
			case _ => Permissions.of(string)
		}
	}

	override def convertToValue(permission: Permission): String = permission match {
		case sperm: SelectorPermission[_] => "%s(%s)".format(sperm.getName, sperm.selector.id)
		case _ => permission.getName
	}

}