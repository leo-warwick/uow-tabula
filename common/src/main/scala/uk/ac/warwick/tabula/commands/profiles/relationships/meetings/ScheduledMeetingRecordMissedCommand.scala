package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.ScheduledMeetingRecordMissedInviteeNotification
import uk.ac.warwick.tabula.data.model.{Notification, ScheduledMeetingRecord, ScheduledNotification}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringMeetingRecordServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ScheduledMeetingRecordMissedCommand {
	def apply(meetingRecord: ScheduledMeetingRecord) =
		new ScheduledMeetingRecordMissedCommand(meetingRecord)
		with ComposableCommand[ScheduledMeetingRecord]
		with ScheduledMeetingRecordMissedDescription
		with ScheduledMeetingRecordMissedPermission
		with ScheduledMeetingRecordMissedValidation
		with ScheduledMeetingRecordMissedNotification
		with AutowiringMeetingRecordServiceComponent

}

class ScheduledMeetingRecordMissedCommand (val meetingRecord: ScheduledMeetingRecord)
	extends CommandInternal[ScheduledMeetingRecord] with ScheduledMeetingRecordMissedState {

	self: MeetingRecordServiceComponent =>

	def applyInternal(): ScheduledMeetingRecord = {
		meetingRecord.missed = true
		meetingRecord.missedReason = missedReason
		meetingRecordService.saveOrUpdate(meetingRecord)
		meetingRecord
	}
}

trait ScheduledMeetingRecordMissedValidation extends SelfValidating {
	self: ScheduledMeetingRecordMissedState =>

	def validate(errors: Errors) {
		if (meetingRecord.meetingDate.isAfterNow) {
			errors.reject("meetingRecord.missed.future")
		}
		if (meetingRecord.missed) {
			errors.reject("meetingRecord.missed.already")
		}
	}
}

trait ScheduledMeetingRecordMissedPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ScheduledMeetingRecordMissedState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.ScheduledMeetingRecord.Confirm, mandatory(meetingRecord))
	}
}

trait ScheduledMeetingRecordMissedDescription extends Describable[ScheduledMeetingRecord] {
	self: ScheduledMeetingRecordMissedState =>

	override lazy val eventName = "ScheduledMeetingRecordMissed"

	def describe(d: Description) {
		d.meeting(meetingRecord)
	}
}

trait ScheduledMeetingRecordMissedNotification
	extends Notifies[ScheduledMeetingRecord, ScheduledMeetingRecord]
	with SchedulesNotifications[ScheduledMeetingRecord, ScheduledMeetingRecord] {

	self: ScheduledMeetingRecordMissedState =>

	def emit(meeting: ScheduledMeetingRecord): Seq[ScheduledMeetingRecordMissedInviteeNotification] = {
		val user = meeting.creator.asSsoUser
		Seq(Notification.init(new ScheduledMeetingRecordMissedInviteeNotification(), user, meeting, meeting.relationship))
	}

	def transformResult(meeting: ScheduledMeetingRecord): Seq[ScheduledMeetingRecord] = Seq(meeting)

	// Notifications are cleared before being re-created, so just don't create any more
	def scheduledNotifications(meeting: ScheduledMeetingRecord): Seq[ScheduledNotification[_]] = Seq()
}

trait ScheduledMeetingRecordMissedState {
	def meetingRecord: ScheduledMeetingRecord
	var missedReason: String = _
}
