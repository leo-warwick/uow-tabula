package uk.ac.warwick.tabula.commands.attendance.note

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object AttendanceNoteAttachmentCommand {
	def apply(student: StudentMember, point: AttendanceMonitoringPoint, user: CurrentUser) =
		new AttendanceNoteAttachmentCommand(student, point, user)
		with ComposableCommand[Option[RenderableFile]]
		with AutowiringAttendanceMonitoringServiceComponent
		with ReadOnly
		with AttendanceNoteAttachmentPermissions
		with AttendanceNoteCommandState
		with AttendanceNoteAttachmentDescription
}

class AttendanceNoteAttachmentCommand(val student: StudentMember, val point: AttendanceMonitoringPoint, val user: CurrentUser)
	extends CommandInternal[Option[RenderableFile]] {

	self: AttendanceMonitoringServiceComponent =>

	def applyInternal(): Option[RenderableAttachment] = {
		attendanceMonitoringService.getAttendanceNote(student, point).flatMap{ note =>
			Option(note.attachment).map{ attachment =>
				new RenderableAttachment(attachment)
			}
		}
	}

}

trait AttendanceNoteAttachmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AttendanceNoteCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, student)
	}
}

trait AttendanceNoteAttachmentDescription extends Describable[Option[RenderableFile]] {
	self: AttendanceNoteCommandState =>

	override lazy val eventName = "DownloadAttendanceNoteAttachment"

	override def describe(d: Description) {
		d.studentIds(Seq(student.universityId))
		d.attendanceMonitoringPoints(Seq(point))
	}
}