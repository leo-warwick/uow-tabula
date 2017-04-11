package uk.ac.warwick.tabula.commands.profiles.membernotes

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{AbstractMemberNote, Member, MemberNote}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringMemberNoteServiceComponent, FileAttachmentServiceComponent, MemberNoteServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object CreateMemberNoteCommand {
	def apply(member: Member, user: CurrentUser) =
		new CreateMemberNoteCommandInternal(member, user)
			with AutowiringFileAttachmentServiceComponent
			with AutowiringMemberNoteServiceComponent
			with ComposableCommand[AbstractMemberNote]
			with CreateMemberNoteValidation
			with CreateMemberNoteDescription
			with CreateMemberNotePermissions
			with CreateMemberNoteCommandState
			with ModifyMemberNoteCommandRequest
			with ModifyMemberNoteCommandBindListener
			with PopulateOnForm {
			override def populate(): Unit = {}
		}
}


class CreateMemberNoteCommandInternal(val member: Member, val user: CurrentUser)
	extends ModifyMemberNoteCommandInternal {

	self: ModifyMemberNoteCommandRequest with CreateMemberNoteCommandState
		with FileAttachmentServiceComponent with MemberNoteServiceComponent =>

	val memberNote = new MemberNote
	memberNote.creationDate = DateTime.now
	memberNote.creatorId = user.universityId
	memberNote.member = member
	override val abstractMemberNote: MemberNote = memberNote

}

trait CreateMemberNoteValidation extends SelfValidating {

	self: ModifyMemberNoteCommandRequest =>

	override def validate(errors: Errors) {
		if (!note.hasText && !file.hasAttachments){
			errors.rejectValue("note", "profiles.memberNote.empty")
		}
	}

}

trait CreateMemberNotePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: CreateMemberNoteCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MemberNotes.Create, member)
	}

}

trait CreateMemberNoteDescription extends Describable[AbstractMemberNote] {

	self: CreateMemberNoteCommandState =>

	override lazy val eventName = "CreateMemberNote"

	override def describe(d: Description) {
		d.member(member)
	}
}

trait CreateMemberNoteCommandState extends ModifyAbstractMemberNoteCommandState {
	def member: Member
}