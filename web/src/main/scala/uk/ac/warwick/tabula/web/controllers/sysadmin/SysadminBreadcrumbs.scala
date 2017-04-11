package uk.ac.warwick.tabula.web.controllers.sysadmin

import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplate
import uk.ac.warwick.tabula.web.{BreadCrumb, Routes}

trait SysadminBreadcrumbs {
	val Breadcrumbs = SysadminBreadcrumbs
}

object SysadminBreadcrumbs {
	case class Standard(title: String, url: Option[String], override val tooltip: String) extends BreadCrumb

	object Departments {
		case object Home extends BreadCrumb {
			val title = "Departments"
			val url = Some(Routes.sysadmin.Departments.home)
		}

		case class Department(department: model.Department) extends BreadCrumb {
			val title: String = department.name
			val url = Some(Routes.sysadmin.Departments.department(department))
		}
	}

	object AttendanceTemplates {
		case object Home extends BreadCrumb {
			val title = "Attendance monitoring templates"
			val url = Some(Routes.sysadmin.AttendanceTemplates.home)
		}

		case class Edit(template: AttendanceMonitoringTemplate) extends BreadCrumb {
			val title = "Edit"
			val url = Some(Routes.sysadmin.AttendanceTemplates.edit(template))
		}
	}

	object Relationships {
		case object Home extends BreadCrumb {
			val title = "Student relationship types"
			val url = Some(Routes.sysadmin.Relationships.home)
		}
	}
}