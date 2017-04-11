package uk.ac.warwick.tabula.commands.coursework.assignments.extensions

import scala.collection.JavaConversions._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.forms.{Extension, ExtensionState}
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment}
import uk.ac.warwick.tabula.services.{UserLookupComponent, UserLookupService}
import uk.ac.warwick.tabula.{CurrentUser, Mockito, RequestInfo, TestBase}
import uk.ac.warwick.userlookup.User

// scalastyle:off magic.number
class EditExtensionCommandTest extends TestBase {

	def createUser(userId: String, warwickId:String): User = {
		val user = new User(userId)
		user.setFoundUser(true)
		user.setWarwickId(warwickId)
		user
	}
	
	@Test
	def addExtension() {
		withUser("cuslat", "1171795") {
			withFakeTime(dateTime(2014, 2, 11)) {

				val assignment = createAssignment()
				val student = createUser("1234567", "u1234567")
				val extension = createExtension(assignment, student)

				extension.approved should be (false)
				extension.rejected should be (false)
				extension.state should be (ExtensionState.Unreviewed)
				extension.reviewerComments should be (null)
			}
		}
	}

	@Test
	def approveExtension() {
		withUser("cuslat", "1171795") {
			withFakeTime(dateTime(2014, 2, 11)) {

				val currentUser = RequestInfo.fromThread.get.user
				val assignment = createAssignment()
				val student = createUser("1234567", "u1234567")
				val extension = createExtension(assignment, student)

				extension.approved should be (false)
				extension.rejected should be (false)
				extension.state should be (ExtensionState.Unreviewed)
				extension.reviewerComments should be (null)

				val reviewerComments = "I've always thought that Tabula should have a photo sharing component"

				val editCommand = new EditExtensionCommandInternal(assignment.module, assignment, student, currentUser, "Grant") with EditExtensionCommandTestSupport
				editCommand.reviewerComments = reviewerComments
				val result = editCommand.apply()

				result.approved should be (true)
				result.rejected should be (false)
				result.state should be (ExtensionState.Approved)
				result.reviewedOn should be (DateTime.now)
				result.reviewerComments should be (reviewerComments)
				result._universityId should be (student.getWarwickId)
			}
		}
	}

	@Test
	def rejectExtension() {
		withUser("cuslat", "1171795") {
			withFakeTime(dateTime(2014, 2, 11)) {

				val currentUser = RequestInfo.fromThread.get.user
				val assignment = createAssignment()
				val student = createUser("1234567", "u1234567")
				val extension = createExtension(assignment, student)

				extension.approved should be (false)
				extension.rejected should be (false)
				extension.state should be (ExtensionState.Unreviewed)
				extension.reviewerComments should be (null)

				val reviewerComments = "something something messaging service something something $17 billion cheers thanks"

				val editCommand = new EditExtensionCommandInternal(assignment.module, assignment, student, currentUser, "Reject") with EditExtensionCommandTestSupport
				editCommand.reviewerComments = reviewerComments
				val result = editCommand.apply()

				result.approved should be (false)
				result.rejected should be (true)
				result.state should be (ExtensionState.Rejected)
				result.reviewedOn should be (DateTime.now)
				result.reviewerComments should be (reviewerComments)
				result.usercode should be (student.getUserId)
			}
		}
	}


	@Test
	def revokeExtension() {
		withUser("cuslat", "1171795") {
			withFakeTime(dateTime(2014, 2, 11)) {

				val currentUser = RequestInfo.fromThread.get.user
				val assignment = createAssignment()
				val student = createUser("1234567", "u1234567")
				val extension = createExtension(assignment, student)

				extension.approved should be (false)
				extension.rejected should be (false)
				extension.state should be (ExtensionState.Unreviewed)

				assignment.extensions.add(extension)

				assignment.extensions.size should be (2)
				val deleteCommand = new DeleteExtensionCommandInternal(assignment.module, assignment, student, currentUser) with DeleteExtensionCommandTestSupport
				val result = deleteCommand.apply()
				assignment.extensions.size should be (1)

				result.approved should be (false)
				result.rejected should be (false)
				result.state should be (ExtensionState.Revoked)
				deleteCommand.deleted should be (true)
			}
		}
	}

	@Test
	def revokeExtensionEmit() {
		withUser("cuslat", "1171795") {

			val deleteCommand = new DeleteExtensionCommandNotification with ModifyExtensionCommandState {
				val currentUser: CurrentUser = RequestInfo.fromThread.get.user
				submitter = currentUser
				assignment = createAssignment()
				assignment.extensions.add(extension)
				student = createUser("1234567", "u1234567")
				extension = createExtension(assignment, student)
			}

			val emitted = deleteCommand.emit(deleteCommand.extension)
			emitted.size should be (1)
			emitted.head.recipientUniversityId should be ("1234567")
			emitted.head.entities.head should be (deleteCommand.assignment)
		}
	}

	def createAssignment(): Assignment = {
		val assignment = newDeepAssignment()
		assignment.closeDate = DateTime.now.plusMonths(1)
		assignment.extensions += new Extension {
			_universityId = currentUser.universityId
			usercode = currentUser.userId
		}
		assignment
	}

	def createExtension(assignment: Assignment, student: User, reviewerComments: String = "") : Extension = {
		val extension = new Extension
		extension.usercode = student.getUserId
		extension._universityId = student.getWarwickId
		extension.assignment = assignment
		extension.expiryDate = DateTime.now.plusMonths(2)
		if(reviewerComments.isEmpty)
			extension.reviewerComments = null
		else
			extension.reviewerComments = reviewerComments
		extension
	}

}

trait ModifyExtensionCommandTestSupport extends UserLookupComponent
with ExtensionPersistenceComponent
with ModifyExtensionCommandState
with Mockito {

	var userLookup: UserLookupService = mock[UserLookupService]
	val testuser = new User("cuslat")
	var deleted = false
	testuser.setWarwickId("1171975")

	userLookup.getUserByWarwickUniId(any[String]) answers { id => testuser }

	def delete(attachment: FileAttachment) {}
	def delete(extension: Extension) { }
	def save(extension: Extension) {}

}

trait EditExtensionCommandTestSupport extends ModifyExtensionCommandTestSupport {

	this : EditExtensionCommandInternal =>

	def apply(): Extension = this.applyInternal()
}

trait DeleteExtensionCommandTestSupport extends ModifyExtensionCommandTestSupport {

	this : DeleteExtensionCommandInternal =>

	def apply(): Extension = this.applyInternal()

	override def delete(extension: Extension) { deleted = true }
}