package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

@Entity
@DiscriminatorValue(value="FeedbackChange")
class FeedbackChangeNotification extends NotificationWithTarget[AssignmentFeedback, Assignment]
	with SingleItemNotification[AssignmentFeedback]
	with SingleRecipientNotification
	with UniversityIdOrUserIdRecipientNotification
	with AutowiringUserLookupComponent
	with AllCompletedActionRequiredNotification {

	def feedback: AssignmentFeedback = item.entity
	def assignment: Assignment = target.entity
	def module: Module = assignment.module
	def moduleCode: String = module.code.toUpperCase

	priority = Warning

	override def onPreSave(newRecord: Boolean) {
		recipientUniversityId = feedback.usercode
	}

	def verb = "modify"

	def title: String = "%s: Your assignment feedback for \"%s\" has been updated".format(moduleCode, assignment.name)

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/feedbackchanged.ftl", Map(
		"assignment" -> assignment,
		"module" -> module
	))

	def url: String = Routes.assignment.receipt(assignment)
	def urlTitle = "view your new feedback"

}
