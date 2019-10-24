package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.DateTime
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils._
import uk.ac.warwick.tabula.DateFormats.DateTimePickerFormatter
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.collection.JavaConverters._
import scala.util.Try

trait ScheduledMeetingRecordValidation extends AttachedFilesValidation {
  self: AbstractScheduledMeetingRecordCommandState =>

  def sharedValidation(errors: Errors, title: String, meetingDateStr: String, meetingTimeStr: String, meetingEndTimeStr: String, meetingLocation: String) {
    rejectIfEmptyOrWhitespace(errors, "title", "NotEmpty")
    if (title.hasText && title.length > MeetingRecord.MaxTitleLength) {
      errors.rejectValue("title", "meetingRecord.title.long", Array(MeetingRecord.MaxTitleLength.toString), "")
    }

    if (meetingLocation != null && meetingLocation.length > MeetingRecord.MaxLocationLength) {
      errors.rejectValue("meetingLocation", "meetingRecord.location.long", Array(MeetingRecord.MaxLocationLength.toString), "")
    }

    if (relationships.isEmpty) {
      errors.rejectValue("relationships", "meetingRecord.relationships.none")
    }

    rejectIfEmptyOrWhitespace(errors, "format", "NotEmpty")
    rejectIfEmptyOrWhitespace(errors, "meetingDateStr", "NotEmpty")
    rejectIfEmptyOrWhitespace(errors, "meetingTimeStr", "NotEmpty")
    rejectIfEmptyOrWhitespace(errors, "meetingEndTimeStr", "NotEmpty")

    if (!errors.hasErrors) {
      val startDate = Try(DateTimePickerFormatter.parseDateTime(meetingDateStr + " " + meetingTimeStr))
      val endDate = Try(DateTimePickerFormatter.parseDateTime(meetingDateStr + " " + meetingEndTimeStr))

      if (startDate.isFailure) {
        errors.rejectValue("meetingTimeStr", "meetingRecord.date.missing")
      } else if (endDate.isFailure) {
        errors.rejectValue("meetingEndTimeStr", "meetingRecord.date.missing")
      } else {
        if (startDate.get.isAfter(endDate.get) || startDate.get.isEqual(endDate.get)) {
          errors.rejectValue("meetingTimeStr", "meetingRecord.date.endbeforestart")
        }
        if (startDate.get.isBeforeNow) {
          errors.rejectValue("meetingDateStr", "meetingRecord.date.past")
        } else if (startDate.get.isAfter(DateTime.now.plusYears(MeetingRecord.MeetingTooOldThresholdYears).toDateTime)) {
          errors.rejectValue("meetingDateStr", "meetingRecord.date.futuristic")
        }
      }
    }

    attachedFilesValidation(errors, Option(attachedFiles).getOrElse(JList()).asScala, Option(file.attached).getOrElse(JList()).asScala)

  }
}
