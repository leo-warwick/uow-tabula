package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{UniversityIdRecipientNotification, SingleRecipientNotification}
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.tabula.data.PreSaveBehaviour
import uk.ac.warwick.tabula.coursework.web.Routes

@Entity
@DiscriminatorValue("SubmissionReceipt")
class SubmissionReceiptNotification extends SubmissionNotification
	with SingleRecipientNotification
	with UniversityIdRecipientNotification
	with PreSaveBehaviour {

	override def preSave(isNew: Boolean) {
		recipientUniversityId = submission.universityId
	}

	def title = moduleCode + ": Submission receipt"
	@transient val templateLocation = "/WEB-INF/freemarker/emails/submissionreceipt.ftl"
	def url = "/coursework" + Routes.assignment.receipt(assignment)
	def urlTitle = "review your submission"

}