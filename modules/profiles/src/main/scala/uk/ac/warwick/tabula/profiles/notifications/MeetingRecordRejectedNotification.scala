package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.data.model.MeetingRecordApproval

class MeetingRecordRejectedNotification(approval: MeetingRecordApproval)
	extends MeetingRecordNotification(approval.meetingRecord){

	val actor = approval.approver.asSsoUser
	val verb = "reject"

	def title = "Meeting record rejected"
	def content = renderToString(FreemarkerTemplate, Map(
		"actor" -> actor,
		"dateFormatter" -> dateFormatter,
		"meetingRecord" -> approval.meetingRecord,
		"verbed" -> "rejected",
		"nextActionDescription" -> "edit the record and submit it for approval again",
		"reason" -> approval.comments,
		"profileLink" -> url
	))
	def recipients = Seq(approval.meetingRecord.creator.asSsoUser)
}
