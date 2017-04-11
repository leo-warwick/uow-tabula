package uk.ac.warwick.tabula.commands.attendance.profile

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{MeetingRecord, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{RelationshipServiceComponent, AutowiringRelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.data.{MeetingRecordDaoComponent, AutowiringMeetingRecordDaoComponent}
import scala.collection.mutable

object ViewMeetingsForPointCommand {
	def apply(student: StudentMember, point: AttendanceMonitoringPoint) =
		new ViewMeetingsForPointCommand(student, point)
			with ComposableCommand[Seq[(MeetingRecord, Seq[String])]]
			with ViewMeetingsForPointPermission
			with ViewMeetingsForPointCommandState
			with AutowiringRelationshipServiceComponent
			with AutowiringMeetingRecordDaoComponent
			with ReadOnly with Unaudited
}

class ViewMeetingsForPointCommand(val student: StudentMember, val point: AttendanceMonitoringPoint)
	extends CommandInternal[Seq[(MeetingRecord, Seq[String])]] with ViewMeetingsForPointCommandState {

	self: RelationshipServiceComponent with MeetingRecordDaoComponent =>

	override def applyInternal(): Seq[(MeetingRecord, mutable.Buffer[String])] = {
		// Get all the enabled relationship types for a department
		val allRelationshipTypes = relationshipService.allStudentRelationshipTypes

		val allMeetings = allRelationshipTypes.flatMap{ relationshipType =>
			relationshipService.getRelationships(relationshipType, student).flatMap(meetingRecordDao.list)
		}

		allMeetings.map{meeting => meeting -> {
			val reasons: mutable.Buffer[String] = mutable.Buffer()
			if (!point.meetingRelationships.contains(meeting.relationship.relationshipType))
				reasons += s"Meeting was not with ${point.meetingRelationships.map{_.agentRole}.mkString(" or ")}"

			if (!point.meetingFormats.contains(meeting.format))
				reasons += s"Meeting was not a ${point.meetingFormats.map{_.description}.mkString(" or ")}"

			if (meeting.isRejected)
				reasons += s"Rejected by ${meeting.relationship.relationshipType.agentRole}"
			else if (!meeting.isAttendanceApproved)
				reasons += s"Awaiting approval by ${meeting.relationship.relationshipType.agentRole}"

			if (meeting.meetingDate.toLocalDate.isBefore(point.startDate))
				reasons += "Took place before"

			if (meeting.meetingDate.toLocalDate.isAfter(point.endDate))
				reasons += "Took place after"

			reasons
		}}
	}

}

trait ViewMeetingsForPointPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewMeetingsForPointCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, student)
	}
}

trait ViewMeetingsForPointCommandState {
	def student: StudentMember
	def point: AttendanceMonitoringPoint
}