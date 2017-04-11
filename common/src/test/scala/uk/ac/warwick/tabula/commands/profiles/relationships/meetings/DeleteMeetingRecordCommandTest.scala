package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{MeetingRecordService, MeetingRecordServiceComponent}

class DeleteMeetingRecordCommandTest extends TestBase with Mockito {

	val someTime: DateTime = dateTime(2013, DateTimeConstants.APRIL)
	val mockMeetingRecordService: MeetingRecordService = smartMock[MeetingRecordService]
	val student: StudentMember = Fixtures.student()
	var creator: StaffMember = Fixtures.staff("9876543", "staffmember")
	val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
	var relationship: StudentRelationship = ExternalStudentRelationship("Professor A Tutor", relationshipType, student, DateTime.now)

	val user: CurrentUser = smartMock[CurrentUser]
	user.universityId returns "9876543"

	trait Fixture {
		val meeting = new MeetingRecord
		meeting.creator = creator
		meeting.relationship = relationship
	}

	@Test
	def testDeleted() { new Fixture {
		var deleted: Boolean = meeting.deleted
		deleted should be {false}

		val cmd = new DeleteMeetingRecordCommand(meeting, user) with MeetingRecordServiceComponent {
			val meetingRecordService: MeetingRecordService = mock[MeetingRecordService]
		}
		cmd.applyInternal()

		deleted = meeting.deleted
		deleted should be {true}
	}}

	@Test
	def testRestore() { new Fixture {
		meeting.deleted = true

		val cmd = new RestoreMeetingRecordCommand(meeting, user) with MeetingRecordServiceComponent {
			val meetingRecordService: MeetingRecordService = mockMeetingRecordService
		}
		cmd.applyInternal()

		val deleted: Boolean = meeting.deleted
		deleted should be {false}
	}}

	@Test
	def testPurge() { new Fixture {
		meeting.deleted = true

		val cmd = new PurgeMeetingRecordCommand(meeting, user) with MeetingRecordServiceComponent {
			val meetingRecordService: MeetingRecordService = mockMeetingRecordService
		}

		cmd.applyInternal()

		verify(mockMeetingRecordService, times(1)).purge(meeting)
	}}
}
