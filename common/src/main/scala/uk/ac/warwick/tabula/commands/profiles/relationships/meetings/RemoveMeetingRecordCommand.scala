package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.{AddsIcalAttachmentToScheduledMeetingNotification, ScheduledMeetingRecordBehalfNotification, ScheduledMeetingRecordInviteeNotification, ScheduledMeetingRecordNotification}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringMeetingRecordServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

trait RemoveMeetingRecordPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: RemoveMeetingRecordState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mandatory(meetingRecord) match {
			case (m: MeetingRecord) =>
				p.PermissionCheck(Permissions.Profiles.MeetingRecord.Manage(meetingRecord.relationship.relationshipType), meetingRecord)
			case (m: ScheduledMeetingRecord) =>
				p.PermissionCheck(Permissions.Profiles.ScheduledMeetingRecord.Manage(meetingRecord.relationship.relationshipType), meetingRecord)
		}
	}
}

trait RemoveMeetingRecordDescription extends Describable[AbstractMeetingRecord] {
	self: RemoveMeetingRecordState =>

	override def describe(d: Description): Unit = d.properties(
		"meetingRecord" -> meetingRecord.id)

}

trait RemoveMeetingRecordValidation {
	self: RemoveMeetingRecordState =>

	def sharedValidation(errors: Errors) {
		if (!meetingRecord.isScheduled && meetingRecord.asInstanceOf[MeetingRecord].isApproved) {
			errors.reject("meetingRecord.delete.approved")
		}
		else if (!meetingRecord.isScheduled && user.universityId != meetingRecord.creator.universityId) {
			errors.reject("meetingRecord.delete.notOwner")
		}
	}

}

trait RemoveMeetingRecordState {
	def meetingRecord: AbstractMeetingRecord
	def user: CurrentUser
}

class DeleteMeetingRecordCommand(val meetingRecord: AbstractMeetingRecord, val user: CurrentUser)
	extends CommandInternal[AbstractMeetingRecord] {

	self: MeetingRecordServiceComponent =>

	override def applyInternal(): AbstractMeetingRecord = {
		meetingRecord.deleted = true
		meetingRecordService.saveOrUpdate(meetingRecord)
		meetingRecord
	}
}

trait DeleteMeetingRecordCommandValidation extends SelfValidating with RemoveMeetingRecordValidation {
	self: RemoveMeetingRecordState =>

	override def validate(errors: Errors) {
		if (meetingRecord.deleted) errors.reject("meetingRecord.delete.alreadyDeleted")

		sharedValidation(errors)
	}
}

trait DeleteScheduledMeetingRecordNotification extends Notifies[AbstractMeetingRecord, ScheduledMeetingRecord] {

	self: RemoveMeetingRecordState =>

	def emit(meeting: AbstractMeetingRecord): Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = {
		meeting match {
			case m: ScheduledMeetingRecord =>
				val inviteeNotification = Notification.init(new ScheduledMeetingRecordInviteeNotification("deleted"), user.apparentUser, m, m.relationship)
				if(!m.universityIdInRelationship(user.universityId)) {
					val behalfNotification = Notification.init(new ScheduledMeetingRecordBehalfNotification("deleted"), user.apparentUser, m, m.relationship)
					Seq(inviteeNotification, behalfNotification)
				} else {
					Seq(inviteeNotification)
				}
			case _ => Nil
		}
	}
}

class RestoreMeetingRecordCommand (val meetingRecord: AbstractMeetingRecord, val user: CurrentUser)
	extends CommandInternal[AbstractMeetingRecord] {

	self: MeetingRecordServiceComponent =>

	override def applyInternal(): AbstractMeetingRecord = {
		meetingRecord.deleted = false
		meetingRecordService.saveOrUpdate(meetingRecord)
		meetingRecord
	}
}

trait RestoreMeetingRecordCommandValidation extends SelfValidating with RemoveMeetingRecordValidation {
	self: RemoveMeetingRecordState =>

	override def validate(errors: Errors) {
		if (!meetingRecord.deleted) errors.reject("meetingRecord.delete.notDeleted")

		sharedValidation(errors)
	}
}

trait RestoreScheduledMeetingRecordNotification extends Notifies[AbstractMeetingRecord, ScheduledMeetingRecord] {

	self: RemoveMeetingRecordState =>

	def emit(meeting: AbstractMeetingRecord): Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = {
		meeting match {
			case m: ScheduledMeetingRecord =>
				val inviteeNotification = Notification.init(new ScheduledMeetingRecordInviteeNotification("rescheduled"), user.apparentUser, m, m.relationship)
				if(!m.universityIdInRelationship(user.universityId)) {
					val behalfNotification = Notification.init(new ScheduledMeetingRecordBehalfNotification("rescheduled"), user.apparentUser, m, m.relationship)
					Seq(inviteeNotification, behalfNotification)
				} else {
					Seq(inviteeNotification)
				}
			case _ => Seq()
		}
	}
}

class PurgeMeetingRecordCommand (val meetingRecord: AbstractMeetingRecord, val user: CurrentUser)
	extends CommandInternal[AbstractMeetingRecord] {

	self: MeetingRecordServiceComponent =>

	override def applyInternal(): AbstractMeetingRecord = {
		meetingRecordService.purge(meetingRecord)
		meetingRecord
	}
}

object DeleteMeetingRecordCommand {
	def apply(meetingRecord: AbstractMeetingRecord, user: CurrentUser) =
		new DeleteMeetingRecordCommand(meetingRecord, user)
		with ComposableCommand[AbstractMeetingRecord]
		with AutowiringMeetingRecordServiceComponent
		with DeleteMeetingRecordCommandValidation
		with RemoveMeetingRecordPermissions
		with RemoveMeetingRecordDescription
		with RemoveMeetingRecordState
		with DeleteScheduledMeetingRecordNotification
}

object RestoreMeetingRecordCommand {
	def apply(meetingRecord: AbstractMeetingRecord, user: CurrentUser) =
		new RestoreMeetingRecordCommand(meetingRecord, user)
			with ComposableCommand[AbstractMeetingRecord]
			with AutowiringMeetingRecordServiceComponent
			with RestoreMeetingRecordCommandValidation
			with RemoveMeetingRecordPermissions
			with RemoveMeetingRecordDescription
			with RemoveMeetingRecordState
			with RestoreScheduledMeetingRecordNotification
}

object PurgeMeetingRecordCommand {
	def apply(meetingRecord: AbstractMeetingRecord, user: CurrentUser) =
		new PurgeMeetingRecordCommand(meetingRecord, user)
			with ComposableCommand[AbstractMeetingRecord]
			with AutowiringMeetingRecordServiceComponent
			with RestoreMeetingRecordCommandValidation
			with RemoveMeetingRecordPermissions
			with RemoveMeetingRecordDescription
			with RemoveMeetingRecordState
}
