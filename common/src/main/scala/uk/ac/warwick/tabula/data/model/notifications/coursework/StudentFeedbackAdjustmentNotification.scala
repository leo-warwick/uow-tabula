package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Info
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.userlookup.User

object StudentFeedbackAdjustmentNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/student_feedback_adjustment_notification.ftl"
}

@Entity
@DiscriminatorValue("StudentFeedbackAdjustment")
class StudentFeedbackAdjustmentNotification
	extends NotificationWithTarget[AssignmentFeedback, Assignment]
	with SingleItemNotification[AssignmentFeedback]
	with SingleRecipientNotification
	with AutowiringUserLookupComponent
	with MyWarwickActivity {

	def verb = "adjusted"
	def assignment: Assignment = target.entity
	def feedback: Feedback = item.entity

	def recipient: User = userLookup.getUserByUserId(feedback.usercode)

	def whatAdjusted: String = {
		val mark = feedback.latestMark.map(m => "mark")
		val grade = feedback.latestGrade.map(g => "grade")
		(mark ++ grade).mkString(" and ")
	}

	def title = s"${assignment.module.code.toUpperCase} - for ${assignment.name} : Adjustments have been made to your $whatAdjusted"

	def content = FreemarkerModel(StudentFeedbackAdjustmentNotification.templateLocation,
		Map(
			"assignment" -> assignment,
			"feedback" -> feedback,
			"whatAdjusted" -> whatAdjusted
		))

	def url: String = Routes.admin.assignment.markerFeedback(assignment, recipient)
	def urlTitle = "view your feedback"

	priority = Info

}
