package uk.ac.warwick.tabula.commands.attendance.agent

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.attendance.view.{BuildsFilteredStudentsAttendanceResult, FilteredStudentsAttendanceResult}
import uk.ac.warwick.tabula.commands.attendance.{GroupedPoint, GroupsPoints}
import uk.ac.warwick.tabula.data.model.{Member, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringRelationshipServiceComponent, AutowiringTermServiceComponent, RelationshipServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

case class AgentStudentsCommandResult(
	studentAttendance: FilteredStudentsAttendanceResult,
	groupedPoints: Map[String, Seq[GroupedPoint]]
)

object AgentStudentsCommand {
	def apply(relationshipType: StudentRelationshipType, academicYear: AcademicYear, currentMember: Member) =
		new AgentStudentsCommandInternal(relationshipType, academicYear, currentMember)
			with ComposableCommand[AgentStudentsCommandResult]
			with AutowiringRelationshipServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with AgentStudentsPermissions
			with AgentStudentsCommandState
			with ReadOnly with Unaudited

}

class AgentStudentsCommandInternal(val relationshipType: StudentRelationshipType, val academicYear: AcademicYear, val currentMember: Member)
	extends CommandInternal[AgentStudentsCommandResult] with BuildsFilteredStudentsAttendanceResult with GroupsPoints {

	self: AttendanceMonitoringServiceComponent with RelationshipServiceComponent with TermServiceComponent =>

	override def applyInternal(): AgentStudentsCommandResult = {
		val students = relationshipService.listCurrentStudentRelationshipsWithMember(relationshipType, currentMember).flatMap(_.studentMember).distinct
		val pointMap = students.map { student =>
			student -> attendanceMonitoringService.listStudentsPoints(student, None, academicYear)
		}.toMap
		val points = pointMap.values.flatten.toSeq.distinct
		AgentStudentsCommandResult(
			buildAttendanceResult(students.size, students, None, academicYear, pointMap),
			groupByMonth(points, groupSimilar = true) ++ groupByTerm(points, groupSimilar = true)
		)
	}

}

trait AgentStudentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AgentStudentsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(relationshipType), currentMember)
	}

}

trait AgentStudentsCommandState {
	def relationshipType: StudentRelationshipType
	def academicYear: AcademicYear
	def currentMember: Member
}
