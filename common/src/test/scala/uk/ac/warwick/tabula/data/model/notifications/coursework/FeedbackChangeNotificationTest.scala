package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class FeedbackChangeNotificationTest extends TestBase {

	@Test def title() = withUser("cuscav", "0672089") {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")

		val feedback = Fixtures.assignmentFeedback()
		feedback.assignment = assignment

		val notification = Notification.init(new FeedbackChangeNotification, currentUser.apparentUser, feedback, assignment)
		notification.title should be ("CS118: Your assignment feedback for \"5,000 word essay\" has been updated")
	}

}
