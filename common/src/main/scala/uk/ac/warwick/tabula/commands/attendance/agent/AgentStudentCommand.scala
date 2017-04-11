package uk.ac.warwick.tabula.commands.attendance.agent

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.GroupsPoints
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object AgentStudentCommand {
	def apply(relationshipType: StudentRelationshipType, academicYear: AcademicYear, student: StudentMember) =
		new AgentStudentCommandInternal(relationshipType, academicYear, student)
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
			with ComposableCommand[Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]]]
			with AgentStudentPermissions
			with AgentStudentCommandState
			with ReadOnly with Unaudited
}


class AgentStudentCommandInternal(val relationshipType: StudentRelationshipType, val academicYear: AcademicYear, val student: StudentMember)
	extends CommandInternal[Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]]]
	with GroupsPoints with TaskBenchmarking {

	self: AttendanceMonitoringServiceComponent with TermServiceComponent =>

	override def applyInternal(): Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]] = {
		val points = benchmarkTask("listStudentsPoints"){
			attendanceMonitoringService.listStudentsPoints(student, None, academicYear)
		}
		val checkpointMap = attendanceMonitoringService.getCheckpoints(points, student)
		val groupedPoints = groupByTerm(points, groupSimilar = false) ++ groupByMonth(points, groupSimilar = false)
		groupedPoints.map{case(period, thesePoints) =>
			period -> thesePoints.map{ groupedPoint =>
				groupedPoint.templatePoint -> checkpointMap.getOrElse(groupedPoint.templatePoint, null)
			}
		}
	}

}

trait AgentStudentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AgentStudentCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, student)
	}

}

trait AgentStudentCommandState {
	def relationshipType: StudentRelationshipType
	def academicYear: AcademicYear
	def student: StudentMember
}
