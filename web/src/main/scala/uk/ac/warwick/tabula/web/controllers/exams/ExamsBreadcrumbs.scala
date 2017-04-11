package uk.ac.warwick.tabula.web.controllers.exams

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.web.BreadCrumb

trait ExamsBreadcrumbs {
	val Breadcrumbs = ExamsBreadcrumbs
}

object ExamsBreadcrumbs {
	abstract class Abstract extends BreadCrumb
	case class Standard(title: String, url: Option[String], override val tooltip: String) extends Abstract

	object Exams {

		case object Home extends Abstract {
			val title = "Manage Exams"
			val url = Some(Routes.Exams.home)
		}

		case class Department(department: model.Department, academicYear: AcademicYear) extends Abstract {
			val title: String = department.name
			val url = Some(Routes.Exams.admin.department(department, academicYear))
		}

		case class Module(module: model.Module, academicYear: AcademicYear) extends Abstract {
			val title: String = module.code.toUpperCase
			val url = Some(Routes.Exams.admin.module(module, academicYear))
			override val tooltip: String = module.name
		}

	}

	object Grids {

		case object Home extends Abstract {
			val title = "Manage Exam Grids"
			val url = Some(Routes.Grids.home)
		}

		case class Department(department: model.Department, academicYear: AcademicYear) extends Abstract {
			val title: String = department.name
			val url = Some(Routes.Grids.departmentAcademicYear(department, academicYear))
		}

	}

}