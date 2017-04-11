package uk.ac.warwick.tabula.commands.attendance.note

import uk.ac.warwick.tabula.data.model.{AbsenceType, FileAttachment, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceState, AttendanceMonitoringCheckpoint, AttendanceMonitoringNote}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.CurrentUser
import org.joda.time.DateTime
import org.springframework.validation.{Errors, BindingResult}
import uk.ac.warwick.tabula.system.BindListener

object EditAttendanceNoteCommand {
	 def apply(student: StudentMember, point: AttendanceMonitoringPoint, user: CurrentUser, customStateStringOption: Option[String]) =
		 new EditAttendanceNoteCommand(student, point, user, customStateStringOption)
		 with ComposableCommand[AttendanceMonitoringNote]
		 with AttendanceNotePermissions
		 with AttendanceNoteDescription
		 with AttendanceNoteValidation
		 with AttendanceNoteCommandState
		 with AutowiringAttendanceMonitoringServiceComponent
		 with AutowiringFileAttachmentServiceComponent
		 with AutowiringUserLookupComponent
 }

abstract class EditAttendanceNoteCommand(
	 val student: StudentMember,
	 val point: AttendanceMonitoringPoint,
	 val user: CurrentUser,
	 val customStateStringOption: Option[String]
 ) extends CommandInternal[AttendanceMonitoringNote] with PopulateOnForm with BindListener
		with AttendanceNoteCommandState {

		self: AttendanceMonitoringServiceComponent with FileAttachmentServiceComponent with UserLookupComponent =>

		def populate(): Unit = {
			note = attendanceNote.note
			attachedFile = attendanceNote.attachment
			absenceType = attendanceNote.absenceType
		}

		def onBind(result: BindingResult) {
			file.onBind(result)
			attendanceNote = attendanceMonitoringService.getAttendanceNote(student, point).getOrElse({
				isNew = true
				val newNote = new AttendanceMonitoringNote
				newNote.student = student
				newNote.point = point
				newNote
			})

			val pointsCheckpointsMap = attendanceMonitoringService.getCheckpoints(Seq(point), student)
			if (pointsCheckpointsMap.nonEmpty) checkpoint = pointsCheckpointsMap.head._2

			customStateStringOption.foreach(stateString => {
				try {
					customState = AttendanceState.fromCode(stateString)
				} catch {
					case _: IllegalArgumentException =>
				}
			})

		}

		def applyInternal(): AttendanceMonitoringNote = {
			attendanceNote.note = note

			if (attendanceNote.attachment != null && attachedFile == null) {
				fileAttachmentService.deleteAttachments(Seq(attendanceNote.attachment))
				attendanceNote.attachment = null
			}

			if (file.hasAttachments) {
				attendanceNote.attachment = file.attached.iterator.next
				attendanceNote.attachment.temporary = false
			}

			attendanceNote.absenceType = absenceType
			attendanceNote.updatedBy = user.apparentId
			attendanceNote.updatedDate = DateTime.now
			attendanceMonitoringService.saveOrUpdate(attendanceNote)
			attendanceNote
		}
	}

trait AttendanceNoteValidation extends SelfValidating {
	 self: AttendanceNoteCommandState =>

	 override def validate(errors: Errors): Unit = {
		 if (absenceType == null) {
			 errors.rejectValue("absenceType", "attendanceNote.absenceType.empty")
		 }
	 }
 }

trait AttendanceNoteDescription extends Describable[AttendanceMonitoringNote] {
	self: AttendanceNoteCommandState =>

	override lazy val eventName = "UpdateAttendanceNote"

	override def describe(d: Description) {
		d.studentIds(Seq(student.universityId))
		d.attendanceMonitoringPoints(Seq(point))
	}

	override def describeResult(d: Description, result: AttendanceMonitoringNote) {
		d.property("note", result.escapedNote)
	}
}

trait AttendanceNotePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AttendanceNoteCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Record, student)
	}
}

trait AttendanceNoteCommandState {
	def student: StudentMember
	def point: AttendanceMonitoringPoint

	var attendanceNote: AttendanceMonitoringNote = _
	var note: String = _
	var file: UploadedFile = new UploadedFile
	var attachedFile: FileAttachment = _
	var absenceType: AbsenceType = _

	var isNew: Boolean = false
	var checkpoint: AttendanceMonitoringCheckpoint = _
	var customState: AttendanceState = _
}
