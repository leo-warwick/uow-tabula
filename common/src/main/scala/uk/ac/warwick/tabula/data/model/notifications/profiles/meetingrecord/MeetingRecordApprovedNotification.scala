package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent

@Entity
@DiscriminatorValue("meetingRecordApproved")
class MeetingRecordApprovedNotification
	extends Notification[MeetingRecordApproval, Unit]
	with MeetingRecordNotificationTrait
	with SingleItemNotification[MeetingRecordApproval]
	with AutowiringTermServiceComponent {

	def approval: MeetingRecordApproval = item.entity
	def meeting: MeetingRecord = approval.meetingRecord
	def relationship: StudentRelationship = meeting.relationship

	def verb = "approve"

	def title: String = {
		val name =
			if (meeting.creator.universityId == meeting.relationship.studentId) meeting.relationship.agentName
			else meeting.relationship.studentMember.flatMap { _.fullName }.getOrElse("student")

		s"${agentRole.capitalize} meeting record with $name approved"
	}

	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> agent,
		"role"->agentRole,
		"dateFormatter" -> dateOnlyFormatter,
		"meetingRecord" -> approval.meetingRecord,
		"verbed" -> "approved"
	))

	def urlTitle = "view the meeting record"

	def recipients = Seq(approval.meetingRecord.creator.asSsoUser)
}

