package uk.ac.warwick.tabula.commands.profiles.membernotes

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{PopulateOnForm, _}
import uk.ac.warwick.tabula.data.model.{AbstractMemberNote, ExtenuatingCircumstances, Member}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringMemberNoteServiceComponent, FileAttachmentServiceComponent, MemberNoteServiceComponent}

object CreateExtenuatingCircumstancesCommand {
	def apply(member: Member, user: CurrentUser) =
		new CreateExtenuatingCircumstancesCommandInternal(member, user)
			with AutowiringFileAttachmentServiceComponent
			with AutowiringMemberNoteServiceComponent
			with ComposableCommand[AbstractMemberNote]
			with CreateExtenuatingCircumstancesValidation
			with CreateExtenuatingCircumstancesDescription
			with CreateMemberNotePermissions
			with CreateMemberNoteCommandState
			with ModifyExtenuatingCircumstancesCommandRequest
			with ModifyMemberNoteCommandBindListener
			with PopulateOnForm {
			override def populate(): Unit = {}
		}
}


class CreateExtenuatingCircumstancesCommandInternal(val member: Member, val user: CurrentUser)
	extends ModifyMemberNoteCommandInternal {

	self: ModifyExtenuatingCircumstancesCommandRequest with CreateMemberNoteCommandState
		with FileAttachmentServiceComponent with MemberNoteServiceComponent =>

	val circumstances = new ExtenuatingCircumstances
	circumstances.creationDate = DateTime.now
	circumstances.creatorId = user.universityId
	circumstances.member = member
	override val abstractMemberNote: ExtenuatingCircumstances = circumstances

}

trait CreateExtenuatingCircumstancesValidation extends SelfValidating {

	self: ModifyExtenuatingCircumstancesCommandRequest =>

	override def validate(errors: Errors) {
		if (!note.hasText && !file.hasAttachments){
			errors.rejectValue("note", "profiles.memberNote.empty")
		}

		if (startDate == null) {
			errors.rejectValue("startDate", "NotEmpty")
		}

		if (endDate == null) {
			errors.rejectValue("endDate", "NotEmpty")
		}
	}

}

trait CreateExtenuatingCircumstancesDescription extends Describable[AbstractMemberNote] {

	self: CreateMemberNoteCommandState =>

	override lazy val eventName = "CreateExtenuatingCircumstances"

	override def describe(d: Description) {
		d.member(member)
	}
}