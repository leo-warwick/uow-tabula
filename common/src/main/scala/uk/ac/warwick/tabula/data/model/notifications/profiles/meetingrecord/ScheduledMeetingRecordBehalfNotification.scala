package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model.{FreemarkerModel, MyWarwickActivity, SingleRecipientNotification}
import uk.ac.warwick.userlookup.User

@Entity
@DiscriminatorValue(value="ScheduledMeetingRecordBehalf")
class ScheduledMeetingRecordBehalfNotification
	extends ScheduledMeetingRecordNotification with SingleRecipientNotification
	with AddsIcalAttachmentToScheduledMeetingNotification
	with MyWarwickActivity {

	def this(theVerb: String) {
		this()
		verbSetting.value = theVerb
	}

	def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/meetingrecord/scheduled_meeting_record_behalf_notification.ftl"

	def title = s"${agentRole.capitalize} meeting with ${student.getFullName} $verb on your behalf by ${agent.getFullName}"

	def student: User = meeting.relationship.studentMember.getOrElse(throw new IllegalStateException(studentNotFoundMessage)).asSsoUser

	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> agent,
		"student" -> student,
		"role" -> agentRole,
		"verb" -> verb,
		"dateTimeFormatter" -> dateTimeFormatter,
		"meetingRecord" -> meeting
	))

	def recipient: User = meeting.relationship.agentMember.getOrElse(throw new IllegalStateException(agentNotFoundMessage)).asSsoUser

}
