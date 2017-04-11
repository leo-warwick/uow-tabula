package uk.ac.warwick.tabula.commands.profiles.membernotes

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{AbstractMemberNote, MemberNote}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringMemberNoteServiceComponent, FileAttachmentServiceComponent, MemberNoteServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object EditMemberNoteCommand {
	def apply(memberNote: MemberNote) =
		new EditMemberNoteCommandInternal(memberNote)
			with AutowiringFileAttachmentServiceComponent
			with AutowiringMemberNoteServiceComponent
			with ComposableCommand[AbstractMemberNote]
			with EditMemberNoteValidation
			with EditMemberNoteDescription
			with EditMemberNotePermissions
			with ModifyMemberNoteCommandState
			with ModifyMemberNoteCommandRequest
			with PopulateMemberNoteCommand
			with ModifyMemberNoteCommandBindListener
}


class EditMemberNoteCommandInternal(val memberNote: MemberNote)
	extends ModifyMemberNoteCommandInternal {

	self: ModifyMemberNoteCommandRequest with ModifyMemberNoteCommandState
		with FileAttachmentServiceComponent with MemberNoteServiceComponent =>

}

trait PopulateMemberNoteCommand extends PopulateOnForm {

	self: ModifyMemberNoteCommandRequest with ModifyMemberNoteCommandState =>

	override def populate(): Unit = {
		title = memberNote.title
		note = memberNote.note
		attachedFiles = memberNote.attachments
	}

}

trait EditMemberNoteValidation extends SelfValidating {

	self: ModifyMemberNoteCommandRequest with ModifyMemberNoteCommandState =>

	override def validate(errors: Errors) {
		if (!note.hasText && !file.hasAttachments){
			errors.rejectValue("note", "profiles.memberNote.empty")
		}

		if (memberNote.deleted) {
			errors.rejectValue("note", "profiles.memberNote.edit.deleted")
		}
	}

}

trait EditMemberNotePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ModifyAbstractMemberNoteCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MemberNotes.Update, abstractMemberNote)
	}

}

trait EditMemberNoteDescription extends Describable[AbstractMemberNote] {

	self: ModifyMemberNoteCommandState =>

	override lazy val eventName = "EditMemberNote"

	override def describe(d: Description) {
		d.memberNote(memberNote)
	}
}