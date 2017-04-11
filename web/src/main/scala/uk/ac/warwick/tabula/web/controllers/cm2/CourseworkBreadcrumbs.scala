package uk.ac.warwick.tabula.web.controllers.cm2

import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.Assignment
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
	object SubmissionsAndFeedback {

		case class SubmissionsAndFeedbackManagement(val assignment: Assignment) extends Abstract {
			val title: String = "Submissions and Feedback Management"
			val url = Some(Routes.admin.assignment.submissionsandfeedback(assignment))

		}

	}
}