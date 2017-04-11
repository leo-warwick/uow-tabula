package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.MeetingRecordDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class ConvertScheduledMeetingRecordCommandTest extends TestBase with Mockito {

	trait Fixture {
		val meetingRecordDao: MeetingRecordDao = mock[MeetingRecordDao]
		val notificationService: NotificationService = mock[NotificationService]
		val mockMeetingRecordService: MeetingRecordService = mock[MeetingRecordService]

		var creator: StaffMember = Fixtures.staff(universityId = "12345")
		val student: StudentMember = Fixtures.student()
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		val relationship = ExternalStudentRelationship("Professor A Tutor", relationshipType, student, DateTime.now)


		val createCommand = new Appliable[MeetingRecord] {
			override def apply(): MeetingRecord = {
				new MeetingRecord
			}
		}

		val scheduledMeetingRecord: ScheduledMeetingRecord = new ScheduledMeetingRecord()

		val command = new ConvertScheduledMeetingRecordCommand(creator, scheduledMeetingRecord) with ConvertScheduledMeetingRecordCommandSupport with MeetingRecordServiceComponent {
			val meetingRecordService: MeetingRecordService = mockMeetingRecordService
		}
		command.createCommand = createCommand
	}

	@Test
	def apply() { new Fixture {
		val newMeetingRecord: MeetingRecord = command.applyInternal()

		verify(mockMeetingRecordService, times(1)).purge(scheduledMeetingRecord)
	}}

	trait ConvertScheduledMeetingRecordCommandSupport extends FileAttachmentServiceComponent {
		def fileAttachmentService: FileAttachmentService = mock[FileAttachmentService]
	}

}
