package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.services.{AutowiringSubmissionServiceComponent, AutowiringZipServiceComponent}

import scala.collection.JavaConverters._
import org.joda.time.DateTime
import org.springframework.mock.web.MockMultipartFile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model.forms.{FileField, FileFormValue, FormFieldContext}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent


class SubmitAssignmentCommandTest extends TestBase with Mockito {

	@Autowired var dao: FileDao = _

	@Test def plagiarism() = withUser(code = "cusebr", universityId = "0678022") {
		val assignment = newActiveAssignment
		val user = RequestInfo.fromThread.get.user
		val cmd = SubmitAssignmentCommand.self(assignment.module, assignment, user)
		cmd.features = emptyFeatures
		cmd.features.disabilityOnSubmission = true

		// no plagiarism box ticked
		var errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getFieldErrors.asScala.head.getField should be ("plagiarismDeclaration")
		errors.getFieldErrors.asScala.head.getCodes should contain ("assignment.submit.plagiarism")

		// oops, sorry, yes, it's totally mine
		cmd.plagiarismDeclaration = true

		errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be {false}
	}

	@Test def multipleSubmissions() = withUser(code = "cusebr", universityId = "0678022") {
		val assignment = newActiveAssignment
		val user = RequestInfo.fromThread.get.user
		val cmd = SubmitAssignmentCommand.self(assignment.module, assignment, user)
		cmd.features = emptyFeatures
		cmd.features.disabilityOnSubmission = true

		// scenario
		assignment.allowResubmission = false
		cmd.plagiarismDeclaration = true

		var errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be {false}

		val submission = new Submission()
		submission.assignment = assignment
		submission._universityId = "0678022"
		submission.usercode = "cusebr"
		assignment.submissions.add(submission)

		// Can't submit twice, silly
		errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be {true}

		// But guys, guys... what if...
		assignment.allowResubmission = true
		errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be{false}
	}

	@Test def fileTypeValidation() = withUser(code = "cusebr", universityId = "0678022") {

		val user = RequestInfo.fromThread.get.user
		val assignment = newActiveAssignment
		val file = new FileField
		file.name = "upload"
		file.id = "upload"
		file.context = FormFieldContext.Submission
		file.attachmentLimit = 4
		file.attachmentTypes = Seq("doc", "docx", "pdf")
		assignment.addField(file)

		// common reusable setup
		trait Setup {
			val cmd: SubmitAssignmentCommandInternal with ComposableCommand[Submission] with SubmitAssignmentBinding with SubmitAssignmentAsSelfPermissions with SubmitAssignmentDescription with SubmitAssignmentValidation with SubmitAssignmentNotifications with SubmitAssignmentTriggers with AutowiringSubmissionServiceComponent with AutowiringFeaturesComponent with AutowiringZipServiceComponent with AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent = SubmitAssignmentCommand.self(assignment.module, assignment, user)
			cmd.features = emptyFeatures
			cmd.features.disabilityOnSubmission = true
			// pre-tick the box
			cmd.plagiarismDeclaration = true
			var errors = new BindException(cmd, "command")
			val submissionValue: FileFormValue = cmd.fields.get("upload").asInstanceOf[FileFormValue]
		}

		new Setup {
			val document: Array[Byte] = resourceAsBytes("attachment1.docx")
			submissionValue.file.upload add new MockMultipartFile("attachment1.docx", "attachment1.docx", null, document)
			cmd.validate(errors)
			errors.hasErrors should be{false}
		}

		new Setup {
			val document2: Array[Byte] = resourceAsBytes("attachment2.doc")
			submissionValue.file.upload add new MockMultipartFile("attachment2.doc", "attachment2.doc", null, document2)
			cmd.validate(errors)
			errors.hasErrors should be{false}
		}

		new Setup {
			val pdf: Array[Byte] = resourceAsBytes("attachment3.pdf")
			submissionValue.file.upload add new MockMultipartFile("attachment3.pdf", "attachment3.PDF", null, pdf)
			cmd.validate(errors)
			errors.hasErrors should be{false}
		}

		new Setup {
			val csv: Array[Byte] = resourceAsBytes("attachment4.csv")
			submissionValue.file.upload add new MockMultipartFile("attachment4.csv", "attachment4.csv", null, csv)
			cmd.validate(errors)
			errors.hasErrors should be{true}
			errors.getFieldError("fields[upload].file").getCode should be("file.wrongtype.one")
		}

		new Setup {
			val pdf: Array[Byte] = resourceAsBytes("attachment3.pdf")
			submissionValue.file.upload add new MockMultipartFile("attachment3.pdf", "attachment3.pdf", null, pdf)
			submissionValue.file.upload add new MockMultipartFile("attachment3.pdf", "attachment3.pdf", null, pdf)
			cmd.validate(errors)
			errors.hasErrors should be{true}
			errors.getFieldError("fields[upload].file").getCode should be("file.duplicate")
		}
	}

	@Test def useDisability() = {
		val student = Fixtures.student(universityId = "0678022", userId = "cusebr")
		student.disability = Fixtures.disability("Test")

		withUser(code = "cusebr", universityId = "0678022", profile = Some(student)) {
			val assignment = newActiveAssignment
			val user = RequestInfo.fromThread.get.user
			val cmd = SubmitAssignmentCommand.self(assignment.module, assignment, user)

			cmd.features = emptyFeatures
			cmd.features.disabilityOnSubmission = true
			cmd.plagiarismDeclaration = true

			// no disability use selected
			var errors = new BindException(cmd, "command")
			cmd.validate(errors)
			errors.hasErrors should be {true}
			errors.getErrorCount should be (1)
			errors.getFieldErrors.asScala.head.getField should be ("useDisability")
			errors.getFieldErrors.asScala.head.getCodes should contain ("assignment.submit.chooseDisability")

			// oops, sorry, yes
			cmd.useDisability = true

			errors = new BindException(cmd, "command")
			cmd.validate(errors)
			errors.hasErrors should be {false}
		}
	}

	private def newActiveAssignment = {
		val assignment = new Assignment
		assignment.setDefaultBooleanProperties()
		assignment.openDate = new DateTime().minusWeeks(1)
		assignment.closeDate = new DateTime().plusWeeks(1)
		assignment.collectSubmissions = true
		assignment.module = new Module
		assignment
	}
}