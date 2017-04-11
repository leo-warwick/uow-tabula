package uk.ac.warwick.tabula.commands.coursework.assignments

import com.google.common.io.ByteSource
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.objectstore.ObjectStorageService
import uk.ac.warwick.userlookup.User

// scalastyle:off magic.number
class AddMarkerFeedbackCommandTest extends TestBase with Mockito {

	var objectStorageService: ObjectStorageService = smartMock[ObjectStorageService]

	// Start from the basis that the store is empty
	objectStorageService.fetch(any[String]) returns None
	objectStorageService.metadata(any[String]) returns None

	val module: Module = Fixtures.module("cs118")
	val assignment: Assignment = Fixtures.assignment("my assignment")
	assignment.module = module
	assignment.markingWorkflow = new OldStudentsChooseMarkerWorkflow

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

		val file = new UploadedFile
		val a = new FileAttachment
		a.name = "file.txt"
		a.objectStorageService = objectStorageService
		a.uploadedData = ByteSource.wrap("content".getBytes)
		file.attached.add(a)

		val item = new FeedbackItem("1010101", user)
		item.file = file
		cmd.items.add(item)

		// Add an existing feedback with the same name
		val feedback = Fixtures.assignmentFeedback("1010101", "student")
		feedback.addAttachment(a)
		assignment.feedbacks.add(feedback)
		feedback.assignment = assignment
		val mf = new MarkerFeedback(feedback)
		feedback.firstMarkerFeedback = mf

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

		val file = new UploadedFile
		val a = new FileAttachment
		a.name = "file.txt"
		a.objectStorageService = objectStorageService
		a.uploadedData = ByteSource.wrap("content".getBytes)
		file.attached.add(a)

		val item = new FeedbackItem("1010101", user)
		item.file = file
		cmd.items.add(item)

		// Add an existing feedback with the same name
		val feedback = Fixtures.assignmentFeedback("1010101", "student")
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