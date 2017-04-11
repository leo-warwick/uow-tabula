package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import org.joda.time.DateTime
import uk.ac.warwick.tabula.{Fixtures, TestBase}
import uk.ac.warwick.tabula.data.model._

class ScheduledMeetingRecordBehalfNotificationTest extends TestBase {

	val agent: StaffMember = Fixtures.staff("1234567")
	agent.firstName = "Tutor"
	agent.lastName = "Name"

	val student: StudentMember = Fixtures.student("7654321")
	student.firstName = "Student"
	student.lastName = "Name"

	val relationshipType = StudentRelationshipType("personalTutor", "tutor", "personal tutor", "personal tutee")

	val relationship: StudentRelationship = StudentRelationship(agent, relationshipType, student, DateTime.now)

	val thirdParty: StaffMember = Fixtures.staff()
	thirdParty.firstName = "Third"
	thirdParty.lastName = "Party"

	@Test def title() {
		val meeting = new ScheduledMeetingRecord(thirdParty, relationship)

		val notification = Notification.init(new ScheduledMeetingRecordBehalfNotification("created"), thirdParty.asSsoUser, meeting, relationship)
		notification.title should be ("Personal tutor meeting with Student Name created on your behalf by Third Party")
	}

}
