package uk.ac.warwick.tabula.commands.attendance.check

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringMeetingRecordServiceComponent, AutowiringAttendanceMonitoringMeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object CheckMeetingRecordCheckpointCommand {
	def apply(student: StudentMember, relationshipType: StudentRelationshipType, meetingFormat: MeetingFormat, meetingDate: DateTime) =
		new CheckMeetingRecordCheckpointCommandInternal(student, relationshipType, meetingFormat, meetingDate)
			with ComposableCommand[Boolean]
			with AutowiringAttendanceMonitoringMeetingRecordServiceComponent
			with CheckMeetingRecordCheckpointPermissions
			with CheckMeetingRecordCheckpointCommandState
			with ReadOnly with Unaudited
}

class CheckMeetingRecordCheckpointCommandInternal(val student: StudentMember, val relationshipType: StudentRelationshipType, val meetingFormat: MeetingFormat, val meetingDate: DateTime)
	extends CommandInternal[Boolean] {

	self: AttendanceMonitoringMeetingRecordServiceComponent =>

	override def applyInternal(): Boolean = {
		val relationship = new MemberStudentRelationship
		relationship.relationshipType = relationshipType
		relationship.studentMember = student
		val meeting = new MeetingRecord
		meeting.relationship = relationship
		meeting.format = meetingFormat
		meeting.meetingDate = meetingDate
		attendanceMonitoringMeetingRecordService.getCheckpoints(meeting)
			.map(c => c.student).nonEmpty
	}

}

trait CheckMeetingRecordCheckpointPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: CheckMeetingRecordCheckpointCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.MeetingRecord.Read(mandatory(relationshipType)), student)
	}

}

trait CheckMeetingRecordCheckpointCommandState {
	def student: StudentMember
	def relationshipType: StudentRelationshipType
	def meetingFormat: MeetingFormat
	def meetingDate: DateTime
}
