package uk.ac.warwick.tabula.commands.profiles.membernotes

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.{AbstractMemberNote, ExtenuatingCircumstances, Member, MemberNote}
import uk.ac.warwick.tabula.services.{AutowiringMemberNoteServiceComponent, MemberNoteServiceComponent}

object RestoreMemberNoteCommand {
	def apply(memberNote: MemberNote, member: Member) =
		new RestoreAbstractMemberNoteCommandInternal(memberNote, member)
			with AutowiringMemberNoteServiceComponent
			with ComposableCommand[AbstractMemberNote]
			with RestoreMemberNoteValidation
			with DeleteMemberNotePermissions
			with DeleteMemberNoteCommandState
			with Describable[AbstractMemberNote] {

			override lazy val eventName = "RestoreMemberNote"

			override def describe(d: Description) {
				d.memberNote(memberNote)
			}
		}
}

object RestoreExtenuatingCircumstancesCommand {
	def apply(circumstances: ExtenuatingCircumstances, member: Member) =
		new RestoreAbstractMemberNoteCommandInternal(circumstances, member)
			with AutowiringMemberNoteServiceComponent
			with ComposableCommand[AbstractMemberNote]
			with RestoreMemberNoteValidation
			with DeleteMemberNotePermissions
			with DeleteMemberNoteCommandState
			with Describable[AbstractMemberNote] {

			override lazy val eventName = "RestoreExtenuatingCircumstances"

			override def describe(d: Description) {
				d.extenuatingCircumstances(circumstances)
			}
		}
}


class RestoreAbstractMemberNoteCommandInternal(val abstractMemberNote: AbstractMemberNote, val member: Member)
	extends CommandInternal[AbstractMemberNote] {

	self: MemberNoteServiceComponent =>

	override def applyInternal(): AbstractMemberNote = {
		abstractMemberNote.deleted = false
		HibernateHelpers.initialiseAndUnproxy(abstractMemberNote) match {
			case memberNote: MemberNote => memberNoteService.saveOrUpdate(memberNote)
			case circumstances: ExtenuatingCircumstances => memberNoteService.saveOrUpdate(circumstances)
		}
		abstractMemberNote
	}

}

trait RestoreMemberNoteValidation extends SelfValidating {

	self: DeleteMemberNoteCommandState =>

	override def validate(errors: Errors) {
		if (!abstractMemberNote.deleted) {
			errors.reject("profiles.memberNote.restore.notDeleted")
		}
	}

}
