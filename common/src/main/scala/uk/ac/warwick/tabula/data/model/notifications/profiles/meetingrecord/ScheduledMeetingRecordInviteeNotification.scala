package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, MyWarwickActivity}
import uk.ac.warwick.userlookup.User

@Entity
@Proxy
@DiscriminatorValue(value = "ScheduledMeetingRecordInvitee")
class ScheduledMeetingRecordInviteeNotification extends ScheduledMeetingRecordNotification
  with AddsIcalAttachmentToScheduledMeetingNotification
  with MyWarwickActivity {

  def this(theVerb: String) = {
    this()
    verbSetting.value = theVerb
  }

  def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/meetingrecord/scheduled_meeting_record_invitee_notification.ftl"

  def title: String = s"Meeting with ${meeting.allParticipantNames} $verb by ${agent.getFullName}"

  override def titleFor(user: User): String = s"Meeting with ${meeting.participantNamesExcept(user)} $verb by ${agent.getFullName}"

  def actor: User = if (meeting.universityIdInRelationship(agent.getWarwickId)) {
    agent
  } else {
    // meeting was scheduled by someone else on the relationship agents behalf so actor is them
    meeting.agents.headOption.map(_.asSsoUser).getOrElse(throw new IllegalStateException("Relationship has no agent"))
  }

  def content = FreemarkerModel(FreemarkerTemplate, Map(
    "actor" -> actor,
    "agentRoles" -> agentRoles,
    "verb" -> verb,
    "dateTimeFormatter" -> dateTimeFormatter,
    "meetingRecord" -> meeting
  ))

  override def recipients: Seq[User] = {
    if (actor == meeting.student.asSsoUser) {
      meeting.agents.map(_.asSsoUser)
    } else {
      Seq(meeting.student.asSsoUser)
    }
  }

}

