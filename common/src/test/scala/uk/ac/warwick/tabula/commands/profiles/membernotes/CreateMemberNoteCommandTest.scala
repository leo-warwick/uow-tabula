package uk.ac.warwick.tabula.commands.profiles.membernotes

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{AbstractMemberNote, StudentMember}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class CreateMemberNoteCommandTest extends TestBase with Mockito {

	val member: StudentMember = Fixtures.student("12345")
	val mockProfileService: ProfileService = smartMock[ProfileService]
	val mockMemberNoteService: MemberNoteService = smartMock[MemberNoteService]
	val mockFileAttachmentService: FileAttachmentService = smartMock[FileAttachmentService]

	trait ApplyFixture {
		val cmd = new CreateMemberNoteCommandInternal(member, currentUser)
			with CreateMemberNoteCommandState with ModifyMemberNoteCommandRequest
			with FileAttachmentServiceComponent with MemberNoteServiceComponent {

			override val memberNoteService: MemberNoteService = mockMemberNoteService
			override val fileAttachmentService: FileAttachmentService = mockFileAttachmentService
		}
	}

	@Test
	def testApply(): Unit = withUser("cuscao", "09876") { new ApplyFixture {
		val memberNote: AbstractMemberNote = cmd.applyInternal()
		memberNote.member should be (member)
		memberNote.creatorId should be ("09876")
		memberNote.note should be (cmd.note)
	}}

	trait ValidationFixture {
		val validator = new CreateMemberNoteValidation with ModifyMemberNoteCommandRequest
	}

	@Test
	def validMemberNote(): Unit = withUser("cuscao", "09876") { new ValidationFixture {
		validator.note = "   a note"

		val errors = new BindException(validator, "command")
		validator.validate(errors)
		errors.hasErrors should be (false)
	}}

	@Test
	def invalidMemberNote(): Unit = withUser("cuscao") { new ValidationFixture {
		val errors = new BindException(validator, "command")

		validator.validate(errors)
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("note")
		errors.getFieldError.getCode should be ("profiles.memberNote.empty")

		validator.note = " "
		validator.validate(errors)
		errors.hasErrors should be (true)
		errors.getErrorCount should be (2)
	}}

}
