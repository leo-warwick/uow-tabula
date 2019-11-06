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

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

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

    if (features.meetingRecordApproval && !meeting.missed) {
      updateMeetingApproval(meeting)
    }

    if (features.attendanceMonitoringMeetingPointType) {
      attendanceMonitoringMeetingRecordService.updateCheckpoints(meeting)
    }

    meeting
  }

  private def persistAttachments(meeting: MeetingRecord) {

    // delete attachments that have been removed
    if (meeting.attachments != null) {
      val filesToKeep = Option(attachedFiles).map(_.asScala.toSeq).getOrElse(Seq())
      val filesToRemove = meeting.attachments.asScala.toSeq diff filesToKeep
      meeting.attachments = JArrayList[FileAttachment](filesToKeep)
      fileAttachmentService.deleteAttachments(filesToRemove)
    }

    val newAttachments = file.attached.asScala.map(_.duplicate())
    newAttachments.foreach(meeting.addAttachment)
  }

  private def updateMeetingApproval(meetingRecord: MeetingRecord): Seq[MeetingRecordApproval] = {
    def createOrUpdateApproval(approver: Member): MeetingRecordApproval = {
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

    // Remove approvals for any removed participants
    meetingRecord.approvals.asScala.filterNot(a => meetingRecord.participants.contains(a.approver)).foreach { approval =>
      approval.meetingRecord = null
      meetingRecord.approvals.remove(approval)
      meetingRecordService.purge(approval)
    }

    val approvers = if (meetingRecord.participants.contains(meetingRecord.creator)) {
      // Approval is required from all participants except the person who created the record
      meetingRecord.participants.filter(_ != meetingRecord.creator)
    } else {
      // The record was created on behalf of the agents
      // Only the student needs to approve the record
      Seq(meetingRecord.student)
    }

    approvers.map(createOrUpdateApproval)
  }
}

trait MeetingRecordCommandBindListener extends BindListener {

  self: MeetingRecordCommandRequest =>

  override def onBind(result: BindingResult): Unit = transactional() {
    result.pushNestedPath("file")
    file.onBind(result)
    result.popNestedPath()
  }
}

trait MeetingRecordValidation extends SelfValidating with AttachedFilesValidation {

  self: MeetingRecordCommandRequest with MeetingRecordCommandState =>

  override def validate(errors: Errors): Unit = {

    rejectIfEmptyOrWhitespace(errors, "title", "NotEmpty")
    if (title.length > MeetingRecord.MaxTitleLength) {
      errors.rejectValue("title", "meetingRecord.title.long", Array(MeetingRecord.MaxTitleLength.toString), "")
    }

    rejectIfEmptyOrWhitespace(errors, "format", "NotEmpty")

    val dateToCheck: DateTime = if (isRealTime) {
      Try(DateTimePickerFormatter.parseDateTime(meetingDateStr + " " + meetingTimeStr))
        .orElse(Try(DatePickerFormatter.parseDateTime(meetingDateStr)))
        .getOrElse(null)
    } else {
      meetingDate.toDateTimeAtStartOfDay
    }

    if (meetingLocation.length > MeetingRecord.MaxLocationLength) {
      errors.rejectValue("meetingLocation", "meetingRecord.location.long", Array(MeetingRecord.MaxLocationLength.toString), "")
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

      val startDateTime: Try[DateTime] = Try(DateTimePickerFormatter.parseDateTime(meetingDateStr + " " + meetingTimeStr))
      val endDateTime: Try[DateTime] = Try(DateTimePickerFormatter.parseDateTime(meetingDateStr + " " + meetingEndTimeStr))

      (startDateTime, endDateTime) match {
        case (Failure(_), Failure(_)) =>
          errors.rejectValue("meetingTimeStr", "meetingRecord.time.invalid")
          errors.rejectValue("meetingEndTimeStr", "meetingRecord.time.invalid")
        case (Failure(_), _) => errors.rejectValue("meetingTimeStr", "meetingRecord.time.invalid")
        case (_, Failure(_)) => errors.rejectValue("meetingEndTimeStr", "meetingRecord.time.invalid")
        case (Success(start), Success(end)) if end.isBefore(start) || start.isEqual(end) => errors.rejectValue("meetingTimeStr", "meetingRecord.date.endbeforestart")
        case _ => // no validation errors
      }
    }

    attachedFilesValidation(errors, Option(attachedFiles).getOrElse(JList()).asScala.toSeq, Option(file.attached).getOrElse(JList()).asScala.toSeq)
  }
}

trait AttachedFilesValidation {
  def attachedFilesValidation(errors: Errors, existingAttachments: Seq[FileAttachment], newAttachments: Seq[FileAttachment]): Unit = {
    for (attached <- newAttachments; existing <- existingAttachments if attached.name == existing.name) {
      errors.rejectValue("attachedFiles", "meetingRecord.files.duplicateName")
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

  var relationships: JList[StudentRelationship] = JArrayList()

  var meetingDate: LocalDate = _
  var meetingDateStr: String = _
  if (meetingDate != null) {
    meetingDateStr = meetingDate.toString(DatePickerFormatter)
  }

  var meetingTime: DateTime = DateTime.now.hourOfDay.roundFloorCopy
  var meetingTimeStr: String = meetingTime.toString(TimePickerFormatter)

  var meetingEndDate: LocalDate = _

  var meetingEndTime: DateTime = DateTime.now.plusHours(1).hourOfDay.roundFloorCopy
  var meetingEndTimeStr: String = meetingEndTime.toString(TimePickerFormatter)

  var meetingDateTime: DateTime = DateTime.now.hourOfDay.roundFloorCopy
  var meetingEndDateTime: DateTime = DateTime.now.plusHours(1).hourOfDay.roundFloorCopy

  var meetingLocation: String = _
  var meetingLocationId: String = _

  var format: MeetingFormat = _
  var file: UploadedFile = new UploadedFile
  var attachedFiles: JList[FileAttachment] = _
}

