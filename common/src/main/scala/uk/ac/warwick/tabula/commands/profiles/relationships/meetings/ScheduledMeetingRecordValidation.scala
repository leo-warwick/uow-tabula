package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.DateTime
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils._
import uk.ac.warwick.tabula.DateFormats.DateTimePickerFormatter
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.util.Try

trait ScheduledMeetingRecordValidation {

	def sharedValidation(errors: Errors, title: String, meetingDateStr: String, meetingTimeStr: String, meetingEndTimeStr: String) {
		rejectIfEmptyOrWhitespace(errors, "title", "NotEmpty")
		if (title.hasText && title.length > MeetingRecord.MaxTitleLength){
			errors.rejectValue("title", "meetingRecord.title.long", new Array(MeetingRecord.MaxTitleLength), "")
		}

		rejectIfEmptyOrWhitespace(errors, "relationship", "NotEmpty")
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

	}
}
