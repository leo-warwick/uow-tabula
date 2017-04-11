package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.permissions.RoleOverride
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.TwoWayConverter

class RoleOverrideConverter extends TwoWayConverter[String, RoleOverride] with Daoisms {

	override def convertLeft(roleOverride: RoleOverride): String = Option(roleOverride).map { _.id }.orNull

	override def convertRight(id: String): RoleOverride =
		if (!id.hasText) null
		else getById[RoleOverride](id).orNull

}