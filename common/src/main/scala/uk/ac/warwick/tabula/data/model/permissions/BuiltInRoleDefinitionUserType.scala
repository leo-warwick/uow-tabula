package uk.ac.warwick.tabula.data.model.permissions

import org.hibernate.`type`.StandardBasicTypes
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition
import uk.ac.warwick.tabula.data.model.AbstractBasicUserType
import uk.ac.warwick.tabula.roles.RoleDefinition
import java.sql.Types

import uk.ac.warwick.tabula.roles.SelectorBuiltInRoleDefinition
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.Promises._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.helpers.MutablePromise

class BuiltInRoleDefinitionUserType extends AbstractBasicUserType[BuiltInRoleDefinition, String] {

	val relationshipService: MutablePromise[RelationshipService] = promise { Wire[RelationshipService] }

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(legacy: String): BuiltInRoleDefinition = {
		// Hardcoded legacy code. Ew
		val string = legacy match {
			case "PersonalTutorRoleDefinition" => "StudentRelationshipAgentRoleDefinition(tutor)"
			case "SupervisorRoleDefinition" => "StudentRelationshipAgentRoleDefinition(supervisor)"
			case _ => legacy
		}

		string match {
			case r"([A-Za-z]+)${roleName}\(\*\)" => {
				SelectorBuiltInRoleDefinition.of(roleName, PermissionsSelector.Any[StudentRelationshipType]) // FIXME hard-wired
			}
			case r"([A-Za-z]+)${roleName}\(([^\)]+)${id}\)" => {
				val selector = relationshipService.get.getStudentRelationshipTypeById(id) match { // FIXME hard-wired
					case Some(selector) => selector
					case _ => relationshipService.get.getStudentRelationshipTypeByUrlPart(id).get // Fall back to url, just in case
				}

				SelectorBuiltInRoleDefinition.of(roleName, selector)
			}
			case _ => RoleDefinition.of(string)
		}
	}

	override def convertToValue(definition: BuiltInRoleDefinition): String = definition match {
		case defn: SelectorBuiltInRoleDefinition[_] => "%s(%s)".format(defn.getName, defn.selector.id)
		case _ => definition.getName
	}

}