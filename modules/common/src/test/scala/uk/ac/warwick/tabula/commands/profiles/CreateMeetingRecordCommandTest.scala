package uk.ac.warwick.tabula.commands.profiles

import org.joda.time.DateTimeConstants
import org.springframework.validation.BindException
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.model.MeetingFormat._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.FileAttachmentService

import scala.collection.JavaConverters._

// scalastyle:off magic.number
class CreateMeetingRecordCommandTest extends PersistenceTestBase with MeetingRecordTests {

	@Test
	def validMeeting() = withUser("cuscav") { withFakeTime(aprilFool) {

		val cmd = new CreateMeetingRecordCommand(creator, relationship, false)
		cmd.title = "A title"
		cmd.format = FaceToFace
		cmd.meetingDateTime  = dateTime(3903, DateTimeConstants.MARCH) // it's the future
		cmd.maintenanceModeService = maintenanceModeService
		cmd.notificationService = notificationService
		cmd.scheduledNotificationService = scheduledNotificationService

		// check invalid future date
		var errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be {true}
		errors.getErrorCount should be (2)
		errors.getFieldErrors.asScala.map(_.getField).contains("meetingDateTime") should be {true}
		errors.getFieldError.getCode should be ("meetingRecord.date.future")

		cmd.meetingDateTime = dateTime(2007, DateTimeConstants.MARCH) // > 5 years ago

		// check invalid past date
		errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be {true}
		errors.getErrorCount should be (2)
		errors.getFieldErrors.asScala.map(_.getField).contains("meetingDateTime") should be {true}
		errors.getFieldError.getCode should be ("meetingRecord.date.prehistoric")

		cmd.meetingDateTime = marchHare
		cmd.title = ""
		cmd.features = emptyFeatures
		cmd.features.meetingRecordApproval = true
		cmd.features.attendanceMonitoringMeetingPointType = false

		// check invalid empty title
		errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("title")
		errors.getFieldError.getCode should be ("NotEmpty")

		cmd.title = "A good title"
		cmd.format = null

		// check invalid empty format
		errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("format")
		errors.getFieldError.getCode should be ("NotEmpty")

		cmd.format = Email

		// check valid
		errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be {false}

		// add some text and apply
		cmd.description = "Lovely words"

		// try adding a file
		val uploadedFile =  new UploadedFile
		val mpFile = mock[MultipartFile]
		uploadedFile.upload.add(mpFile)

		val fileAttach = new FileAttachment
		fileAttach.name = "Beltane.txt"

		uploadedFile.attached.add(fileAttach)

		val fileAttachmentService: FileAttachmentService = mock[FileAttachmentService]
		cmd.fileAttachmentService = fileAttachmentService
		cmd.file = uploadedFile
		cmd.meetingRecordService = meetingRecordService

		val meeting = transactional { tx => cmd.apply() }

		meeting.creator should be (creator)
		meeting.creationDate should be (aprilFool)
		meeting.lastUpdatedDate should be (aprilFool)
		meeting.title should be ("A good title")
		meeting.description should be ("Lovely words")
		meeting.meetingDate should be (marchHare)
		meeting.attachments.get(0).name should be ("Beltane.txt")
		meeting.format should be (Email)
	}}
}
