package uk.ac.warwick.tabula.commands.attendance.profile

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.attendance.GroupsPoints
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringNote, AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

object AttendanceProfileCommand {
	def apply(student: StudentMember, academicYear: AcademicYear) =
		new AttendanceProfileCommandInternal(student, academicYear)
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
			with ComposableCommand[AttendanceProfileCommandResult]
			with AttendanceProfilePermissions
			with AttendanceProfileCommandState
			with ReadOnly with Unaudited
}

case class AttendanceProfileCommandResult(
	groupedPointMap: Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]],
	notes: Seq[AttendanceMonitoringNote],
	noteCheckpoints: Map[AttendanceMonitoringNote, Option[AttendanceMonitoringCheckpoint]],
	missedPointCountByTerm: Map[String, Int],
	hasAnyMissedPoints: Boolean
)

class AttendanceProfileCommandInternal(val student: StudentMember, val academicYear: AcademicYear)
	extends CommandInternal[AttendanceProfileCommandResult]
		with GroupsPoints with TaskBenchmarking {

	self: AttendanceMonitoringServiceComponent with TermServiceComponent =>

	override def applyInternal(): AttendanceProfileCommandResult = {
		val points = benchmarkTask("listStudentsPoints") {
			attendanceMonitoringService.listStudentsPoints(student, None, academicYear)
		}
		val checkpointMap = attendanceMonitoringService.getCheckpoints(points, student)
		val groupedPoints = groupByTerm(points, groupSimilar = false) ++ groupByMonth(points, groupSimilar = false)
		val groupedPointMap = groupedPoints.map { case (period, thesePoints) =>
			period -> thesePoints.map { groupedPoint =>
				groupedPoint.templatePoint -> checkpointMap.getOrElse(groupedPoint.templatePoint, null)
			}
		}

		val notes = attendanceMonitoringService.getAttendanceNoteMap(student).filterKeys(points.contains).values.toSeq.sortBy(_.updatedDate)

		val noteCheckpoints = notes.map(note => note -> checkpointMap.get(note.point)).toMap

		val missedPointCountByTerm = groupedPointMap.map { case (period, pointCheckpointPairs) =>
			period -> pointCheckpointPairs.count { case (_, checkpoint) => Option(checkpoint).exists(_.state == AttendanceState.MissedUnauthorised) }
		}

		val hasAnyMissedPoints = missedPointCountByTerm.exists { case(_, pointCount) => pointCount > 0 }

		AttendanceProfileCommandResult(
			groupedPointMap,
			notes,
			noteCheckpoints,
			missedPointCountByTerm,
			hasAnyMissedPoints
		)
	}
}

trait AttendanceProfilePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AttendanceProfileCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, student)
	}

}

trait AttendanceProfileCommandState {
	def academicYear: AcademicYear
	def student: StudentMember
}
