package uk.ac.warwick.tabula.commands.attendance.view

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.commands.attendance.GroupsPoints
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.services.{TermServiceComponent, AutowiringTermServiceComponent}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.AcademicYear

object ViewStudentAttendanceCommand {
	def apply(department: Department, academicYear: AcademicYear, student: StudentMember) =
		new ViewStudentAttendanceCommandInternal(department, academicYear, student)
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
			with ComposableCommand[Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]]]
			with ViewStudentAttendancePermissions
			with ViewStudentAttendanceCommandState
			with ReadOnly with Unaudited
}


class ViewStudentAttendanceCommandInternal(val department: Department, val academicYear: AcademicYear, val student: StudentMember)
	extends CommandInternal[Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]]]
		with GroupsPoints with TaskBenchmarking {

	self: AttendanceMonitoringServiceComponent with TermServiceComponent =>

	override def applyInternal(): Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]] = {
		val points = benchmarkTask("listStudentsPoints"){
			attendanceMonitoringService.listStudentsPoints(student, Option(department), academicYear)
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

trait ViewStudentAttendancePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ViewStudentAttendanceCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, student)
	}

}

trait ViewStudentAttendanceCommandState {
	def department: Department
	def academicYear: AcademicYear
	def student: StudentMember
}
