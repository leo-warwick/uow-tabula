package uk.ac.warwick.tabula.commands.profiles.membernotes

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{AbstractMemberNote, ExtenuatingCircumstances}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services._

object EditExtenuatingCircumstancesCommand {
	def apply(circumstances: ExtenuatingCircumstances) =
		new EditExtenuatingCircumstancesCommandInternal(circumstances)
			with AutowiringFileAttachmentServiceComponent
			with AutowiringMemberNoteServiceComponent
			with ComposableCommand[AbstractMemberNote]
			with EditExtenuatingCircumstancesValidation
			with EditExtenuatingCircumstancesDescription
			with EditMemberNotePermissions
			with ModifyExtenuatingCircumstancesCommandState
			with ModifyExtenuatingCircumstancesCommandRequest
			with PopulateExtenuatingCircumstancesCommand
			with ModifyMemberNoteCommandBindListener
}


class EditExtenuatingCircumstancesCommandInternal(val circumstances: ExtenuatingCircumstances)
	extends ModifyMemberNoteCommandInternal {

	self: ModifyExtenuatingCircumstancesCommandRequest with ModifyExtenuatingCircumstancesCommandState
		with FileAttachmentServiceComponent with MemberNoteServiceComponent =>

	title = circumstances.title
	note = circumstances.note
	attachedFiles = circumstances.attachments
	startDate = circumstances.startDate
	endDate = circumstances.endDate

}

trait PopulateExtenuatingCircumstancesCommand extends PopulateOnForm {

	self: ModifyExtenuatingCircumstancesCommandRequest with ModifyExtenuatingCircumstancesCommandState =>

	override def populate(): Unit = {
		title = circumstances.title
		note = circumstances.note
		attachedFiles = circumstances.attachments
		startDate = circumstances.startDate
		endDate = circumstances.endDate
	}

}

trait EditExtenuatingCircumstancesValidation extends SelfValidating {

	self: ModifyExtenuatingCircumstancesCommandRequest with ModifyExtenuatingCircumstancesCommandState =>

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

		if (circumstances.deleted) {
			errors.rejectValue("note", "profiles.memberNote.edit.deleted")
		}
	}

}

trait EditExtenuatingCircumstancesDescription extends Describable[AbstractMemberNote] {

	self: ModifyExtenuatingCircumstancesCommandState =>

	override lazy val eventName = "EditExtenuatingCircumstances"

	override def describe(d: Description) {
		d.extenuatingCircumstances(circumstances)
	}
}