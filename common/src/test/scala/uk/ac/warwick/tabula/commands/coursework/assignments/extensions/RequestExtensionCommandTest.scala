package uk.ac.warwick.tabula.commands.coursework.assignments.extensions

import org.joda.time.DateTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.services.{FileAttachmentService, FileAttachmentServiceComponent, RelationshipService, RelationshipServiceComponent}
import uk.ac.warwick.tabula.{Mockito, RequestInfo, TestBase}

import scala.collection.JavaConversions._

// scalastyle:off magic.number
class RequestExtensionCommandTest extends TestBase with Mockito {

	@Test
	def itWorks() {
		withUser("cuslat", "1171795") {
			withFakeTime(dateTime(2014, 2, 11)) {

				val currentUser = RequestInfo.fromThread.get.user
				val assignment = newDeepAssignment()
				assignment.closeDate = DateTime.now.plusMonths(1)
				assignment.module.adminDepartment.allowExtensionRequests = true

				val command = new RequestExtensionCommandInternal(assignment.module, assignment, currentUser) with RequestExtensionCommandTestSupport
				command.requestedExpiryDate = DateTime.now.plusMonths(2)
				command.reason  = "Fun fun fun"
				command.readGuidelines = true
				command.disabilityAdjustment = true
				val errors = new BindException(command, "command")
				command.validate(errors)
				errors.hasErrors should be {false}

				var returnedExtension = command.applyInternal()

				returnedExtension.requestedExpiryDate should be (Some(DateTime.now.plusMonths(2)))
				returnedExtension.reason should be ("Fun fun fun")
				returnedExtension.disabilityAdjustment should be {true}
				returnedExtension.requestedOn should be (DateTime.now)
				returnedExtension.approved should be {false}
				returnedExtension.rejected should be {false}
				returnedExtension.reviewedOn should be (null)
				returnedExtension.usercode should be (currentUser.userId)
				returnedExtension.universityId should be (Some(currentUser.universityId))
				returnedExtension.assignment should be (assignment)
				returnedExtension.attachments.isEmpty should be {true}

				// check boolean is correctly propagated
				command.disabilityAdjustment = false
				returnedExtension = command.applyInternal()
				returnedExtension.disabilityAdjustment should be {false}

				command.disabilityAdjustment = null
				returnedExtension = command.applyInternal()
				returnedExtension.disabilityAdjustment should be {false}
			}
		}
	}


	@Test
	def extensionReuse() {
		withUser("cuslat", "1171795") {
			withFakeTime(dateTime(2014, 2, 11)) {

				val currentUser = RequestInfo.fromThread.get.user
				var assignment = newDeepAssignment()
				val newExtension = new Extension {
					_universityId = currentUser.universityId
					usercode = currentUser.userId
				}
				newExtension.approve()
				newExtension.reviewedOn = DateTime.now
				assignment.extensions.add(newExtension)

				var command = new RequestExtensionCommandInternal(assignment.module, assignment, currentUser) with RequestExtensionCommandTestSupport
				var returnedExtension = command.applyInternal()

				returnedExtension.approved should be {true}
				returnedExtension.reviewedOn should be (DateTime.now)

				assignment = newDeepAssignment()
				command = new RequestExtensionCommandInternal(assignment.module, assignment, currentUser) with RequestExtensionCommandTestSupport
				returnedExtension = command.applyInternal()

				returnedExtension.approved should be {false}
				returnedExtension.reviewedOn should be (null)

			}
		}
	}


	@Test
	def validation() {
		withUser("cuslat", "1171795") {
			withFakeTime(dateTime(2014, 2, 11)) {

				val currentUser = RequestInfo.fromThread.get.user
				val assignment = newDeepAssignment()
				assignment.closeDate = DateTime.now.plusMonths(1)

				val command = new RequestExtensionCommandInternal(assignment.module, assignment, currentUser) with RequestExtensionCommandTestSupport
				var errors = new BindException(command, "command")
				errors.hasErrors should be {false}
				command.validate(errors)
				errors.hasErrors should be {true}

				errors.getFieldErrors("readGuidelines").isEmpty should be {false}
				command.readGuidelines = true
				errors = new BindException(command, "command")
				command.validate(errors)
				errors.getFieldErrors("readGuidelines").isEmpty should be {true}

				errors.getFieldErrors("reason").isEmpty should be {false}
				command.reason = "Hello sailor"
				errors = new BindException(command, "command")
				command.validate(errors)
				errors.getFieldErrors("reason").isEmpty should be {true}

				errors.getFieldErrors("requestedExpiryDate").isEmpty should be {false}
				command.requestedExpiryDate = DateTime.now
				errors = new BindException(command, "command")
				command.validate(errors)
				errors.getFieldErrors("requestedExpiryDate").isEmpty should be {false}
				errors.getFieldError("requestedExpiryDate").getCode should be ("extension.requestedExpiryDate.beforeAssignmentExpiry")

				command.requestedExpiryDate = DateTime.now.plusMonths(2)
				errors = new BindException(command, "command")
				command.validate(errors)
				errors.getFieldErrors("requestedExpiryDate").isEmpty should be {true}

				assignment.extensionAttachmentMandatory = true
				errors = new BindException(command, "command")
				command.validate(errors)
				errors.getFieldErrors("file").nonEmpty should be {true}
			}
		}
	}



	@Test
	def attachmentDeletion() {
		withUser("cuslat", "1171795") {
			withFakeTime(dateTime(2014, 2, 11)) {

				val currentUser = RequestInfo.fromThread.get.user
				val assignment = newDeepAssignment()

				val newExtension = new Extension {
					_universityId = currentUser.universityId
					usercode = currentUser.userId
				}
				val attachment = new FileAttachment

				newExtension.addAttachment(attachment)
				assignment.extensions.add(newExtension)

				val command = new RequestExtensionCommandInternal(assignment.module, assignment, currentUser) with RequestExtensionCommandTestSupport
				// populate command's view of attachments
				command.presetValues(newExtension)

				var returnedExtension = command.applyInternal()
				returnedExtension.attachments.head should be (attachment)

				command.attachedFiles.remove(attachment)
				returnedExtension = command.applyInternal()
				returnedExtension.attachments.isEmpty should be {true}
			}
		}
	}





	trait RequestExtensionCommandTestSupport extends FileAttachmentServiceComponent
				with RelationshipServiceComponent
				with ExtensionPersistenceComponent
				with RequestExtensionCommandValidation
				with Mockito {

					this : RequestExtensionCommandInternal =>

					val fileAttachmentService: FileAttachmentService = mock[FileAttachmentService]

					def apply(): Extension = this.applyInternal()

					var relationshipService: RelationshipService = mock[RelationshipService]
					def delete(attachment: FileAttachment) {}
					def delete(extension: Extension) {}
					def save(extension: Extension) {}

	}

}