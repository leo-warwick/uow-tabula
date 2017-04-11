package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent

@Entity
@DiscriminatorValue("meetingRecordRejected")
class MeetingRecordRejectedNotification
	extends Notification[MeetingRecordApproval, Unit]
	with MeetingRecordNotificationTrait
	with SingleItemNotification[MeetingRecordApproval]
	with AllCompletedActionRequiredNotification
	with AutowiringTermServiceComponent {

	priority = NotificationPriority.Warning

	def approval: MeetingRecordApproval = item.entity
	def meeting: MeetingRecord = approval.meetingRecord
	def relationship: StudentRelationship = meeting.relationship

	def verb = "return"

	def title: String = {
		val name =
			if (meeting.creator.universityId == meeting.relationship.studentId) meeting.relationship.agentName
			else meeting.relationship.studentMember.flatMap { _.fullName }.getOrElse("student")

		s"${agentRole.capitalize} meeting record with $name returned with comments"
	}

	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> agent,
		"role"->agentRole,
		"dateFormatter" -> dateOnlyFormatter,
		"meetingRecord" -> approval.meetingRecord,
		"verbed" -> "returned",
		"reason" -> approval.comments
	))

	def urlTitle = "edit the record and submit it for approval again"

	def recipients = Seq(approval.meetingRecord.creator.asSsoUser)
}
