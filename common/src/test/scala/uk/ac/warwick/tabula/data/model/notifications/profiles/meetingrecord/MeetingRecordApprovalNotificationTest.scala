package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import org.joda.time.DateTime
import uk.ac.warwick.tabula.{Fixtures, TestBase}
import uk.ac.warwick.tabula.data.model._

class MeetingRecordApprovalNotificationTest extends TestBase {

	val agent: StaffMember = Fixtures.staff("1234567")
	agent.firstName = "Tutor"
	agent.lastName = "Name"

	val student: StudentMember = Fixtures.student("7654321")
	student.firstName = "Student"
	student.lastName = "Name"

	val relationshipType = StudentRelationshipType("personalTutor", "tutor", "personal tutor", "personal tutee")

	val relationship: StudentRelationship = StudentRelationship(agent, relationshipType, student, DateTime.now)

	@Test def titleNewMeetingStudent() = withUser("cuscav", "0672089") {
		val meeting = new MeetingRecord(agent, relationship)

		val notification = Notification.init(new NewMeetingRecordApprovalNotification, currentUser.apparentUser, meeting, relationship)
		notification.title should be ("Personal tutor meeting record with Tutor Name needs review")
	}

	@Test def titleNewMeetingTutor() = withUser("cuscav", "0672089") {
		val meeting = new MeetingRecord(student, relationship)

		val notification = Notification.init(new NewMeetingRecordApprovalNotification, currentUser.apparentUser, meeting, relationship)
		notification.title should be ("Personal tutor meeting record with Student Name needs review")
	}

	@Test def titleEditMeetingStudent() = withUser("cuscav", "0672089") {
		val meeting = new MeetingRecord(agent, relationship)

		val notification = Notification.init(new EditedMeetingRecordApprovalNotification, currentUser.apparentUser, meeting, relationship)
		notification.title should be ("Personal tutor meeting record with Tutor Name needs review")
	}

	@Test def titleEditMeetingTutor() = withUser("cuscav", "0672089") {
		val meeting = new MeetingRecord(student, relationship)

		val notification = Notification.init(new EditedMeetingRecordApprovalNotification, currentUser.apparentUser, meeting, relationship)
		notification.title should be ("Personal tutor meeting record with Student Name needs review")
	}

}
