package uk.ac.warwick.tabula.commands.attendance.note

import org.joda.time.DateTime
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringNote, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.data.model.{AbsenceType, FileAttachment, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringUserLookupComponent, FileAttachmentServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}


object BulkAttendanceNoteCommand {
	def apply(point: AttendanceMonitoringPoint, students: Seq[StudentMember], user: CurrentUser) =
		new BulkAttendanceNoteCommand(point, students, user)
			with ComposableCommand[Seq[AttendanceMonitoringNote]]
			with BulkAttendanceNotePermissions
			with BulkAttendanceNoteDescription
			with BulkAttendanceNoteValidation
			with BulkAttendanceNoteCommandState
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringFileAttachmentServiceComponent
			with AutowiringUserLookupComponent
}

abstract class BulkAttendanceNoteCommand (
	val point: AttendanceMonitoringPoint,
	val students: Seq[StudentMember],
	val user: CurrentUser
) extends CommandInternal[Seq[AttendanceMonitoringNote]] with PopulateOnForm with BindListener
	with BulkAttendanceNoteCommandState {

	self: AttendanceMonitoringServiceComponent with FileAttachmentServiceComponent with UserLookupComponent =>

	def populate(): Unit = {
		val firstNote = students.flatMap(attendanceMonitoringService.getAttendanceNote(_, point)).headOption
		firstNote.foreach(n => {
			note = n.note
			attachedFile = n.attachment
			absenceType = n.absenceType
		})
		students.headOption.foreach(student = _)
		isNew = firstNote.isEmpty
	}

	def onBind(result: BindingResult) {
		file.onBind(result)
	}

	def applyInternal(): Seq[AttendanceMonitoringNote] = {

		val notes = students.flatMap(student => {

			val existingNote = attendanceMonitoringService.getAttendanceNote(student, point)

			if(overwrite || existingNote.isEmpty) {
				val attendanceNote = existingNote.getOrElse({
					val newNote = new AttendanceMonitoringNote
					newNote.student = student
					newNote.point = point
					newNote
				})

				attendanceNote.note = note
				if (attendanceNote.attachment != null && attachedFile == null) {
					fileAttachmentService.deleteAttachments(Seq(attendanceNote.attachment))
					attendanceNote.attachment = null
				}

				if (file.hasAttachments) {
					attendanceNote.attachment = file.attached.iterator.next.duplicate()
					attendanceNote.attachment.temporary = false
				}
				attendanceNote.absenceType = absenceType
				attendanceNote.updatedBy = user.apparentId
				attendanceNote.updatedDate = DateTime.now
				attendanceMonitoringService.saveOrUpdate(attendanceNote)
				Some(attendanceNote)
			} else {
				existingNote
			}
		})
		notes
	}
}

trait BulkAttendanceNoteValidation extends SelfValidating {
	self: BulkAttendanceNoteCommandState =>

	override def validate(errors: Errors): Unit = {
		if (absenceType == null) {
			errors.rejectValue("absenceType", "attendanceNote.absenceType.empty")
		}
	}
}

trait BulkAttendanceNoteDescription extends Describable[Seq[AttendanceMonitoringNote]] {
	self: BulkAttendanceNoteCommandState =>

	override lazy val eventName = "UpdateBulkAttendanceNote"

	override def describe(d: Description) {
		d.studentIds(students.map(_.universityId))
		d.attendanceMonitoringPoints(Seq(point))
	}

	override def describeResult(d: Description, result: Seq[AttendanceMonitoringNote]) {
		d.property("note", result.head.escapedNote)
	}
}

trait BulkAttendanceNotePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: BulkAttendanceNoteCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheckAll(Permissions.MonitoringPoints.Record, students)
	}
}

trait BulkAttendanceNoteCommandState {

	def point: AttendanceMonitoringPoint
	def students: Seq[StudentMember]
	var student: StudentMember = _ // used for attachment url
	var overwrite: Boolean = _

	var note: String = _
	var file: UploadedFile = new UploadedFile
	var attachedFile: FileAttachment = _
	var absenceType: AbsenceType = _

	var isNew: Boolean = false

}
