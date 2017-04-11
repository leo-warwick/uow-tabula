package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.FeedbackForSitsStatus.UploadNotAttempted
import uk.ac.warwick.tabula.{Fixtures, TestBase}


class FeedbackForSitsTest extends TestBase {

	@Test
	def testInit = withUser("0070790", "cusdx") {
		val feedback = Fixtures.assignmentFeedback(currentUser.apparentUser.getWarwickId)

		val feedbackForSits = new FeedbackForSits
		feedbackForSits.feedback = feedback

		feedbackForSits.init(feedback, currentUser.apparentUser)

		feedbackForSits.initialiser should be (currentUser.apparentUser)
		feedbackForSits.lastInitialisedOn.dayOfMonth should be (DateTime.now.dayOfMonth())
		feedbackForSits.status should be (UploadNotAttempted)
		feedbackForSits.feedback should be (feedback)
	}
}
