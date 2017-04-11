package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class ScheduledMeetingRecordMissedInviteeNotificationTest extends TestBase with Mockito {

	@Test def recipient() = withUser("cuscav", "0672089") {
		val agent = Fixtures.staff(currentUser.universityId, currentUser.userId)
		val student = Fixtures.student()

		val relationshipType = StudentRelationshipType("tutor", "tutor", "tutor", "tutee")

		val relationship = StudentRelationship(agent, relationshipType, student, DateTime.now)

		// Scheduled by the agent
		val scheduledMeeting = new ScheduledMeetingRecord(agent, relationship)
		scheduledMeeting.title = "my meeting"
		scheduledMeeting.description = "discuss things"
		scheduledMeeting.meetingDate = DateTime.now
		scheduledMeeting.format = MeetingFormat.FaceToFace

		val notification = Notification.init(new ScheduledMeetingRecordMissedInviteeNotification(), currentUser.apparentUser, scheduledMeeting, scheduledMeeting.relationship)
		notification.recipient.getUserId should be (student.userId)

		// if the student creates the meeting instead, recipient should be staff
		scheduledMeeting.creator = student
		notification.recipient.getUserId should be (agent.userId)
	}

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
	}

	@Test def titleStudent() { new TitleFixture {
		val meeting = new ScheduledMeetingRecord(agent, relationship)

		val notification: ScheduledMeetingRecordMissedInviteeNotification = Notification.init(new ScheduledMeetingRecordMissedInviteeNotification, agent.asSsoUser, meeting, relationship)
		notification.title should be ("Scheduled personal tutor meeting with Tutor Name did not take place")
		notification.recipient.getUserId should be (student.userId)
	}}

	@Test def titleTutor() { new TitleFixture {
		val meeting = new ScheduledMeetingRecord(student, relationship)

		val notification: ScheduledMeetingRecordMissedInviteeNotification = Notification.init(new ScheduledMeetingRecordMissedInviteeNotification, student.asSsoUser, meeting, relationship)
		notification.title should be ("Scheduled personal tutor meeting with Student Name did not take place")
		notification.recipient.getUserId should be (agent.userId)
	}}

}