package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.DateTime
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.DateFormats._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.{AddsIcalAttachmentToScheduledMeetingNotification, ScheduledMeetingRecordBehalfNotification, ScheduledMeetingRecordInviteeNotification, ScheduledMeetingRecordNotification}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringMeetingRecordServiceComponent, FileAttachmentServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

case class ScheduledMeetingRecordResult(meetingRecord: ScheduledMeetingRecord, isRescheduled: Boolean)

object EditScheduledMeetingRecordCommand {
	def apply(editor: Member, meetingRecord: ScheduledMeetingRecord) =
		new EditScheduledMeetingRecordCommand(editor, meetingRecord)
			with ComposableCommand[ScheduledMeetingRecordResult]
			with EditScheduledMeetingRecordPermissions
			with EditScheduledMeetingRecordState
			with EditScheduledMeetingRecordDescription
			with AutowiringMeetingRecordServiceComponent
			with EditScheduledMeetingRecordCommandValidation
			with EditScheduledMeetingRecordNotification
			with AutowiringFileAttachmentServiceComponent
			with EditScheduledMeetingRecordNotifications
			with PopulateScheduledMeetingRecordCommand
}

class EditScheduledMeetingRecordCommand (val editor: Member, val meetingRecord: ScheduledMeetingRecord)
	extends CommandInternal[ScheduledMeetingRecordResult] with EditScheduledMeetingRecordState with BindListener {

	self: MeetingRecordServiceComponent with FileAttachmentServiceComponent =>

	def applyInternal(): ScheduledMeetingRecordResult = {

		def persistAttachments(meeting: ScheduledMeetingRecord) {
			// delete attachments that have been removed

			if (meeting.attachments != null) {
				val filesToKeep = Option(attachedFiles).map(_.asScala.toList).getOrElse(List())
				val filesToRemove = meeting.attachments.asScala -- filesToKeep
				meeting.attachments = JArrayList[FileAttachment](filesToKeep)
				fileAttachmentService.deleteAttachments(filesToRemove)
			}

			file.attached.asScala.foreach { attachment =>
				attachment.meetingRecord = meeting
				meeting.attachments.add(attachment)
				attachment.temporary = false
			}
		}

		meetingRecord.title = title
		meetingRecord.description = description

		val meetingDate = DateTimePickerFormatter.parseDateTime(meetingRecord.meetingDate.toString(DateTimePickerFormatter))
		val newMeetingDate = DateTimePickerFormatter.parseDateTime(meetingDateStr+" "+meetingTimeStr)

		val isRescheduled = !meetingDate.equals(newMeetingDate)

		if ((!meetingDateStr.isEmptyOrWhitespace) && (!meetingTimeStr.isEmptyOrWhitespace) && (!meetingEndTimeStr.isEmptyOrWhitespace)) {
			meetingRecord.meetingDate = DateTimePickerFormatter.parseDateTime(meetingDateStr + " " + meetingTimeStr).withHourOfDay(DateTimePickerFormatter.parseDateTime(meetingDateStr + " " + meetingTimeStr).getHourOfDay)
			meetingRecord.meetingEndDate = DateTimePickerFormatter.parseDateTime(meetingDateStr + " " + meetingEndTimeStr).withHourOfDay(DateTimePickerFormatter.parseDateTime(meetingDateStr + " " + meetingEndTimeStr).getHourOfDay)
		}
		meetingRecord.lastUpdatedDate = DateTime.now
		meetingRecord.format = format

		meetingRecord.meetingLocation =
			if (meetingLocation.hasText) {
				if (meetingLocationId.hasText) {
					MapLocation(meetingLocation, meetingLocationId)
				} else {
					NamedLocation(meetingLocation)
				}
			} else {
				null
			}

		persistAttachments(meetingRecord)
		meetingRecordService.saveOrUpdate(meetingRecord)
		ScheduledMeetingRecordResult(meetingRecord, isRescheduled)
	}

	def onBind(result: BindingResult) {
		file.onBind(result)
	}

}

trait PopulateScheduledMeetingRecordCommand extends PopulateOnForm {

	self: EditScheduledMeetingRecordState =>

	override def populate(): Unit = {
		title = meetingRecord.title
		description = meetingRecord.description

		meetingDateStr = meetingRecord.meetingDate.toString(DatePickerFormatter)
		meetingTimeStr = meetingRecord.meetingDate.withHourOfDay(meetingRecord.meetingDate.getHourOfDay).toString(TimePickerFormatter)
		meetingEndTimeStr = meetingRecord.meetingEndDate.withHourOfDay(meetingRecord.meetingEndDate.getHourOfDay).toString(TimePickerFormatter)

		Option(meetingRecord.meetingLocation).foreach {
			case NamedLocation(name) => meetingLocation = name
			case MapLocation(name, lid) =>
				meetingLocation = name
				meetingLocationId = lid
		}

		format = meetingRecord.format
		attachedFiles = meetingRecord.attachments
	}

}

trait EditScheduledMeetingRecordCommandValidation extends SelfValidating with ScheduledMeetingRecordValidation {
	self: EditScheduledMeetingRecordState with MeetingRecordServiceComponent =>

	override def validate(errors: Errors) {

		sharedValidation(errors, title, meetingDateStr, meetingTimeStr, meetingEndTimeStr)

		meetingRecordService.listScheduled(Set(meetingRecord.relationship), Some(editor)).foreach(
			m => if ((!meetingDateStr.isEmptyOrWhitespace) && (!meetingTimeStr.isEmptyOrWhitespace) && (!meetingEndTimeStr.isEmptyOrWhitespace)) {
				if (m.meetingDate.toString(DateTimePickerFormatter).equals(meetingDateStr+" "+meetingTimeStr) && (m.id != meetingRecord.id) ) errors.rejectValue("meetingDateStr", "meetingRecord.date.duplicate")
			}
		)
	}
}

trait EditScheduledMeetingRecordState {
	def editor: Member
	def meetingRecord: ScheduledMeetingRecord

	var title: String = _
	var description: String = _

	var meetingDateStr: String = _
	var meetingTimeStr: String = _
	var meetingEndTimeStr: String = _

	var format: MeetingFormat = _

	var meetingLocation: String = _
	var meetingLocationId: String = _

	var file: UploadedFile = new UploadedFile
	var attachedFiles:JList[FileAttachment] = _

	var attachmentTypes: Seq[String] = Seq[String]()

	lazy val relationship: StudentRelationship = meetingRecord.relationship
}

trait EditScheduledMeetingRecordPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditScheduledMeetingRecordState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mandatory(meetingRecord) // Otherwise we'll get an NPE evaluating relationship.relationshipType
		p.PermissionCheck(Permissions.Profiles.ScheduledMeetingRecord.Manage(relationship.relationshipType), meetingRecord)
	}
}

trait EditScheduledMeetingRecordDescription extends Describable[ScheduledMeetingRecordResult] {
	self: EditScheduledMeetingRecordState =>

	override lazy val eventName = "EditScheduledMeetingRecord"

	override def describe(d: Description) {
		meetingRecord.relationship.studentMember.map { d.member }
		d.properties(
			"creator" -> editor.universityId,
			"relationship" -> meetingRecord.relationship.relationshipType.toString()
		)
	}
}

trait EditScheduledMeetingRecordNotification extends Notifies[ScheduledMeetingRecordResult, ScheduledMeetingRecord] {
	self: EditScheduledMeetingRecordState =>

	def emit(result: ScheduledMeetingRecordResult): Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = {
		val meeting = result.meetingRecord
		val user = editor.asSsoUser
		val verb =
			if (result.isRescheduled) "rescheduled"
			else "updated"

		val inviteeNotification = Notification.init(new ScheduledMeetingRecordInviteeNotification(verb), user, meeting, meeting.relationship)
		if(!meeting.universityIdInRelationship(user.getWarwickId)) {
			val behalfNotification = Notification.init(new ScheduledMeetingRecordBehalfNotification(verb), user, meeting, meeting.relationship)
			Seq(inviteeNotification, behalfNotification)
		} else {
			Seq(inviteeNotification)
		}
	}
}

trait EditScheduledMeetingRecordNotifications extends SchedulesNotifications[ScheduledMeetingRecordResult, ScheduledMeetingRecord] {

	override def transformResult(result: ScheduledMeetingRecordResult) = Seq(result.meetingRecord)

	override def scheduledNotifications(meetingRecord: ScheduledMeetingRecord): Seq[ScheduledNotification[ScheduledMeetingRecord]] = {
		Seq(
			new ScheduledNotification[ScheduledMeetingRecord]("ScheduledMeetingRecordReminderStudent", meetingRecord, meetingRecord.meetingDate.withTimeAtStartOfDay),
			new ScheduledNotification[ScheduledMeetingRecord]("ScheduledMeetingRecordReminderAgent", meetingRecord, meetingRecord.meetingDate.withTimeAtStartOfDay),
			new ScheduledNotification[ScheduledMeetingRecord]("ScheduledMeetingRecordConfirm", meetingRecord, meetingRecord.meetingDate),
			new ScheduledNotification[ScheduledMeetingRecord]("ScheduledMeetingRecordConfirm", meetingRecord, meetingRecord.meetingDate.plusDays(5))
		)
	}

}
