package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.{DateTime, LocalDate}
import org.springframework.validation.ValidationUtils._
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.DateFormats.{DatePickerFormatter, DateTimePickerFormatter, TimePickerFormatter}
import uk.ac.warwick.tabula.FeaturesComponent
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.MeetingApprovalState.Pending
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.services.{FileAttachmentServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.BindListener

import scala.collection.JavaConverters._

abstract class AbstractMeetingRecordCommand {

	self: MeetingRecordCommandRequest with MeetingRecordServiceComponent
		with FeaturesComponent with AttendanceMonitoringMeetingRecordServiceComponent
		with FileAttachmentServiceComponent =>

	protected def applyCommon(meeting: MeetingRecord): MeetingRecord = {
		meeting.title = title
		meeting.description = description
		if (meeting.isRealTime) {
			if (meetingDateStr.hasText && meetingTimeStr.hasText) {
				meeting.meetingDate = DateTimePickerFormatter.parseDateTime(meetingDateStr + " " + meetingTimeStr)
			}
			if (meetingDateStr.hasText && meetingEndTimeStr.hasText) {
				meeting.meetingEndDate = DateTimePickerFormatter.parseDateTime(meetingDateStr + " " + meetingEndTimeStr)
			}
		} else {
			meeting.meetingDate = meetingDate.toDateTimeAtStartOfDay.withHourOfDay(MeetingRecord.DefaultMeetingTimeOfDay)
			meeting.meetingEndDate = meetingEndDate.toDateTimeAtStartOfDay.withHourOfDay(MeetingRecord.DefaultMeetingTimeOfDay).plusHours(1)
		}
		if (meetingLocation.hasText) {
			if (meetingLocationId.hasText) {
				meeting.meetingLocation = MapLocation(meetingLocation, meetingLocationId)
			} else {
				meeting.meetingLocation = NamedLocation(meetingLocation)
			}
		} else {
			meeting.meetingLocation = null
		}

		meeting.format = format
		meeting.lastUpdatedDate = DateTime.now
		persistAttachments(meeting)

		// persist the meeting record
		meetingRecordService.saveOrUpdate(meeting)

		if (features.meetingRecordApproval) {
			updateMeetingApproval(meeting)
		}

		if (features.attendanceMonitoringMeetingPointType) {
			attendanceMonitoringMeetingRecordService.updateCheckpoints(meeting)
		}

		meeting
	}

	private def persistAttachments(meeting: MeetingRecord) {

		// delete attachments that have been removed
		if (meeting.attachments != null && meeting.attachments.size() > 0) {
			val filesToKeep = Option(attachedFiles).map(_.asScala.toList).getOrElse(List())
			val filesToRemove = meeting.attachments.asScala -- filesToKeep
			meeting.attachments = JArrayList[FileAttachment](filesToKeep)
			fileAttachmentService.deleteAttachments(filesToRemove)
		}

		val newAttachments = file.attached.asScala.map(_.duplicate())
		newAttachments.foreach(meeting.addAttachment)
	}

	private def updateMeetingApproval(meetingRecord: MeetingRecord): Option[MeetingRecordApproval] = {

		def getMeetingRecord(approver: Member): MeetingRecordApproval = {

			val meetingRecordApproval = meetingRecord.approvals.asScala.find(_.approver == approver).getOrElse {
				val newMeetingRecordApproval = new MeetingRecordApproval()
				newMeetingRecordApproval.approver = approver
				newMeetingRecordApproval.meetingRecord = meetingRecord
				meetingRecord.approvals.add(newMeetingRecordApproval)
				newMeetingRecordApproval
			}
			meetingRecordApproval.state = Pending
			meetingRecordService.saveOrUpdate(meetingRecordApproval)
			meetingRecordApproval
		}

		val approver = Seq(meetingRecord.relationship.agentMember, meetingRecord.relationship.studentMember).flatten.find(_ != meetingRecord.creator)
		approver.map(getMeetingRecord)
	}
}

trait MeetingRecordCommandBindListener extends BindListener {

	self: MeetingRecordCommandRequest =>

	override def onBind(result: BindingResult): Unit = transactional() {
		file.onBind(result)
	}
}

trait MeetingRecordValidation extends SelfValidating {

	self: MeetingRecordCommandRequest with MeetingRecordCommandState =>

	override def validate(errors: Errors) {

		rejectIfEmptyOrWhitespace(errors, "title", "NotEmpty")
		if (title.length > MeetingRecord.MaxTitleLength) {
			errors.rejectValue("title", "meetingRecord.title.long", new Array(MeetingRecord.MaxTitleLength), "")
		}

		rejectIfEmptyOrWhitespace(errors, "format", "NotEmpty")

		val dateToCheck: DateTime = if (isRealTime) {
			if ((!meetingDateStr.isEmptyOrWhitespace) && (!meetingTimeStr.isEmptyOrWhitespace)) {
				DateTimePickerFormatter.parseDateTime(meetingDateStr + " " + meetingTimeStr)
			} else {
				new DateTime
			}
		} else {
			meetingDate.toDateTimeAtStartOfDay
		}

		if (dateToCheck == null) {
			errors.rejectValue("meetingDateStr", "meetingRecord.date.missing")
		} else {
			if (dateToCheck.isAfter(DateTime.now)) {
				errors.rejectValue("meetingDateStr", "meetingRecord.date.future")
			} else if (dateToCheck.isBefore(DateTime.now.minusYears(MeetingRecord.MeetingTooOldThresholdYears))) {
				errors.rejectValue("meetingDateStr", "meetingRecord.date.prehistoric")
			}
		}

		if (meetingTimeStr.isEmptyOrWhitespace) {
			errors.rejectValue("meetingTimeStr", "meetingRecord.starttime.missing")
		}
		if (meetingEndTimeStr.isEmptyOrWhitespace) {
			errors.rejectValue("meetingEndTimeStr", "meetingRecord.endtime.missing")
		}

		if ((!meetingDateStr.isEmptyOrWhitespace) && (!meetingTimeStr.isEmptyOrWhitespace) && (!meetingEndTimeStr.isEmptyOrWhitespace)) {

			val startDateTime: DateTime = DateTimePickerFormatter.parseDateTime(meetingDateStr + " " + meetingTimeStr)
			val endDateTime: DateTime = DateTimePickerFormatter.parseDateTime(meetingDateStr + " " + meetingEndTimeStr)

			if (endDateTime.isBefore(startDateTime) || startDateTime.isEqual(endDateTime)) {
				errors.rejectValue("meetingTimeStr", "meetingRecord.date.endbeforestart")
			}

		}
	}
}

trait MeetingRecordCommandState {
	def creator: Member
	val attachmentTypes: Seq[String] = Seq[String]()
	var isRealTime: Boolean = true
}

trait MeetingRecordCommandRequest {
	var title: String = _
	var description: String = _

	var meetingDate: LocalDate = _
	var meetingDateStr: String  = _
	if(meetingDate != null){
		meetingDateStr = meetingDate.toString(DatePickerFormatter)
	}

	var meetingTime: DateTime = DateTime.now.hourOfDay.roundFloorCopy
	var meetingTimeStr: String  = meetingTime.toString(TimePickerFormatter)

	var meetingEndDate: LocalDate = _

	var meetingEndTime: DateTime = DateTime.now.plusHours(1).hourOfDay.roundFloorCopy
	var meetingEndTimeStr: String  = meetingEndTime.toString(TimePickerFormatter)

	var meetingDateTime: DateTime = DateTime.now.hourOfDay.roundFloorCopy
	var meetingEndDateTime: DateTime = DateTime.now.plusHours(1).hourOfDay.roundFloorCopy

	var meetingLocation: String = _
	var meetingLocationId: String = _

	var format: MeetingFormat = _
	var file: UploadedFile = new UploadedFile
	var attachedFiles: JList[FileAttachment] = _
}
