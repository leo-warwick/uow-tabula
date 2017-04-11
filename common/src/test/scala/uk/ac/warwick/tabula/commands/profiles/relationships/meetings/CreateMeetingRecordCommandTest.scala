package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.{DateTime, DateTimeConstants}
import org.springframework.validation.BindException
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.tabula.DateFormats.{DatePickerFormatter, TimePickerFormatter}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model.MeetingFormat._
import uk.ac.warwick.tabula.data.model.{ExternalStudentRelationship, _}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringMeetingRecordService, AttendanceMonitoringMeetingRecordServiceComponent}
import uk.ac.warwick.tabula.services.{FileAttachmentService, FileAttachmentServiceComponent, MeetingRecordService, MeetingRecordServiceComponent}

import scala.collection.JavaConverters._

// scalastyle:off magic.number
class CreateMeetingRecordCommandTest extends TestBase with Mockito {

	val aprilFool: DateTime = dateTime(2013, DateTimeConstants.APRIL)
	val marchHare: DateTime = dateTime(2013, DateTimeConstants.MARCH)
	val thisCreator: StaffMember = Fixtures.staff("9876543")
	val student: StudentMember = Fixtures.student(universityId="1170836", userId="studentmember")
	val thisRelationship = ExternalStudentRelationship(
		"Professor A Tutor",
		StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee"),
		student,
		DateTime.now
	)
	
	trait ValidationFixture {
		val validator = new ModifyMeetingRecordValidation
			with MeetingRecordCommandRequest
			with CreateMeetingRecordCommandState {
			override val creator: Member = thisCreator
			override val relationship: StudentRelationship = thisRelationship
		}
	}
	
	@Test
	def invalidFutureDate(): Unit = withUser("cuscav") { withFakeTime(aprilFool) { new ValidationFixture {
		validator.title = "A title"
		validator.format = FaceToFace
		validator.meetingDateTime  = dateTime(3903, DateTimeConstants.MARCH) // it's the future
		validator.meetingDateStr = validator.meetingDateTime.toString(DatePickerFormatter)
		validator.meetingTimeStr = validator.meetingDateTime.toString(TimePickerFormatter)

		var errors = new BindException(validator, "command")
		validator.validate(errors)
		errors.hasErrors should be (true)
		errors.getErrorCount should be(1)
		errors.getFieldErrors.asScala.map(_.getField).contains("meetingDateStr") should be (true)
		errors.getFieldError.getCode should be ("meetingRecord.date.future")
	}}}

	@Test
	def invalidPastDate(): Unit = withUser("cuscav") { withFakeTime(aprilFool) { new ValidationFixture {
		validator.title = "A title"
		validator.format = FaceToFace
		validator.meetingDateTime = dateTime(2007, DateTimeConstants.MARCH) // > 5 years ago
		validator.meetingDateStr = dateTime(2007, DateTimeConstants.MARCH).toString(DatePickerFormatter)
		validator.meetingTimeStr = dateTime(2007, DateTimeConstants.MARCH).toString(TimePickerFormatter)

		var errors = new BindException(validator, "command")
		validator.validate(errors)
		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getFieldErrors.asScala.map(_.getField).contains("meetingDateStr") should be (true)
		errors.getFieldError.getCode should be ("meetingRecord.date.prehistoric")
	}}}

	@Test
	def invalidTimes(): Unit = withUser("cuscav") { withFakeTime(aprilFool) { new ValidationFixture {
		validator.title = "A title"
		validator.format = FaceToFace

		val yesterday: DateTime = DateTime.now.minusDays(1).plusHours(10)
		validator.meetingDateTime = yesterday
		validator.meetingDateStr = yesterday.toString(DatePickerFormatter)
		validator.meetingTimeStr = yesterday.toString(TimePickerFormatter)

		validator.meetingEndDateTime = yesterday.minusHours(1) // end is before the start
		validator.meetingEndTimeStr = yesterday.minusHours(1).toString(TimePickerFormatter)

		var errors = new BindException(validator, "command")
		validator.validate(errors)
		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getFieldErrors.asScala.map(_.getField).contains("meetingTimeStr") should be (true)
		errors.getFieldError.getCode should be ("meetingRecord.date.endbeforestart")
	}}}

	@Test
	def invalidEmptyStartTime(): Unit = withUser("cuscav") { withFakeTime(aprilFool) { new ValidationFixture {
		validator.meetingDateTime = marchHare
		validator.meetingDateStr = validator.meetingDateTime.toString(DatePickerFormatter)
		validator.meetingTimeStr = ""
		validator.meetingEndTimeStr = "10:00:00"
		validator.title = "A good title"
		validator.format = FaceToFace

		var errors = new BindException(validator, "command")
		validator.validate(errors)
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("meetingTimeStr")
		errors.getFieldError.getCode should be ("meetingRecord.starttime.missing")
	}}}

	@Test
	def invalidEmptyEndTime(): Unit = withUser("cuscav") { withFakeTime(aprilFool) { new ValidationFixture {
		validator.meetingDateTime = marchHare
		validator.meetingDateStr = marchHare.toString(DatePickerFormatter)
		validator.meetingTimeStr =  "09:00:00"
		validator.meetingEndTimeStr = ""
		validator.title = "A good title"
		validator.format = FaceToFace

		var errors = new BindException(validator, "command")
		validator.validate(errors)
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("meetingEndTimeStr")
		errors.getFieldError.getCode should be ("meetingRecord.endtime.missing")
	}}}

	@Test
	def invalidEmptyTitle(): Unit = withUser("cuscav") { withFakeTime(aprilFool) { new ValidationFixture {
		validator.meetingDateTime = marchHare
		validator.meetingDateStr = validator.meetingDateTime.toString(DatePickerFormatter)
		validator.meetingTimeStr = validator.meetingDateTime.toString(TimePickerFormatter)
		validator.meetingEndTimeStr = validator.meetingDateTime.plusHours(1).toString(TimePickerFormatter)
		validator.format = FaceToFace
		validator.title = ""

		var errors = new BindException(validator, "command")
		validator.validate(errors)
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("title")
		errors.getFieldError.getCode should be ("NotEmpty")
	}}}


	@Test
	def invalidEmptyFormat(): Unit = withUser("cuscav") { withFakeTime(aprilFool) { new ValidationFixture {
		validator.meetingDateTime = marchHare
		validator.meetingDateStr = validator.meetingDateTime.toString(DatePickerFormatter)
		validator.meetingTimeStr = validator.meetingDateTime.toString(TimePickerFormatter)
		validator.meetingEndTimeStr = validator.meetingDateTime.plusHours(1).toString(TimePickerFormatter)
		validator.title = "A good title"
		validator.format = null

		var errors = new BindException(validator, "command")
		validator.validate(errors)
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("format")
		errors.getFieldError.getCode should be ("NotEmpty")
	}}}

	@Test
	def valid(): Unit = withUser("cuscav") { withFakeTime(aprilFool) { new ValidationFixture {
		validator.meetingDateTime = marchHare
		validator.meetingDateStr = validator.meetingDateTime.toString(DatePickerFormatter)
		validator.meetingTimeStr = validator.meetingDateTime.toString(TimePickerFormatter)
		validator.meetingEndTimeStr = validator.meetingDateTime.plusHours(1).toString(TimePickerFormatter)
		validator.title = "A good title"
		validator.format = Email

		var errors = new BindException(validator, "command")
		validator.validate(errors)
		errors.hasErrors should be (false)
	}}}

	@Test
	def validMeeting() = withUser("cuscav") { withFakeTime(aprilFool) {
		val cmd = new CreateMeetingRecordCommandInternal(thisCreator, thisRelationship)
			with MeetingRecordCommandRequest
			with CreateMeetingRecordCommandState
			with MeetingRecordServiceComponent
			with FeaturesComponent
			with AttendanceMonitoringMeetingRecordServiceComponent
			with FileAttachmentServiceComponent {
			override val meetingRecordService: MeetingRecordService = smartMock[MeetingRecordService]
			override val features: Features = Features.empty
			override val attendanceMonitoringMeetingRecordService: AttendanceMonitoringMeetingRecordService = smartMock[AttendanceMonitoringMeetingRecordService]
			override val fileAttachmentService: FileAttachmentService = smartMock[FileAttachmentService]
		}

		cmd.title = "A good title"
		cmd.format = Email
		cmd.meetingDateTime = marchHare
		cmd.meetingDateStr = cmd.meetingDateTime.toString(DatePickerFormatter)
		cmd.meetingTimeStr = cmd.meetingDateTime.toString(TimePickerFormatter)
		cmd.meetingEndTimeStr = cmd.meetingDateTime.plusHours(1).toString(TimePickerFormatter)

		cmd.description = "Lovely words"

		// try adding a file
		val uploadedFile =  new UploadedFile
		val mpFile = smartMock[MultipartFile]
		uploadedFile.upload.add(mpFile)

		val fileAttach = new FileAttachment
		fileAttach.name = "Beltane.txt"
 		fileAttach.fileDao = smartMock[FileDao]
		uploadedFile.attached.add(fileAttach)
		cmd.file = uploadedFile

		val meeting = cmd.applyInternal()

		meeting.creator should be (thisCreator)
		meeting.creationDate should be (aprilFool)
		meeting.lastUpdatedDate should be (aprilFool)
		meeting.title should be ("A good title")
		meeting.description should be ("Lovely words")
		meeting.meetingDate should be (marchHare)
		meeting.attachments.get(0).name should be ("Beltane.txt")
		meeting.format should be (Email)
	}}
}
