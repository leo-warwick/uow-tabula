package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, MyWarwickActivity}
import uk.ac.warwick.userlookup.User

@Entity
@Proxy
@DiscriminatorValue(value = "ScheduledMeetingRecordBehalf")
class ScheduledMeetingRecordBehalfNotification
  extends ScheduledMeetingRecordNotification
    with AddsIcalAttachmentToScheduledMeetingNotification
    with MyWarwickActivity {

  def this(theVerb: String) = {
    this()
    verbSetting.value = theVerb
  }

  def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/meetingrecord/scheduled_meeting_record_behalf_notification.ftl"

  def title = s"Meeting with ${meeting.allParticipantNames} $verb on your behalf by ${agent.getFullName}"

  override def titleFor(user: User): String = s"Meeting with ${meeting.participantNamesExcept(user)} $verb on your behalf by ${agent.getFullName}"

  def student: User = meeting.student.asSsoUser

  def content = FreemarkerModel(FreemarkerTemplate, Map(
    "actor" -> agent,
    "student" -> student,
    "agentRoles" -> agentRoles,
    "verb" -> verb,
    "dateTimeFormatter" -> dateTimeFormatter,
    "meetingRecord" -> meeting
  ))

  def recipients: Seq[User] = meeting.agents.map(_.asSsoUser)

}
