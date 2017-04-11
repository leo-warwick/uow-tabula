package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class ReleaseToMarkerNotificationTest extends TestBase {

	@Test def title() = withUser("cuscav", "0672089") {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")

		val feedback = Fixtures.assignmentFeedback()
		feedback.assignment = assignment

		val markerFeedback = Fixtures.markerFeedback(feedback)

		val notification = Notification.init(new ReleaseToMarkerNotification, currentUser.apparentUser, markerFeedback, assignment)
		notification.title should be ("CS118: Submissions for \"5,000 word essay\" have been released for marking")
	}

}
