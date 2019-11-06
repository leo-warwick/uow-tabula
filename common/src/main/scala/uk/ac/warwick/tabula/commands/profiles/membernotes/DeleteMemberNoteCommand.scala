package uk.ac.warwick.tabula.commands.profiles.membernotes

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.{AbstractMemberNote, Member, MemberNote}
import uk.ac.warwick.tabula.services.{AutowiringMemberNoteServiceComponent, MemberNoteServiceComponent}

object DeleteMemberNoteCommand {
  def apply(memberNote: MemberNote, member: Member) =
    new DeleteAbstractMemberNoteCommandInternal(memberNote, member)
      with AutowiringMemberNoteServiceComponent
      with ComposableCommand[AbstractMemberNote]
      with DeleteMemberNoteValidation
      with DeleteMemberNotePermissions
      with DeleteMemberNoteCommandState
      with Describable[AbstractMemberNote] {

      override lazy val eventName = "DeleteMemberNote"

      override def describe(d: Description): Unit = {
        d.memberNote(memberNote)
      }
    }
}


class DeleteAbstractMemberNoteCommandInternal(val abstractMemberNote: AbstractMemberNote, val member: Member)
  extends CommandInternal[AbstractMemberNote] {

  self: MemberNoteServiceComponent =>

  override def applyInternal(): AbstractMemberNote = {
    abstractMemberNote.deleted = true
    HibernateHelpers.initialiseAndUnproxy(abstractMemberNote) match {
      case memberNote: MemberNote => memberNoteService.saveOrUpdate(memberNote)
    }
    abstractMemberNote
  }

}

trait DeleteMemberNoteValidation extends SelfValidating {

  self: DeleteMemberNoteCommandState =>

  override def validate(errors: Errors): Unit = {
    if (abstractMemberNote.deleted) {
      errors.reject("profiles.memberNote.delete.notDeleted")
    }
  }

}

trait DeleteMemberNotePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: DeleteMemberNoteCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    mustBeLinked(abstractMemberNote, member)
    p.PermissionCheck(Permissions.MemberNotes.Delete, abstractMemberNote)
  }

}

trait DeleteMemberNoteCommandState {
  def abstractMemberNote: AbstractMemberNote

  def member: Member
}
