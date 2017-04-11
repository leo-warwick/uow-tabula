package uk.ac.warwick.tabula.web.controllers.groups

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.BreadCrumb

trait GroupsBreadcrumbs {
	val Breadcrumbs = GroupsBreadcrumbs
}

object GroupsBreadcrumbs {
	abstract class Abstract extends BreadCrumb
	case class Standard(title: String, url: Option[String], override val tooltip: String) extends Abstract

	/**
	 * Special case breadcrumb for the department admin page, for a particular academic year.
	 */
	case class Department(department: model.Department, academicYear: AcademicYear) extends Abstract {
		val title: String = department.name
		val url = Some(Routes.admin(department, academicYear))
	}

	/**
	 * Special case breadcrumb for a module admin page.
	 * Text is the module code, showing the name as a tooltip on hover.
	 */
	case class Module(module: model.Module) extends Abstract {
		val title: String = module.code.toUpperCase
		val url = Some(Routes.admin(module.adminDepartment, AcademicYear.guessSITSAcademicYearByDate(DateTime.now)))
		override val tooltip: String = module.name
	}

	/**
	 * Special case breadcrumb for a module admin page.
	 * Text is the module code, showing the name as a tooltip on hover.
	 */
	case class ModuleForYear(module: model.Module, academicYear: AcademicYear) extends Abstract {
		val title: String = module.code.toUpperCase
		val url = Some(Routes.admin.module(module, academicYear))
		override val tooltip: String = module.name
	}

	case class Reusable(department: model.Department, academicYear: AcademicYear) extends Abstract {
		val title = "Reusable small groups"
		val url = Some(Routes.admin.reusable(department, academicYear))
	}

	/**
	 * A breadcrumb without a link, to represent the current page.
	 * We don't currently include the current page in crumbs, but can use this for page titles
	 */
	case class Current(title: String) extends Abstract {
		val url = None
	}
}