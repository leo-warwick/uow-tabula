package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model._


abstract class TurnitinReportNotification
	extends NotificationWithTarget[OriginalityReport, Assignment] with AllCompletedActionRequiredNotification {

	def assignment: Assignment = target.entity

	def verb = "request"
	def url: String = Routes.admin.assignment.submissionsandfeedback(assignment)
	def urlTitle = "view the Turnitin results"
	priority = Warning

}