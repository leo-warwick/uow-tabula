package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence

object AttendanceNoteAttachmentCommand {
	def apply(member: Member, occurrence: SmallGroupEventOccurrence, user: CurrentUser) =
		new AttendanceNoteAttachmentCommand(member, occurrence, user)
		with ComposableCommand[Option[RenderableFile]]
		with Appliable[Option[RenderableFile]]
		with AutowiringSmallGroupServiceComponent
		with ReadOnly
		with AttendanceNoteAttachmentPermissions
		with AttendanceNoteCommandState
		with AttendanceNoteAttachmentDescription
}

class AttendanceNoteAttachmentCommand(val member: Member, val occurrence: SmallGroupEventOccurrence, val user: CurrentUser)
	extends CommandInternal[Option[RenderableFile]] {

	self: SmallGroupServiceComponent =>

	def applyInternal(): Option[RenderableAttachment] = {
		smallGroupService.getAttendanceNote(member.universityId, occurrence).flatMap{ note =>
			Option(note.attachment).map{ attachment =>
				new RenderableAttachment(attachment)
			}
		}
	}

}

trait AttendanceNoteAttachmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AttendanceNoteCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroupEvents.ViewRegister, member)
	}
}

trait AttendanceNoteAttachmentDescription extends Describable[Option[RenderableFile]] {
	self: AttendanceNoteCommandState =>

	override lazy val eventName = "DownloadAttendanceNoteAttachment"

	override def describe(d: Description) {
		d.studentIds(Seq(member.universityId))
		d.smallGroupEventOccurrence(occurrence)
	}
}