package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent


abstract class SubmissionNotification
	extends NotificationWithTarget[Submission, Assignment]
	with SingleItemNotification[Submission]
	with AutowiringUserLookupComponent {

	def submission: Submission = item.entity
	def assignment: Assignment = target.entity
	def module: Module = assignment.module
	def moduleCode: String = module.code.toUpperCase

	def verb = "submit"
	def templateLocation : String

	def content = FreemarkerModel(templateLocation, Map(
		"submission" -> submission,
		"submissionDate" -> dateTimeFormatter.print(submission.submittedDate),
		"assignment" -> assignment,
		"module" -> module,
		"user" -> userLookup.getUserByUserId(submission.usercode))
	)
}
