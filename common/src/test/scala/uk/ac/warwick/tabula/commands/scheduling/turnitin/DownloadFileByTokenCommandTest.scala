package uk.ac.warwick.tabula.commands.scheduling.turnitin

import org.joda.time.DateTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{FileAttachment, FileAttachmentToken, Submission}
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class DownloadFileByTokenCommandTest extends TestBase with Mockito {

	trait Fixture {
		val attachment = new FileAttachment
		val attachmentId = "attachment"
		attachment.id = attachmentId

		val submission: Submission = Fixtures.submissionWithId("0000001", id = "submission")

		val sv = new SavedFormValue
		sv.submission = submission

		attachment.submissionValue = sv

		val token: FileAttachmentToken = attachment.generateToken()

		val command = DownloadFileByTokenCommand(submission, attachment, token)

		val errors = new BindException(command, "command")
	}


	@Test
	def validToken() { new Fixture {
		command.validate(errors)
		errors.hasErrors should be {false}
	}}

	@Test
	def invalidUsedToken() { new Fixture {
		token.dateUsed = new DateTime()
		command.validate(errors)
		errors.hasErrors should be {true}
	}}

	@Test
	def invalidExpiredToken() { new Fixture {
		token.expires = new DateTime().minusMinutes(1)
		command.validate(errors)
		errors.hasErrors should be {true}
	}}

	@Test
	def invalidAccessToken() { new Fixture {
		token.fileAttachmentId = "wrong id"
		command.validate(errors)
		errors.hasErrors should be {true}
	}}

}