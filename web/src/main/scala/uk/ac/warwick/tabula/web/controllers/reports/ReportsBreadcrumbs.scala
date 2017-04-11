package uk.ac.warwick.tabula.web.controllers.reports

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.web.{Routes, BreadCrumb}

trait ReportsBreadcrumbs {
	val Breadcrumbs = ReportsBreadcrumbs
}

object ReportsBreadcrumbs {
	case class Standard(title: String, url: Option[String]) extends BreadCrumb

	object Home {
		case class Department(department: model.Department) extends BreadCrumb {
			val title: String = department.name
			val url = Some(Routes.reports.departmentHome(department))
		}

		case class DepartmentForYear(department: model.Department, academicYear: AcademicYear) extends BreadCrumb {
			val title: String = academicYear.startYear.toString
			val url = Some(Routes.reports.departmentAcademicYear(department, academicYear))
		}
	}

	object Attendance {
		case class Home(department: model.Department, academicYear: AcademicYear) extends BreadCrumb {
			val title = "Monitoring points"
			val url = Some(Routes.reports.Attendance.home(department, academicYear))
		}
	}

	object SmallGroups {
		case class Home(department: model.Department, academicYear: AcademicYear) extends BreadCrumb {
			val title = "Small group teaching"
			val url = Some(Routes.reports.SmallGroups.home(department, academicYear))
		}
		case class Unrecorded(department: model.Department, academicYear: AcademicYear) extends BreadCrumb {
			val title = "Unrecorded"
			val url = Some(Routes.reports.SmallGroups.unrecorded(department, academicYear))
		}
		case class Missed(department: model.Department, academicYear: AcademicYear) extends BreadCrumb {
			val title = "Missed"
			val url = Some(Routes.reports.SmallGroups.missed(department, academicYear))
		}
	}

	object Profiles {
		case class Home(department: model.Department, academicYear: AcademicYear) extends BreadCrumb {
			val title = "Profiles"
			val url = Some(Routes.reports.Profiles.home(department, academicYear))
		}
		case class Export(department: model.Department, academicYear: AcademicYear) extends BreadCrumb {
			val title = "Export"
			val url = Some(Routes.reports.Profiles.export(department, academicYear))
		}
	}
}