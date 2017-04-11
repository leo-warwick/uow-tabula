package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.data.model.{AssignmentFeedback, HeronWarningNotification, Submission, UserSettings}
import uk.ac.warwick.tabula.{Fixtures, TestBase}
import uk.ac.warwick.tabula.roles.{FeedbackRecipient, SettingsOwner, Submitter}
import uk.ac.warwick.userlookup.User

class OwnDataRoleProviderTest extends TestBase {

	val provider = new OwnDataRoleProvider

	val submission: Submission = Fixtures.submission("0123456", "cuscav")
	val feedback: AssignmentFeedback = Fixtures.assignmentFeedback("0123456", "cuscav")
	val userSettings: UserSettings = Fixtures.userSettings("cuscav")

	val agent: User =  Fixtures.user("cuslaj")
	val user: User = Fixtures.user("0123456", "cuscav")

	val notification: HeronWarningNotification = Fixtures.notification(agent, user)
	val notification2: HeronWarningNotification = Fixtures.notification(agent, agent)


	@Test def forSubmission() = withUser("cuscav", "0123456") {
		provider.getRolesFor(currentUser, submission) should be (Seq(Submitter(submission)))
		provider.getRolesFor(currentUser, Fixtures.submission("xxxxxx", "000000")) should be (Seq())
	}

	@Test def forFeedback() = withUser("cuscav", "0123456") {
		// only if released!
		provider.getRolesFor(currentUser, feedback) should be (Seq())

		feedback.released = true

		provider.getRolesFor(currentUser, feedback) should be (Seq(FeedbackRecipient(feedback)))

		provider.getRolesFor(currentUser, Fixtures.assignmentFeedback()) should be (Seq())
	}

	@Test def forSettings() = withUser("cuscav", "0123456") {
		provider.getRolesFor(currentUser, userSettings) should be (Seq(SettingsOwner(userSettings)))
		provider.getRolesFor(currentUser, Fixtures.userSettings("xxxxxx")) should be (Seq())
	}

	@Test def forSettingsNoId() = withUser("") {
		provider.getRolesFor(currentUser, Fixtures.userSettings("")) should be (Seq())
	}

	@Test def handlesDefault() = withUser("cuscav", "0123456") {
		provider.getRolesFor(currentUser, Fixtures.department("in", "IN202")) should be (Seq())
	}

}