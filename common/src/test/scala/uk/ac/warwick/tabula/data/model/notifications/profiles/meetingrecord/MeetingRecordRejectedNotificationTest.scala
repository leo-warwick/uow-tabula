package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class MeetingRecordRejectedNotificationTest extends TestBase {

	 val agent: StaffMember = Fixtures.staff("1234567")
	 agent.firstName = "Tutor"
	 agent.lastName = "Name"

	 val student: StudentMember = Fixtures.student("7654321")
	 student.firstName = "Student"
	 student.lastName = "Name"

	 val relationshipType = StudentRelationshipType("personalTutor", "tutor", "personal tutor", "personal tutee")

	 val relationship: StudentRelationship = StudentRelationship(agent, relationshipType, student, DateTime.now)

	 @Test def titleStudent() = withUser("cuscav", "0672089") {
		 val meeting = new MeetingRecord(student, relationship)

		 val approval = Fixtures.meetingRecordApproval(state = MeetingApprovalState.Rejected)
		 approval.meetingRecord = meeting

		 val notification = Notification.init(new MeetingRecordRejectedNotification, currentUser.apparentUser, approval)
		 notification.title should be ("Personal tutor meeting record with Tutor Name returned with comments")
	 }

	 @Test def titleTutor() = withUser("cuscav", "0672089") {
		 val meeting = new MeetingRecord(agent, relationship)

		 val approval = Fixtures.meetingRecordApproval(state = MeetingApprovalState.Rejected)
		 approval.meetingRecord = meeting

		 val notification = Notification.init(new MeetingRecordRejectedNotification, currentUser.apparentUser, approval)
		 notification.title should be ("Personal tutor meeting record with Student Name returned with comments")
	 }

 }
