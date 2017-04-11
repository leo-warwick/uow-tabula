package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.permissions.Permissions

/*
 * Allow creators of administrative notes to update/remove them
 */
case class MemberNoteCreator(note: model.AbstractMemberNote) extends BuiltInRole(MemberNoteCreatorRoleDefinition, note)

case object MemberNoteCreatorRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "Member Note Creator"

	GrantsScopedPermission(
		Permissions.MemberNotes.Update,
		Permissions.MemberNotes.Delete
	)

}