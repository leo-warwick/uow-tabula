package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.permissions.BuiltInRoleDefinitionUserType

class RoleDefinitionConverter extends TwoWayConverter[String, RoleDefinition] with Daoisms {

	val builtInUserType = new BuiltInRoleDefinitionUserType

	override def convertLeft(definition: RoleDefinition): String = Option(definition) match {
		case Some(builtIn: BuiltInRoleDefinition) => builtInUserType.convertToValue(builtIn)
		case Some(custom: CustomRoleDefinition) => custom.getId
		case _ => null
	}

	override def convertRight(name: String): RoleDefinition =
		if (!name.hasText) null
		else getById[CustomRoleDefinition](name) getOrElse { try { builtInUserType.convertToObject(name) } catch { case e: IllegalArgumentException => null } }

}