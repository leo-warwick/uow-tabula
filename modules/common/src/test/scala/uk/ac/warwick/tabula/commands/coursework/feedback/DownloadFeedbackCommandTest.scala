package uk.ac.warwick.tabula.commands.coursework.feedback

import org.springframework.validation.BindException
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment}
import uk.ac.warwick.tabula.services.{FileAttachmentService, UserLookupService, ZipService}
import uk.ac.warwick.userlookup.{AnonymousUser, User}


class DownloadFeedbackCommandTest extends TestBase with Mockito {

	trait Fixture {
		var userDatabase = Seq(new User())
		var userLookup: UserLookupService = smartMock[UserLookupService]
		userLookup.getUserByUserId(any[String]) answers { id =>
			userDatabase find {_.getUserId == id} getOrElse new AnonymousUser()
		}
		userLookup.getUserByWarwickUniId(any[String]) answers { id =>
			userDatabase find {_.getWarwickId == id} getOrElse new AnonymousUser()
		}

		val department = Fixtures.department(code="ls", name="Life Sciences")
		val module = Fixtures.module(code="ls101")
		val assignment = new Assignment
		val feedback = Fixtures.assignmentFeedback("0123456")
		feedback.id = "123"

		department.postLoad // force legacy settings
		module.adminDepartment = department
		assignment.module = module
		assignment.addFeedback(feedback)

		val attachment = new FileAttachment
		attachment.id = "123"
		attachment.fileAttachmentService = smartMock[FileAttachmentService]
		attachment.fileAttachmentService.getData(attachment.id) returns Option(createTemporaryFile())
		attachment.name = "0123456-feedback.doc"
		feedback.attachments.add(attachment)
	}

	@Test def applyCommand() { new Fixture { withUser("custard") {
		feedback.assignment.feedbacks.size should be (1)

		val command = new DownloadFeedbackCommand(feedback.assignment.module, feedback.assignment, feedback, Some(Fixtures.student("123")))
		command.zip = new ZipService
		command.zip.userLookup = userLookup
		command.zip.features = Features.empty
		command.zip.zipDir = createTemporaryDirectory()

		command.filename = attachment.name

		val errors = new BindException(command, "command")
		withClue(errors) { errors.hasErrors should be {false} }

		command.applyInternal().get.filename should be (attachment.name)

		command.filename = null
		command.applyInternal().get.file.isDefined should be {true}
	}}}

}