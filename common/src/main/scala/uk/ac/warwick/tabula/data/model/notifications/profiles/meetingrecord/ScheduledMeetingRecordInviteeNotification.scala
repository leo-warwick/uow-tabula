package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model.{FreemarkerModel, MyWarwickActivity, SingleRecipientNotification}
import uk.ac.warwick.userlookup.User

@Entity
@DiscriminatorValue(value="ScheduledMeetingRecordInvitee")
class ScheduledMeetingRecordInviteeNotification extends ScheduledMeetingRecordNotification
	with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification
	with MyWarwickActivity {

	def this(theVerb: String) {
		this()
		verbSetting.value = theVerb
	}

	def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/meetingrecord/scheduled_meeting_record_invitee_notification.ftl"

	def title: String = {
		val name =
			if (actor.getWarwickId == meeting.relationship.studentId) meeting.relationship.studentMember.flatMap { _.fullName }.getOrElse("student")
			else meeting.relationship.agentName

		s"${agentRole.capitalize} meeting with $name $verb by ${agent.getFullName}"
	}

	def actor: User = if (meeting.universityIdInRelationship(agent.getWarwickId)) {
		agent
	} else {
		// meeting was scheduled by someone else on the relationship agents behalf so actor is them
		meeting.relationship.agentMember.map(_.asSsoUser).getOrElse(throw new IllegalStateException("Relationship has no agent"))
	}

	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> actor,
		"role" -> agentRole,
		"verb" -> verb,
		"dateTimeFormatter" -> dateTimeFormatter,
		"meetingRecord" -> meeting
	))

	def recipient: User = {
		if (actor.getWarwickId == meeting.relationship.studentId) {
			meeting.relationship.agentMember.getOrElse(throw new IllegalStateException(agentNotFoundMessage)).asSsoUser
		} else {
			meeting.relationship.studentMember.getOrElse(throw new IllegalStateException(studentNotFoundMessage)).asSsoUser
		}
	}

}

