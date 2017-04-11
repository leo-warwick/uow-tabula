package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

@Entity
@DiscriminatorValue(value="FeedbackPublished")
class FeedbackPublishedNotification
	extends NotificationWithTarget[AssignmentFeedback, Assignment]
	with SingleRecipientNotification
	with SingleItemNotification[AssignmentFeedback]
	with UniversityIdOrUserIdRecipientNotification
	with AutowiringUserLookupComponent
	with AllCompletedActionRequiredNotification {

	def feedback: AssignmentFeedback = item.entity
	def assignment: Assignment = feedback.assignment
	def module: Module = assignment.module
	def moduleCode: String = module.code.toUpperCase

	priority = Warning

	def verb = "publish"

	def title: String = "%s: Your assignment feedback for \"%s\" is now available".format(moduleCode, assignment.name)

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/feedbackready.ftl", Map(
		"assignmentName" -> assignment.name,
		"moduleCode" -> assignment.module.code.toUpperCase,
		"moduleName" -> assignment.module.name,
		"path" -> url
	))

	def url: String = Routes.assignment.receipt(assignment)
	def urlTitle = "view your feedback"

}
