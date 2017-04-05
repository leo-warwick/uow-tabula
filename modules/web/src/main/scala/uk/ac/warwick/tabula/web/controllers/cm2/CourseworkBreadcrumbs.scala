package uk.ac.warwick.tabula.web.controllers.cm2

import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.web.BreadCrumb

trait CourseworkBreadcrumbs {
	val Breadcrumbs = CourseworkBreadcrumbs
}

object CourseworkBreadcrumbs {

	abstract class Abstract extends BreadCrumb



	object Assignment {

		case class AssignmentManagement() extends Abstract {
			val title: String = "Assignment Management"
			val url = Some(Routes.home)
		}

	}
	object Department {

		case class DepartmentManagement() extends Abstract {
			val title: String = "Department Management"
			val url = Some(Routes.admin.department)
		}

	}
}