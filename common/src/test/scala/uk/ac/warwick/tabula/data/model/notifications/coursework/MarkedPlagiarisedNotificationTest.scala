package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class MarkedPlagiarisedNotificationTest extends TestBase {

	@Test def title() = withUser("cuscav", "cuscav") {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")

		val submission = Fixtures.submission("1412345", "1412345")
		submission.assignment = assignment

		val notification = Notification.init(new MarkedPlagiarisedNotification, currentUser.apparentUser, submission, assignment)
		notification.title should be ("CS118: A submission by 1412345 for \"5,000 word essay\" is suspected of plagiarism")
	}

}
