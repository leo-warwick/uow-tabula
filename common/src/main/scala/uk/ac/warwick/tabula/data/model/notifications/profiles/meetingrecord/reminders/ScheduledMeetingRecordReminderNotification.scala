package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.reminders

import javax.persistence.{DiscriminatorValue, Entity}

import org.joda.time.{DateTime, Days}
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.ScheduledMeetingRecordNotification
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, SingleRecipientNotification}
import uk.ac.warwick.tabula.helpers.ConfigurableIntervalFormatter
import uk.ac.warwick.userlookup.User

abstract class ScheduledMeetingRecordReminderNotification extends ScheduledMeetingRecordNotification with SingleRecipientNotification {

	verbSetting.value = "remind"
	priority = Warning

	def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/meetingrecord/scheduled_meeting_record_reminder_notification.ftl"

	def referenceDate: DateTime = meeting.meetingDate
	def isToday: Boolean = {
		val today = DateTime.now.withTimeAtStartOfDay()
		val meetingDate = meeting.meetingDate.withTimeAtStartOfDay()
		today == meetingDate
	}

	def title: String = {
		val name =
			if (isAgent) meeting.relationship.studentMember.flatMap { _.fullName }.getOrElse("student")
			else meeting.relationship.agentName

		val timeFormat = ConfigurableIntervalFormatter.Hour12OptionalMins
		val date =
			if (isToday) s"today at ${timeFormat.formatTime(meeting.meetingDate).get}"
			else s"at ${timeFormat.formatTime(meeting.meetingDate).get}, ${meeting.meetingDate.toString("EEEE d MMMM yyyy")}"

		s"${agentRole.capitalize} meeting with $name $date"
	}

	def isAgent: Boolean

	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"isAgent" -> isAgent,
		"role" -> agentRole,
		"partner" -> (isAgent match {
			case true => meeting.relationship.studentMember.getOrElse(throw new IllegalStateException(studentNotFoundMessage))
			case false => meeting.relationship.agentMember.getOrElse(throw new IllegalStateException(agentNotFoundMessage))
		}),
		"dateTimeFormatter" -> dateTimeFormatter,
		"meetingRecord" -> meeting
	))
}

@Entity
@DiscriminatorValue(value="ScheduledMeetingRecordReminderStudent")
class ScheduledMeetingRecordReminderStudentNotification extends ScheduledMeetingRecordReminderNotification {

	override def isAgent = false

	override def recipient: User = meeting.relationship.studentMember.getOrElse(throw new IllegalStateException(studentNotFoundMessage)).asSsoUser

}

@Entity
@DiscriminatorValue(value="ScheduledMeetingRecordReminderAgent")
class ScheduledMeetingRecordReminderAgentNotification extends ScheduledMeetingRecordReminderNotification {

	override def isAgent = true

	override def recipient: User = meeting.relationship.agentMember.getOrElse(throw new IllegalStateException(agentNotFoundMessage)).asSsoUser

}
