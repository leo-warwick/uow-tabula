package uk.ac.warwick.tabula.admin.web

import uk.ac.warwick.tabula.data.model.{Assignment, Member, Route, Module, Department}
import uk.ac.warwick.tabula.web.RoutesUtils
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroup}
import uk.ac.warwick.tabula.data.model.permissions.{RoleOverride, CustomRoleDefinition}

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import RoutesUtils._
	private val context = "/admin"
	def home: String = context + "/"
	def masquerade: String = context + "/masquerade"

	object department {
		def apply(department: Department): String = context + "/department/%s" format (encoded(department.code))

		def permissions(department: Department): String = context + "/department/%s/permissions" format (encoded(department.code))

		def edit(department: Department): String = context + "/department/%s/edit" format (encoded(department.code))
		def createSubDepartment(department: Department): String = context + "/department/%s/subdepartment/new" format (encoded(department.code))
		def createModule(department: Department): String = context + "/department/%s/module/new" format (encoded(department.code))

		def sortModules(department: Department): String = context + "/department/%s/sort-modules" format (encoded(department.code))
		def sortRoutes(department: Department): String = context + "/department/%s/sort-routes" format (encoded(department.code))

		object customRoles {
			def apply(department: Department) = s"$context/department/${encoded(department.code)}/customroles/list"
			def add(department: Department) = s"$context/department/${encoded(department.code)}/customroles/list"
			def edit(definition: CustomRoleDefinition) = s"$context/department/${encoded(definition.department.code)}/customroles/${encoded(definition.id)}/edit"
			def delete(definition: CustomRoleDefinition) = s"$context/department/${encoded(definition.department.code)}/customroles/${encoded(definition.id)}/delete"

			def overrides(definition: CustomRoleDefinition) = s"$context/department/${encoded(definition.department.code)}/customroles/${encoded(definition.id)}/overrides/list"
			def addOverride(definition: CustomRoleDefinition) = s"$context/department/${encoded(definition.department.code)}/customroles/${encoded(definition.id)}/overrides/add"
			def deleteOverride(roleOverride: RoleOverride) = s"$context/department/${encoded(roleOverride.customRoleDefinition.department.code)}/customroles/${encoded(roleOverride.customRoleDefinition.id)}/overrides/${encoded(roleOverride.id)}/delete"
		}
	}

	object module {
		def apply(module: Module): String = department(module.adminDepartment) + "#module-" + encoded(module.code)

		def permissions(module: Module): String = context + "/module/%s/permissions" format (encoded(module.code))
	}

	object route {
		def apply(route: Route): String = department(route.adminDepartment) + "#route-" + encoded(route.code)

		def permissions(route: Route): String = context + "/route/%s/permissions" format (encoded(route.code))
	}

	object permissions {
		def apply[A <: PermissionsTarget](target: A) =
			s"$context/permissions/${encoded(target.urlCategory)}/${encoded(target.urlSlug)}"
	}
}
