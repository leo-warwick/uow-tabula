package uk.ac.warwick.tabula.api.web

import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, RoleOverride}
import uk.ac.warwick.tabula.data.model.{Assignment, Department, Module, Route}
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.web.RoutesUtils

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import RoutesUtils._
	private val context = "/api/v1"

	object assignment {
		def apply(assignment: Assignment) =
			context + "/module/%s/assignments/%s" format (encoded(assignment.module.code), encoded(assignment.id))
	}
}
