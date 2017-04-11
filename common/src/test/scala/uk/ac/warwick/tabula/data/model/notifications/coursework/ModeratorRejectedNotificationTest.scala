package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class ModeratorRejectedNotificationTest extends TestBase {

	@Test def title() = withUser("cuscav", "0672089") {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")

		val feedback = Fixtures.assignmentFeedback("1412345", "1412345")
		feedback.assignment = assignment

		val markerFeedback = Fixtures.markerFeedback(feedback)

		val notification = Notification.init(new ModeratorRejectedNotification, currentUser.apparentUser, markerFeedback)
		notification.title should be ("CS118: Feedback for 1412345 for \"5,000 word essay\" has been rejected by the moderator")
	}

}
