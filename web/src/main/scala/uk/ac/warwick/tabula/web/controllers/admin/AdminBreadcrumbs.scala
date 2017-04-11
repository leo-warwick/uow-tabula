package uk.ac.warwick.tabula.web.controllers.admin

import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.web.{Routes, BreadCrumb}

trait AdminBreadcrumbs {
	val Breadcrumbs = AdminBreadcrumbs
}

object AdminBreadcrumbs {

	/**
	 * Special case breadcrumb for the department admin page.
	 */
	case class Department(department: model.Department) extends BreadCrumb {
		val title: String = department.name
		val url = Some(Routes.admin.department(department))
	}

	/**
	 * Special case breadcrumb for a module admin page.
	 * Text is the module code, showing the name as a tooltip on hover.
	 */
	case class Module(module: model.Module) extends BreadCrumb {
		val title: String = module.code.toUpperCase
		val url = Some(Routes.admin.module(module))
		override val tooltip: String = module.name
	}

	/**
	 * Special case breadcrumb for a route admin page.
	 * Text is the route code, showing the name as a tooltip on hover.
	 */
	case class Route(route: model.Route) extends BreadCrumb {
		val title: String = route.code.toUpperCase
		val url = Some(Routes.admin.route(route))
		override val tooltip: String = route.name
	}

	/**
	 * Special case breadcrumb for the permissions page
	 */
	case class Permissions[A <: PermissionsTarget](target: A) extends BreadCrumb {
		val title: String = target.humanReadableId
		val url = Some(Routes.admin.permissions(target))
	}

	/**
	 * A breadcrumb without a link, to represent the current page.
	 * We don't currently include the current page in crumbs, but can use this for page titles
	 */
	case class Current(title: String) extends BreadCrumb {
		val url = None
	}
}