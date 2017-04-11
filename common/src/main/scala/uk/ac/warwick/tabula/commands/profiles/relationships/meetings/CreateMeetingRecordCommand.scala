package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.NewMeetingRecordApprovalNotification
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringMeetingRecordServiceComponent, AutowiringAttendanceMonitoringMeetingRecordServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringMeetingRecordServiceComponent, FileAttachmentServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}

object CreateMeetingRecordCommand {
	def apply(creator: Member, relationship: StudentRelationship) =
		new CreateMeetingRecordCommandInternal(creator, relationship)
			with AutowiringMeetingRecordServiceComponent
			with AutowiringFeaturesComponent
			with AutowiringAttendanceMonitoringMeetingRecordServiceComponent
			with AutowiringFileAttachmentServiceComponent
			with ComposableCommand[MeetingRecord]
			with MeetingRecordCommandBindListener
			with ModifyMeetingRecordValidation
			with CreateMeetingRecordDescription
			with ModifyMeetingRecordPermissions
			with CreateMeetingRecordCommandState
			with MeetingRecordCommandRequest
			with CreateMeetingRecordCommandNotifications
			with PopulateOnForm {
			override def populate(): Unit = {}
		}
}


class CreateMeetingRecordCommandInternal(val creator: Member, var relationship: StudentRelationship)
	extends AbstractModifyMeetingRecordCommand {

	self: CreateMeetingRecordCommandState with MeetingRecordCommandRequest with MeetingRecordServiceComponent
		with FeaturesComponent with AttendanceMonitoringMeetingRecordServiceComponent
		with FileAttachmentServiceComponent =>

	override def applyInternal(): MeetingRecord = {
		val meeting = new MeetingRecord(creator, relationship)
		applyCommon(meeting)
	}

}

trait CreateMeetingRecordDescription extends ModifyMeetingRecordDescription {

	self: ModifyMeetingRecordCommandState =>

	override lazy val eventName = "CreateMeetingRecord"

}

trait CreateMeetingRecordCommandState extends ModifyMeetingRecordCommandState {
	override def creator: Member
	override def relationship: StudentRelationship
}

trait CreateMeetingRecordCommandNotifications extends Notifies[MeetingRecord, MeetingRecord]
	with SchedulesNotifications[MeetingRecord, MeetingRecord] {

	self: CreateMeetingRecordCommandState =>

	override def emit(meeting: MeetingRecord) = Seq(
		Notification.init(new NewMeetingRecordApprovalNotification, creator.asSsoUser, Seq(meeting), relationship)
	)

	override def transformResult(meetingRecord: MeetingRecord) = Seq(meetingRecord)

	override def scheduledNotifications(result: MeetingRecord) = Seq(
		new ScheduledNotification[MeetingRecord]("newMeetingRecordApproval", result, DateTime.now.plusWeeks(1))
	)

}