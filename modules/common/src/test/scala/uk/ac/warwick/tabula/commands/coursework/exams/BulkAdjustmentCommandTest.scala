package uk.ac.warwick.tabula.commands.coursework.exams


import java.io.InputStream

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model.{GradeBoundary, Assignment, FileAttachment, Assessment}
import uk.ac.warwick.tabula.commands.exams.{BulkAdjustmentValidation, BulkAdjustmentCommand, BulkAdjustmentCommandState, BulkAdjustmentCommandBindListener}
import uk.ac.warwick.tabula.helpers.SpreadsheetHelpers
import uk.ac.warwick.tabula.services.{FileAttachmentService, MaintenanceModeService, GeneratesGradesFromMarks}
import uk.ac.warwick.tabula.{Fixtures, CurrentUser, Mockito, TestBase}

class BulkAdjustmentCommandTest extends TestBase with Mockito {

	trait BindFixture {
		val mockFileAttachmentService = smartMock[FileAttachmentService]
		mockFileAttachmentService.getData(any[String]) returns None
		val mockSpreadsheetHelper = smartMock[SpreadsheetHelpers]
		val thisAssessment = new Assignment
		val feedback = Fixtures.assignmentFeedback("1234")
		thisAssessment.feedbacks.add(feedback)
		val bindListener = new BulkAdjustmentCommandBindListener with BulkAdjustmentCommandState {
			override def assessment: Assessment = thisAssessment
			override def user: CurrentUser = null
			override def gradeGenerator: GeneratesGradesFromMarks = null
			override def spreadsheetHelper = mockSpreadsheetHelper
		}
		val file = new UploadedFile
		file.maintenanceModeService = smartMock[MaintenanceModeService]
		val attachment = new FileAttachment
		attachment.name = "file.xlsx"
		file.attached.add(attachment)
		bindListener.file = file
		file.fileAttachmentService = mockFileAttachmentService
		attachment.fileAttachmentService = mockFileAttachmentService
	}

	@Test
	def bindValidationBadExtension() { new BindFixture {
		val badFile = new UploadedFile
		val badAttachment = new FileAttachment
		badAttachment.name = "file.txt"
		badFile.attached.add(badAttachment)
		bindListener.file = badFile
		val errors = new BindException(bindListener, "command")
		bindListener.onBind(errors)
		errors.hasFieldErrors("file") should be {true}
	}}

	@Test
	def bindValidationExtractDataNoStudentHeader() { new BindFixture {
		mockSpreadsheetHelper.parseXSSFExcelFile(any[InputStream], any[Boolean]) returns Seq(
			Map("someheader" -> "123")
		)
		val errors = new BindException(bindListener, "command")
		bindListener.onBind(errors)
		errors.hasFieldErrors("file") should be {false}
		bindListener.ignoredRows.size should be (1)
	}}

	@Test
	def bindValidationExtractDataInvalidStudentIdFormat() { new BindFixture {
		mockSpreadsheetHelper.parseXSSFExcelFile(any[InputStream], any[Boolean]) returns Seq(
			Map(BulkAdjustmentCommand.StudentIdHeader.toLowerCase -> "student")
		)
		val errors = new BindException(bindListener, "command")
		bindListener.onBind(errors)
		errors.hasFieldErrors("file") should be {false}
		bindListener.ignoredRows.size should be (1)
	}}

	@Test
	def bindValidationExtractDataInvalidStudentNoFeedback() { new BindFixture {
		mockSpreadsheetHelper.parseXSSFExcelFile(any[InputStream], any[Boolean]) returns Seq(
			Map(BulkAdjustmentCommand.StudentIdHeader.toLowerCase -> "2345")
		)
		val errors = new BindException(bindListener, "command")
		bindListener.onBind(errors)
		errors.hasFieldErrors("file") should be {false}
		bindListener.ignoredRows.size should be (1)
	}}

	@Test
	def bindValidationExtractDataValidStudent() { new BindFixture {
		val studentId = thisAssessment.allFeedback.head.universityId
		mockSpreadsheetHelper.parseXSSFExcelFile(any[InputStream], any[Boolean]) returns Seq(
			Map(
				BulkAdjustmentCommand.StudentIdHeader.toLowerCase -> studentId,
				BulkAdjustmentCommand.MarkHeader.toLowerCase -> "100",
				BulkAdjustmentCommand.GradeHeader.toLowerCase -> "A",
				BulkAdjustmentCommand.ReasonHeader.toLowerCase -> "Reason",
				BulkAdjustmentCommand.CommentsHeader.toLowerCase -> "Comments"
			)
		)
		val errors = new BindException(bindListener, "command")
		bindListener.onBind(errors)
		errors.hasFieldErrors("file") should be {false}
		bindListener.ignoredRows.isEmpty should be {true}
		bindListener.students.size should be (1)
		bindListener.marks.get(studentId) should be ("100")
		bindListener.grades.get(studentId) should be ("A")
		bindListener.reasons.get(studentId) should be ("Reason")
		bindListener.comments.get(studentId) should be ("Comments")
	}}

	trait ValidateFixture {
		val dept = Fixtures.department("its")
		val module = Fixtures.module("it101")
		val thisAssessment = new Assignment
		thisAssessment.module = module
		module.adminDepartment = dept
		val feedback = Fixtures.assignmentFeedback("1234")
		thisAssessment.feedbacks.add(feedback)
		val mockGradeGenerator = smartMock[GeneratesGradesFromMarks]
		mockGradeGenerator.applyForMarks(Map("1234" -> 100)) returns Map("1234" -> Seq(GradeBoundary(null, "A", 0, 0, null)))
		val validator = new BulkAdjustmentValidation with BulkAdjustmentCommandState {
			override def assessment: Assessment = thisAssessment
			override def user: CurrentUser = null
			override def gradeGenerator: GeneratesGradesFromMarks = mockGradeGenerator
			override def spreadsheetHelper = null
		}
		validator.students.add("1234")
		val errors = new BindException(validator, "command")
	}

	@Test
	def validateNoMark() { new ValidateFixture {
		validator.validate(errors)
		errors.hasFieldErrors("marks[1234]") should be {true}
	}}

	@Test
	def validateBlankMark() { new ValidateFixture {
		validator.marks.put("1234", "")
		validator.validate(errors)
		errors.hasFieldErrors("marks[1234]") should be {true}
	}}

	@Test
	def validateInvalidMark() { new ValidateFixture {
		validator.marks.put("1234", "101")
		validator.validate(errors)
		errors.hasFieldErrors("marks[1234]") should be {true}
	}}

	@Test
	def validateInvalidMark2() { new ValidateFixture {
		validator.marks.put("1234", "one")
		validator.validate(errors)
		errors.hasFieldErrors("marks[1234]") should be {true}
	}}

	@Test
	def validateInvalidGradeNoValidation() { new ValidateFixture {
		dept.assignmentGradeValidation = false
		validator.marks.put("1234", "100")
		validator.grades.put("1234", "F")
		validator.validate(errors)
		errors.hasFieldErrors("marks[1234]") should be {false}
		errors.hasFieldErrors("grades[1234]") should be {false}
	}}

	@Test
	def validateEmptyGrade() { new ValidateFixture {
		dept.assignmentGradeValidation = true
		validator.marks.put("1234", "100")
		validator.grades.put("1234", "")
		validator.validate(errors)
		errors.hasFieldErrors("marks[1234]") should be {false}
		errors.hasFieldErrors("grades[1234]") should be {false}
	}}

	@Test
	def validateInvalidGrade() { new ValidateFixture {
		dept.assignmentGradeValidation = true
		validator.marks.put("1234", "100")
		validator.grades.put("1234", "F")
		validator.validate(errors)
		errors.hasFieldErrors("marks[1234]") should be {false}
		errors.hasFieldErrors("grades[1234]") should be {true}
	}}

	@Test
	def validateReasonTooLong() { new ValidateFixture {
		validator.reasons.put("1234",
			"""
				Bacon ipsum dolor amet labore leberkas sausage pork loin. Pork belly fugiat tempor ad.
				Kielbasa adipisicing short ribs nisi jowl. Qui short loin corned beef dolor ut. Ut do flank ad.
				Qui ullamco in aliquip meatball. Spare ribs fugiat chuck pancetta meatball capicola, meatloaf tail brisket.
				Bacon ipsum dolor amet labore leberkas sausage pork loin. Pork belly fugiat tempor ad.
				Kielbasa adipisicing short ribs nisi jowl. Qui short loin corned beef dolor ut. Ut do flank ad.
				Qui ullamco in aliquip meatball. Spare ribs fugiat chuck pancetta meatball capicola, meatloaf tail brisket.
				Spare ribs fugiat chuck pancetta meatball capicola, meatloaf tail brisket.
			""")
		validator.validate(errors)
		errors.hasFieldErrors("reasons[1234]") should be {true}
	}}

	@Test
	def validateNoDefaultReason() { new ValidateFixture {
		validator.confirmStep = true
		validator.reasons.put("1234", "")
		validator.comments.put("1234", "")
		validator.validate(errors)
		errors.hasFieldErrors("defaultReason") should be {true}
		errors.hasFieldErrors("defaultComment") should be {true}
	}}

}
