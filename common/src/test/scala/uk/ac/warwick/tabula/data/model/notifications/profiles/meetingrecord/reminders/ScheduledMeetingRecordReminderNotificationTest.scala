package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.reminders

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{Fixtures, TestBase}
import uk.ac.warwick.userlookup.AnonymousUser

class ScheduledMeetingRecordReminderNotificationTest extends TestBase {

	trait TitleFixture {
		val agent: StaffMember = Fixtures.staff("1234567", "tutor")
		agent.firstName = "Tutor"
		agent.lastName = "Name"

		val student: StudentMember = Fixtures.student("7654321", "student")
		student.firstName = "Student"
		student.lastName = "Name"

		val relationshipType = StudentRelationshipType("personalTutor", "tutor", "personal tutor", "personal tutee")

		val relationship: StudentRelationship = StudentRelationship(agent, relationshipType, student, DateTime.now)

		val thirdParty: StaffMember = Fixtures.staff("1122331", "3rdparty")
		thirdParty.firstName = "Third"
		thirdParty.lastName = "Party"

		val meeting = new ScheduledMeetingRecord(agent, relationship)
		meeting.meetingDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 11, 0, 0, 0)
	}

	@Test def titleForStudent() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) { new TitleFixture {
		val notification: ScheduledMeetingRecordReminderStudentNotification = Notification.init(new ScheduledMeetingRecordReminderStudentNotification, new AnonymousUser, meeting, relationship)
		notification.title should be ("Personal tutor meeting with Tutor Name today at 11am")
	}}

	@Test def titleForTutor() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) { new TitleFixture {
		val notification: ScheduledMeetingRecordReminderAgentNotification = Notification.init(new ScheduledMeetingRecordReminderAgentNotification, new AnonymousUser, meeting, relationship)
		notification.title should be ("Personal tutor meeting with Student Name today at 11am")
	}}

	@Test def titleForStudentAfterTheFact() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 18, 9, 39, 0, 0)) { new TitleFixture {
		val notification: ScheduledMeetingRecordReminderStudentNotification = Notification.init(new ScheduledMeetingRecordReminderStudentNotification, new AnonymousUser, meeting, relationship)
		notification.title should be ("Personal tutor meeting with Tutor Name at 11am, Monday 15 September 2014")
	}}

}
