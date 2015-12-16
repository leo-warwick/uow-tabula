package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.Fixtures
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.MockUserLookup
import uk.ac.warwick.tabula.services.FileAttachmentService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.{StudentsChooseMarkerWorkflow, FileAttachment}
import uk.ac.warwick.tabula.data.FileDao

// scalastyle:off magic.number
class AddMarkerFeedbackCommandTest extends TestBase with Mockito {

	var service: FileAttachmentService = mock[FileAttachmentService]
	service.getData(null) returns None

	val module = Fixtures.module("cs118")
	val assignment = Fixtures.assignment("my assignment")
	assignment.module = module
	assignment.markingWorkflow = new StudentsChooseMarkerWorkflow

	val userLookup = new MockUserLookup
	val user = new User("student")
	user.setFoundUser(true)
	user.setWarwickId("1010101")
	userLookup.users += ("student" -> user)

	/**
	 * TAB-535
	 */
	@Test def duplicateFileNamesInParent() = withUser("cuscav") {
		val cmd = new AddMarkerFeedbackCommand(module, assignment, currentUser.apparentUser, currentUser)
		cmd.userLookup = userLookup

		cmd.uniNumber = "1010101"

		val file = new UploadedFile
		val a = new FileAttachment
		a.name = "file.txt"
		a.fileAttachmentService = service
		a.uploadedDataLength = 300
		file.attached.add(a)

		val item = new FeedbackItem("1010101")
		item.file = file
		cmd.items.add(item)

		// Add an existing feedback with the same name
		val feedback = Fixtures.assignmentFeedback("1010101")
		feedback.addAttachment(a)
		assignment.feedbacks.add(feedback)
		feedback.assignment = assignment

		item.submissionExists should be (false)

		val errors = new BindException(cmd, "command")
		cmd.postExtractValidation(errors)

		errors.hasErrors should be (false)

		// This should STILL be false, MarkerFeedback shouldn't match against normal feedback!
		item.submissionExists should be (false)
		item.duplicateFileNames should be ('empty)
	}

	@Test def duplicateFileNames() = withUser("cuscav") {
		val cmd = new AddMarkerFeedbackCommand(module, assignment, currentUser.apparentUser, currentUser)
		cmd.userLookup = userLookup

		cmd.uniNumber = "1010101"

		val file = new UploadedFile
		val a = new FileAttachment
		a.name = "file.txt"
		a.fileAttachmentService = service
		a.uploadedDataLength = 300
		file.attached.add(a)

		val item = new FeedbackItem("1010101")
		item.file = file
		cmd.items.add(item)

		// Add an existing feedback with the same name
		val feedback = Fixtures.assignmentFeedback("1010101")
		feedback.firstMarkerFeedback = Fixtures.markerFeedback(feedback)
		feedback.firstMarkerFeedback.addAttachment(a)
		assignment.feedbacks.add(feedback)
		feedback.assignment = assignment

		item.submissionExists should be (false)

		val errors = new BindException(cmd, "command")
		cmd.postExtractValidation(errors)

		errors.hasErrors should be (false)

		item.submissionExists should be (true)
		item.duplicateFileNames should be (Set("file.txt"))
	}

}