package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils._
import uk.ac.warwick.tabula.DateFormats.{DatePickerFormatter, TimePickerFormatter}
import uk.ac.warwick.tabula.FeaturesComponent
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.services.{FileAttachmentServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

abstract class AbstractModifyMeetingRecordCommand extends AbstractMeetingRecordCommand with CommandInternal[MeetingRecord] {
	self: ModifyMeetingRecordCommandState with MeetingRecordCommandRequest with MeetingRecordServiceComponent
		with FeaturesComponent with AttendanceMonitoringMeetingRecordServiceComponent
		with FileAttachmentServiceComponent =>
}

trait PopulateMeetingRecordCommand extends PopulateOnForm {

	self: MeetingRecordCommandRequest with EditMeetingRecordCommandState =>

	override def populate(): Unit = {
		title = meetingRecord.title
		description = meetingRecord.description
		HibernateHelpers.initialiseAndUnproxy(meetingRecord) match {
			case meeting: MeetingRecord =>
				isRealTime = meeting.isRealTime
				if (meeting.isRealTime) {
					meetingDateStr = meetingRecord.meetingDate.toString(DatePickerFormatter)
					meetingTimeStr = meetingRecord.meetingDate.withHourOfDay(meetingRecord.meetingDate.getHourOfDay).toString(TimePickerFormatter)
					meetingEndTimeStr = meetingRecord.meetingEndDate.withHourOfDay(meetingRecord.meetingEndDate.getHourOfDay).toString(TimePickerFormatter)
				} else {
					meetingDate = meetingRecord.meetingDate.toLocalDate
					meetingTime = meetingRecord.meetingDate.withHourOfDay(meetingRecord.meetingDate.getHourOfDay)
					meetingEndTime = meetingRecord.meetingEndDate.withHourOfDay(meetingRecord.meetingEndDate.getHourOfDay).plusHours(1)
				}
			case _: ScheduledMeetingRecord =>
				meetingDateStr = meetingRecord.meetingDate.toString(DatePickerFormatter)
				meetingTimeStr = meetingRecord.meetingDate.withHourOfDay(meetingRecord.meetingDate.getHourOfDay).toString(TimePickerFormatter)
				meetingEndTimeStr = meetingRecord.meetingEndDate.withHourOfDay(meetingRecord.meetingEndDate.getHourOfDay).toString(TimePickerFormatter)
		}


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

trait ModifyMeetingRecordValidation extends MeetingRecordValidation {

	self: MeetingRecordCommandRequest with ModifyMeetingRecordCommandState =>

	override def validate(errors: Errors) {

		super.validate(errors)

		rejectIfEmptyOrWhitespace(errors, "relationship", "NotEmpty")

	}

}

trait ModifyMeetingRecordPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ModifyMeetingRecordCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.MeetingRecord.Manage(relationship.relationshipType), mandatory(relationship.studentMember))
	}

}

trait ModifyMeetingRecordDescription extends Describable[MeetingRecord] {

	self: ModifyMeetingRecordCommandState =>

	override def describe(d: Description) {
		relationship.studentMember.map(d.member)
		d.properties(
			"creator" -> creator.universityId,
			"relationship" -> relationship.relationshipType.toString()
		)
	}

	override def describeResult(d: Description, meeting: MeetingRecord) {
		relationship.studentMember.map(d.member)
		d.properties(
			"creator" -> creator.universityId,
			"relationship" -> relationship.relationshipType.toString(),
			"meeting" -> meeting.id
		)
		d.fileAttachments(meeting.attachments.asScala)
	}
}

trait ModifyMeetingRecordCommandState extends MeetingRecordCommandState {
	def relationship: StudentRelationship
}


