package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.NewMeetingRecordApprovalNotification
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringMeetingRecordServiceComponent, AutowiringAttendanceMonitoringMeetingRecordServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringMeetingRecordServiceComponent, FileAttachmentServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}

import scala.collection.JavaConverters._

object BulkMeetingRecordCommand {
	def apply(studentRelationships: Seq[StudentRelationship], creator: Member) =
		new BulkMeetingRecordCommandInternal(studentRelationships, creator)
			with AutowiringMeetingRecordServiceComponent
			with AutowiringFeaturesComponent
			with AutowiringAttendanceMonitoringMeetingRecordServiceComponent
			with AutowiringFileAttachmentServiceComponent
			with ComposableCommand[Seq[MeetingRecord]]
			with MeetingRecordCommandBindListener
			with MeetingRecordValidation
			with BulkMeetingRecordDescription
			with BulkMeetingRecordPermissions
			with BulkMeetingRecordCommandState
			with MeetingRecordCommandRequest
			with BulkMeetingRecordCommandNotifications
}


class BulkMeetingRecordCommandInternal(val studentRelationships: Seq[StudentRelationship], val creator: Member) extends AbstractMeetingRecordCommand
	with CommandInternal[Seq[MeetingRecord]] with TaskBenchmarking {

	self: BulkMeetingRecordCommandState with MeetingRecordCommandRequest with MeetingRecordServiceComponent
		with FeaturesComponent with AttendanceMonitoringMeetingRecordServiceComponent
		with FileAttachmentServiceComponent =>

	override def applyInternal(): Seq[MeetingRecord] = {
		val meetingRecords = studentRelationships.map { studentRelationship =>
			new MeetingRecord(creator, studentRelationship)
		}
		benchmarkTask("BulkMeetingRecord") {
			meetingRecords.map { meeting => applyCommon(meeting) }
		}
	}
}


trait BulkMeetingRecordCommandNotifications extends Notifies[Seq[MeetingRecord], MeetingRecord]
	with SchedulesNotifications[Seq[MeetingRecord], MeetingRecord] {
	self: BulkMeetingRecordCommandState =>

	def emit(meetingRecords: Seq[MeetingRecord]): Seq[NewMeetingRecordApprovalNotification] = meetingRecords.map { meetingRecord =>
		Notification.init(new NewMeetingRecordApprovalNotification, creator.asSsoUser, meetingRecord, meetingRecord.relationship)
	}

	override def transformResult(meetingRecords: Seq[MeetingRecord]): Seq[MeetingRecord] = meetingRecords

	override def scheduledNotifications(meetingRecord: MeetingRecord) = Seq(
		new ScheduledNotification[MeetingRecord]("newMeetingRecordApproval", meetingRecord, DateTime.now.plusWeeks(1))
	)
}


trait BulkMeetingRecordPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: BulkMeetingRecordCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		studentRelationships.foreach { studentRelationship =>
			p.PermissionCheck(Permissions.Profiles.MeetingRecord.Manage(studentRelationship.relationshipType), mandatory(studentRelationship.studentMember))
		}
	}
}


trait BulkMeetingRecordDescription extends Describable[Seq[MeetingRecord]] {

	self: BulkMeetingRecordCommandState =>

	override def describe(d: Description) {
		d.property("creator" -> creator.universityId)
		d.property("students" -> studentRelationships.map(_.studentId))
	}

	override def describeResult(d: Description, meetings: Seq[MeetingRecord]) {
		d.property("meetingTitle" -> meetings.head.title)
		d.property("meetings" -> meetings.map(_.id))
		d.fileAttachments(meetings.flatMap(_.attachments.asScala))
	}
}

trait BulkMeetingRecordCommandState extends MeetingRecordCommandState {
	def studentRelationships: Seq[StudentRelationship]
}
