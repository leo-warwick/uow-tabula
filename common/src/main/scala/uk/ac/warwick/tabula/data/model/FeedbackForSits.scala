package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.joda.time.DateTime
import org.hibernate.annotations.Type
import uk.ac.warwick.userlookup.User

@Entity
class FeedbackForSits extends GeneratedId {

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "feedback_id")
	var feedback: Feedback = _


	@Column(name="status")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.FeedbackForSitsStatusUserType")
	var status: FeedbackForSitsStatus = _

	var dateOfUpload: DateTime = _
	var actualMarkLastUploaded: Integer = _
	var actualGradeLastUploaded: String = _
	var firstCreatedOn: DateTime = _
	var lastInitialisedOn: DateTime = _

	// 'initialiser' is the identify of whoever last queued this feedback for upload - same as the creator unless for
	// some reason the feedback is republished (and that functionality is being removed soon, so in future having an
	// initialiser who is not the creator might only be possible as a result of a db hack)
	@Column(nullable=false)
	@Type(`type`="uk.ac.warwick.tabula.data.model.SSOUserType")
	final var initialiser: User = null

	def init(feedback: Feedback, initialiser: User): Unit = {
		this.feedback = feedback
		this.initialiser = initialiser
		this.lastInitialisedOn = DateTime.now()
		this.status = FeedbackForSitsStatus.UploadNotAttempted
	}

}
