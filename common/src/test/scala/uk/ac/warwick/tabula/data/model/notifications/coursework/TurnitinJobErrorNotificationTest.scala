package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model.{Notification, OriginalityReport}
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class TurnitinJobErrorNotificationTest extends TestBase {

	 @Test def title() = withUser("cuscav", "0672089") {
		 val assignment = Fixtures.assignment("5,000 word essay")
		 assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")

		 val report = new OriginalityReport

		 val notification = Notification.init(new TurnitinJobErrorNotification, currentUser.apparentUser, report, assignment)
		 notification.title should be ("CS118: The Turnitin check for \"5,000 word essay\" has not completed successfully")
	 }

 }
