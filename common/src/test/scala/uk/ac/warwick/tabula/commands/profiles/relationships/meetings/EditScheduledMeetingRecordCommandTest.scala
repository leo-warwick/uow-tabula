package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.DateTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{FileAttachmentService, FileAttachmentServiceComponent, MeetingRecordService, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.DateFormats._

class EditScheduledMeetingRecordCommandTest  extends TestBase with Mockito {

	trait Fixture {
		val relationship: StudentRelationship = mock[StudentRelationship]

		val mockMeetingRecordService: MeetingRecordService = mock[MeetingRecordService]
		mockMeetingRecordService.listScheduled(Set(relationship), Some(creator)) returns Seq()

		var creator: StaffMember = _
		var scheduledMeetingRecord: ScheduledMeetingRecord = new ScheduledMeetingRecord()
		val newMeetingDate = new DateTime().plusMonths(1)
		scheduledMeetingRecord.meetingDate = newMeetingDate
		scheduledMeetingRecord.meetingEndDate = newMeetingDate.plusHours(1)
		scheduledMeetingRecord.relationship = relationship
		val command = new EditScheduledMeetingRecordCommand(creator, scheduledMeetingRecord) with EditScheduledMeetingRecordCommandValidation with EditScheduledMeetingRecordCommandSupport with MeetingRecordServiceComponent {
			val meetingRecordService: MeetingRecordService = mockMeetingRecordService
		}
	}

	@Test
	def applyNotRescheduled() { new Fixture {
		val sameDate: DateTime = new DateTime().plusMonths(1)
		scheduledMeetingRecord.meetingDate = sameDate
		scheduledMeetingRecord.meetingEndDate = sameDate.plusHours(1)

		scheduledMeetingRecord.lastUpdatedDate.isAfter(scheduledMeetingRecord.creationDate) should be (false)

		val newTitle: String = "new title"
		command.title = newTitle
		command.meetingDateStr = sameDate.toString(DatePickerFormatter)
		command.meetingTimeStr = sameDate.toString(TimePickerFormatter)
		command.meetingEndTimeStr = sameDate.plusHours(1).toString(TimePickerFormatter)

		Thread.sleep(2) // otherwise sometimes the last updated date is not after created date and that assertion fails

		val result: ScheduledMeetingRecordResult = command.applyInternal()

		result.meetingRecord.title should be (newTitle)
		result.meetingRecord.lastUpdatedDate.isAfter(result.meetingRecord.creationDate) should be (true)
		result.isRescheduled should be (false)

		verify(mockMeetingRecordService, times(1)).saveOrUpdate(scheduledMeetingRecord)

	}}

	@Test
	def applyRescheduled() { new Fixture {

		scheduledMeetingRecord.lastUpdatedDate == scheduledMeetingRecord.creationDate should be (true)

		val newTitle: String = "new title"
		val newDescription: String = "new description"
		val newFormat = MeetingFormat.VideoConference

		val theNewMeetingDate: DateTime = new DateTime().plusMinutes(1)
		command.title = newTitle
		command.description = newDescription
		command.format = newFormat
		command.meetingDateStr = theNewMeetingDate.toString(DatePickerFormatter)
		command.meetingTimeStr = theNewMeetingDate.toString(TimePickerFormatter)
		command.meetingEndTimeStr = theNewMeetingDate.plusHours(1).toString(TimePickerFormatter)

		Thread.sleep(2) // otherwise sometimes the last updated date is not after created date and that assertion fails

		val result: ScheduledMeetingRecordResult = command.applyInternal()

		result.meetingRecord.title should be (newTitle)
		result.meetingRecord.description should be (newDescription)
		result.meetingRecord.lastUpdatedDate.isAfter(result.meetingRecord.creationDate) should be (true)
		result.meetingRecord.format should be (newFormat)
		result.meetingRecord.meetingDate.toString(DatePickerFormatter) should be (theNewMeetingDate.toString(DatePickerFormatter))
		result.isRescheduled should be (true)

		verify(mockMeetingRecordService, times(1)).saveOrUpdate(scheduledMeetingRecord)

	}}

	@Test
	def validMeeting() { new Fixture {
		val errors = new BindException(command, "command")
		command.title = "title"
		command.format = MeetingFormat.FaceToFace
		val theMeeting = new DateTime().plusDays(1)
		command.meetingDateStr = theMeeting.toString(DatePickerFormatter)
		command.meetingTimeStr = theMeeting.toString(TimePickerFormatter)
		command.meetingEndTimeStr = theMeeting.plusHours(1).toString(TimePickerFormatter)
		errors.hasErrors should be (false)
	}}

	@Test
	def noTitle() { new Fixture {
		val errors = new BindException(command, "command")
		command.format = MeetingFormat.FaceToFace
		val startdate = new DateTime().plusDays(1)
		command.meetingDateStr = startdate.toString(DatePickerFormatter)
		command.meetingTimeStr = startdate.toString(TimePickerFormatter)
		command.meetingEndTimeStr = startdate.plusHours(1).toString(TimePickerFormatter)
		command.validate(errors)
		errors.hasErrors should be (true)
		errors.getFieldErrorCount should be(1)
		errors.getFieldErrors("title").size should be(1)
	}}

	@Test
	def noFormat() { new Fixture {
		val errors = new BindException(command, "command")
		command.title = "A Meeting"
		command.meetingDateStr = new DateTime().plusHours(1).toString(DatePickerFormatter)
		command.meetingTimeStr = new DateTime().plusHours(1).toString(TimePickerFormatter)
		command.meetingEndTimeStr = new DateTime().plusHours(2).toString(TimePickerFormatter)
		command.validate(errors)
		errors.hasErrors should be (true)
		errors.getFieldErrorCount should be(1)
		errors.getFieldErrors("format").size should be(1)
	}}

	@Test
	def scheduleInPast() { new Fixture {
		val errors = new BindException(command, "command")
		command.format = MeetingFormat.FaceToFace
		command.title = "A Title"
		command.meetingDateStr = new DateTime().minusDays(1).toString(DatePickerFormatter)
		command.meetingTimeStr = new DateTime().minusDays(1).toString(TimePickerFormatter)
		command.meetingEndTimeStr = new DateTime().minusDays(1).plusHours(1).toString(TimePickerFormatter)
		command.validate(errors)
		errors.hasErrors should be (true)
		errors.getFieldErrorCount should be(1)
		errors.getFieldErrors("meetingDateStr").size should be(1)
	}}

	@Test
	def scheduleDuplicateDate() { new Fixture {

		val meetingTime: DateTime = new DateTime().plusMonths(1)

		val meetingWithDupeDate: ScheduledMeetingRecord = new ScheduledMeetingRecord
		meetingWithDupeDate.meetingDate = meetingTime
		meetingWithDupeDate.meetingEndDate = meetingTime.plusHours(1)

		meetingWithDupeDate.id = "A"

		command.meetingRecord.id = "B"

		mockMeetingRecordService.listScheduled(Set(relationship), Some(creator)) returns Seq(meetingWithDupeDate)

		val errors = new BindException(command, "command")
		command.format = MeetingFormat.FaceToFace
		command.title = "A Title"
		command.meetingDateStr = meetingTime.toString(DatePickerFormatter)
		command.meetingTimeStr = meetingTime.toString(TimePickerFormatter)
		command.meetingEndTimeStr = meetingTime.plusHours(1).toString(TimePickerFormatter)
		command.validate(errors)
		errors.hasErrors should be (true)
		errors.getFieldErrorCount should be(1)
		errors.getFieldErrors("meetingDateStr").size should be(1)
	}}

	@Test
	def noInput() { new Fixture {
		val errors = new BindException(command, "command")
		command.validate(errors)
		errors.hasErrors should be (true)
	}}

	trait EditScheduledMeetingRecordCommandSupport extends FileAttachmentServiceComponent {
		def fileAttachmentService: FileAttachmentService = mock[FileAttachmentService]
	}

}
