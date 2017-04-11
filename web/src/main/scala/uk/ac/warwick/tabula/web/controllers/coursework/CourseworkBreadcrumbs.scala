package uk.ac.warwick.tabula.web.controllers.coursework

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.web.BreadCrumb

trait CourseworkBreadcrumbs {
	val Breadcrumbs = CourseworkBreadcrumbs
}

object CourseworkBreadcrumbs {
	abstract class Abstract extends BreadCrumb
	case class Standard(val title: String, val url: Option[String], override val tooltip: String) extends Abstract

	/**
	 * Special case breadcrumb for the department admin page.
	 */
	case class Department(val department: model.Department) extends Abstract {
		val title: String = department.name
		val url = Some(Routes.admin.department(department))
	}

	/**
	 * Special case breadcrumb for a module admin page.
	 * Text is the module code, showing the name as a tooltip on hover.
	 */
	case class Module(val module: model.Module) extends Abstract {
		val title: String = module.code.toUpperCase
		val url = Some(Routes.admin.module(module))
		override val tooltip: String = module.name
	}

	/**
	 * A breadcrumb without a link, to represent the current page.
	 * We don't currently include the current page in crumbs, but can use this for page titles
	 */
	case class Current(val title: String) extends Abstract {
		val url = None
	}
}