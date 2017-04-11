package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class SubmissionReceiptNotificationTest extends TestBase {

	@Test def title() = withUser("cuscav", "0672089") {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")

		val submission = Fixtures.submission()
		submission.assignment = assignment

		val notification = Notification.init(new SubmissionReceiptNotification, currentUser.apparentUser, submission, assignment)
		notification.title should be ("CS118: Submission receipt for \"5,000 word essay\"")
	}

}
