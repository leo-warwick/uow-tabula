package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringMeetingRecordServiceComponent, FileAttachmentServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConversions._

object ConvertScheduledMeetingRecordCommand {
	def apply(creator: Member, meetingRecord: ScheduledMeetingRecord) =
		new ConvertScheduledMeetingRecordCommand(creator, meetingRecord)
			with ComposableCommand[MeetingRecord]
			with ConvertScheduledMeetingRecordPermissions
			with ConvertScheduledMeetingRecordState
			with MeetingRecordCommandRequest
			with ConvertScheduledMeetingRecordDescription
			with AutowiringMeetingRecordServiceComponent
			with ConvertScheduledMeetingRecordCommandValidation
			with AutowiringFileAttachmentServiceComponent
			with PopulateMeetingRecordCommand
}

class ConvertScheduledMeetingRecordCommand (override val creator: Member, val meetingRecord: ScheduledMeetingRecord)
	extends CommandInternal[MeetingRecord] with ConvertScheduledMeetingRecordState {

	self: MeetingRecordServiceComponent with FileAttachmentServiceComponent =>

	def applyInternal(): MeetingRecord = {
		val newMeeting = createCommand.apply()
		newMeeting.attachments.foreach(_.meetingRecord = newMeeting)

		meetingRecord.removeAllAttachments()
		meetingRecordService.purge(meetingRecord)
		newMeeting
	}

}

trait ConvertScheduledMeetingRecordCommandValidation extends SelfValidating {
	self: ConvertScheduledMeetingRecordState  =>
	override def validate(errors: Errors) {
		if (meetingRecord.missed)
			errors.reject("meetingRecord.confirm.missed")
	}
}

trait ConvertScheduledMeetingRecordPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ConvertScheduledMeetingRecordState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(
			Permissions.Profiles.ScheduledMeetingRecord.Confirm,
			mandatory(meetingRecord)
		)
	}
}

trait ConvertScheduledMeetingRecordDescription extends Describable[MeetingRecord] {
	self: ConvertScheduledMeetingRecordState =>

	override lazy val eventName = "ConvertScheduledMeetingRecord"

	override def describe(d: Description) {
		meetingRecord.relationship.studentMember.map { d.member }
		d.properties(
			"creator" -> creator.universityId,
			"relationship" -> meetingRecord.relationship.relationshipType.toString()
		)
	}

	override def describeResult(d: Description, result: MeetingRecord) {
		d.meeting(result)
	}
}

trait ConvertScheduledMeetingRecordState extends EditMeetingRecordCommandState {
	def creator: Member
	def meetingRecord: ScheduledMeetingRecord
	override lazy val relationship: StudentRelationship = meetingRecord.relationship
	var createCommand: Appliable[MeetingRecord] = _
}