package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.TermServiceComponent

trait MeetingRecordNotificationTrait {

	self: TermServiceComponent =>

	def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/meetingrecord/meeting_record_notification_template.ftl"

	def meeting: MeetingRecord

	def academicYear: AcademicYear = AcademicYear.findAcademicYearContainingDate(meeting.meetingDate)

	def url: String = Routes.Profile.relationshipType(
		meeting.relationship.studentCourseDetails,
		academicYear,
		meeting.relationship.relationshipType
	)

	def agentRole: String = meeting.relationship.relationshipType.agentRole
}
