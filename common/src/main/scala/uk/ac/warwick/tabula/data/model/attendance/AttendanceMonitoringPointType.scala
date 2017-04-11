package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.data.model.AbstractStringUserType

sealed abstract class AttendanceMonitoringPointType(val dbValue: String, val description: String)

object AttendanceMonitoringPointType {
	case object Standard extends AttendanceMonitoringPointType("standard", "Standard")
	case object Meeting extends AttendanceMonitoringPointType("meeting", "Meeting")
	case object SmallGroup extends AttendanceMonitoringPointType("smallGroup", "Teaching event")
	case object AssignmentSubmission extends AttendanceMonitoringPointType("assignmentSubmission", "Coursework")

	def fromCode(code: String): AttendanceMonitoringPointType = code match {
		case Standard.dbValue => Standard
		case Meeting.dbValue => Meeting
		case SmallGroup.dbValue => SmallGroup
		case AssignmentSubmission.dbValue => AssignmentSubmission
		case _ => throw new IllegalArgumentException()
	}

	val values: Seq[AttendanceMonitoringPointType] = Seq(Standard, Meeting, SmallGroup, AssignmentSubmission)
}

class AttendanceMonitoringPointTypeUserType extends AbstractStringUserType[AttendanceMonitoringPointType] {

	override def convertToObject(string: String): AttendanceMonitoringPointType = AttendanceMonitoringPointType.fromCode(string)

	override def convertToValue(state: AttendanceMonitoringPointType): String = state.dbValue

}