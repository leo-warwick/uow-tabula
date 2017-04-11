package uk.ac.warwick.tabula.commands.coursework.assignments

import com.google.common.io.ByteSource
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Fixtures
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.MockUserLookup
import uk.ac.warwick.tabula.services.objectstore.ObjectStorageService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment, Module}
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.Mockito

// scalastyle:off magic.number
class AddFeedbackCommandTest extends TestBase with Mockito {

	var objectStorageService: ObjectStorageService = smartMock[ObjectStorageService]

	// Start from the basis that the store is empty
	objectStorageService.fetch(any[String]) returns None
	objectStorageService.metadata(any[String]) returns None

	val module: Module = Fixtures.module("cs118")
	val assignment: Assignment = Fixtures.assignment("my assignment")
	assignment.module = module

	val userLookup = new MockUserLookup
	val user = new User("student")
	user.setFoundUser(true)
	user.setWarwickId("1010101")
	userLookup.users += ("student" -> user)

	@Test def duplicateFileNames() = withUser("cuscav") {
		val cmd = new AddFeedbackCommand(module, assignment, currentUser.apparentUser, currentUser)
		cmd.userLookup = userLookup
		cmd.fileDao = smartMock[FileDao]

		val file = new UploadedFile
		val a = new FileAttachment
		a.name = "file.txt"
		a.uploadedData = ByteSource.wrap("one".getBytes)
		a.objectStorageService = objectStorageService
		file.attached.add(a)

		val b = new FileAttachment
		b.name = "file2.txt"
		b.uploadedData = ByteSource.wrap("one".getBytes)
		b.objectStorageService = objectStorageService
		file.attached.add(b)

		val item = new FeedbackItem("1010101", user)
		item.file = file
		cmd.items.add(item)

		// Add an existing feedback with the same name and content - will be ignored
		val feedback = Fixtures.assignmentFeedback("1010101")
		feedback.addAttachment(a)

		// Add an existing feedback with the same name but different content - will be overwritten
		val b2 = new FileAttachment
		b2.name = "file2.txt"
		b2.uploadedData = ByteSource.wrap("magic".getBytes)
		b2.objectStorageService = objectStorageService
		feedback.addAttachment(b2)

		assignment.feedbacks.add(feedback)

		item.submissionExists should be (false)

		val errors = new BindException(cmd, "command")
		cmd.postExtractValidation(errors)

		errors.hasErrors should be (false)

		item.submissionExists should be (true)
		item.ignoredFileNames should be (Set("file.txt"))
		item.duplicateFileNames should be (Set("file2.txt"))
	}

}