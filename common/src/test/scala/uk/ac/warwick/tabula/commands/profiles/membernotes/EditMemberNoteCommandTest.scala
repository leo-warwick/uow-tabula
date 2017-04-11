package uk.ac.warwick.tabula.commands.profiles.membernotes

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{AbstractMemberNote, MemberNote, StudentMember}
import uk.ac.warwick.tabula.services.{MemberNoteServiceComponent, _}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class EditMemberNoteCommandTest extends TestBase with Mockito {

	val member: StudentMember = Fixtures.student("12345")
	val mockProfileService: ProfileService = smartMock[ProfileService]
	val mockMemberNoteService: MemberNoteService = smartMock[MemberNoteService]
	val mockFileAttachmentService: FileAttachmentService = smartMock[FileAttachmentService]

	trait ApplyFixture {
		val cmd = new EditMemberNoteCommandInternal(Fixtures.memberNoteWithId("some notes", member, "123"))
			with ModifyMemberNoteCommandState with ModifyMemberNoteCommandRequest
			with FileAttachmentServiceComponent with MemberNoteServiceComponent {

			override val memberNoteService: MemberNoteService = mockMemberNoteService
			override val fileAttachmentService: FileAttachmentService = mockFileAttachmentService
		}
	}

	@Test
	def testApply(): Unit = withUser("cuscao", "09876") { new ApplyFixture {
		cmd.note = "the note"

		val memberNote: AbstractMemberNote = cmd.applyInternal()
		memberNote.member should be (member)
		memberNote.note should be (cmd.note)
	}}

	trait ValidationFixture {
		val validator = new EditMemberNoteValidation
			with ModifyMemberNoteCommandRequest with ModifyMemberNoteCommandState {
			override val memberNote: MemberNote = new MemberNote
			memberNote.member = member
			memberNote.note = "Existing note"
			memberNote.creatorId = "cuscao"
		}
		validator.title = validator.memberNote.title
		validator.note = validator.memberNote.note
	}

	@Test
	def validExistingNote(): Unit = { new ValidationFixture {
		val errors = new BindException(validator, "command")
		validator.validate(errors)
		errors.hasFieldErrors should be (false)

	}}

	@Test
	def invalidEditedNote(): Unit = { new ValidationFixture {
		val errors = new BindException(validator, "command")
		validator.validate(errors)
		errors.hasFieldErrors should be (false)

		validator.note = " "
		validator.validate(errors)
		errors.hasFieldErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("note")
		errors.getFieldError.getCode should be ("profiles.memberNote.empty")

	}}

	@Test
	def editDeletedNote(): Unit = { new ValidationFixture {
		validator.memberNote.deleted = true

		val errors = new BindException(validator, "command")

		validator.validate(errors)
		errors.hasFieldErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("note")
		errors.getFieldError.getCode should be ("profiles.memberNote.edit.deleted")
	}}

}
