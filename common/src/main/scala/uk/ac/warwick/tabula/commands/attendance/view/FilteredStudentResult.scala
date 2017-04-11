package uk.ac.warwick.tabula.commands.attendance.view

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.GroupsPoints
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringCheckpointTotal, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.data.model.{AttendanceNote, Department, StudentMember}
import uk.ac.warwick.tabula.services.TermServiceComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringServiceComponent

case class FilteredStudentResult(
	student: StudentMember,
	groupedPointCheckpointPairs: Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]],
	attendanceNotes: Map[AttendanceMonitoringPoint, AttendanceNote],
	checkpointTotal: AttendanceMonitoringCheckpointTotal
)

case class FilteredStudentsAttendanceResult(
	totalResults: Int,
	results: Seq[FilteredStudentResult],
	students: Seq[StudentMember]
)

trait BuildsFilteredStudentsAttendanceResult extends TaskBenchmarking with GroupsPoints {

	self: AttendanceMonitoringServiceComponent with TermServiceComponent =>

	def buildAttendanceResult(
		totalResults: Int,
		students: Seq[StudentMember],
		departmentOption: Option[Department],
		academicYear: AcademicYear,
		pointMap: Map[StudentMember, Seq[AttendanceMonitoringPoint]] = Map()
	): FilteredStudentsAttendanceResult = {
		val results = benchmarkTask("Build FilteredStudentResults"){ students.map { student =>
			val points = pointMap.getOrElse(student, benchmarkTask("listStudentsPoints") {
				attendanceMonitoringService.listStudentsPoints(student, departmentOption, academicYear)
			})
			val checkpointMap = benchmarkTask("getCheckpoints") {
				attendanceMonitoringService.getCheckpoints(points, student)
			}
			val nonActiveCheckpoints = benchmarkTask("nonActiveCheckpoints") {
				attendanceMonitoringService.getNonActiveCheckpoints(student, departmentOption, academicYear, checkpointMap.values.toSeq)
			}
			val allPoints = points ++ nonActiveCheckpoints.map{_.point}
			val allCheckpointMap = checkpointMap ++ nonActiveCheckpoints.map(c => c.point -> c)
			val groupedPoints = benchmarkTask("groupedPoints") {
				groupByTerm(allPoints, groupSimilar = false) ++ groupByMonth(allPoints, groupSimilar = false)
			}
			val groupedPointCheckpointPairs = benchmarkTask("groupedPointCheckpointPairs") {
				groupedPoints.map { case (period, thesePoints) =>
					period -> thesePoints.map { groupedPoint =>
						groupedPoint.templatePoint -> allCheckpointMap.getOrElse(groupedPoint.templatePoint, null)
					}
				}
			}
			val attendanceNotes = benchmarkTask("attendanceNotes") {
				attendanceMonitoringService.getAttendanceNoteMap(student)
			}
			val checkpointTotal = benchmarkTask("checkpointTotal") {
				attendanceMonitoringService.getCheckpointTotal(student, departmentOption, academicYear)
			}
			FilteredStudentResult(student, groupedPointCheckpointPairs, attendanceNotes, checkpointTotal)
		}}
		FilteredStudentsAttendanceResult(totalResults, benchmarkTask("spacePoints"){spacePoints(results)}, students)
	}

	private def spacePoints(results: Seq[FilteredStudentResult]): Seq[FilteredStudentResult] = {
		// do not remove; import needed for sorting
		// should be: import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

		val allPointsByPeriod = results.flatMap(_.groupedPointCheckpointPairs.toSeq).groupBy(_._1).mapValues(_.flatMap(_._2))

		// All the distinct dates in a given period
		val requiredDatesByPeriod = allPointsByPeriod.map {
			case (period, pointCheckpointPairs) => period -> pointCheckpointPairs.map(_._1.startDate).distinct.sorted
		}

		// For each period, for each distinct date, the maximum number of points
		val maxPointsPerDatePerPeriod = results.flatMap(fsr => {
			// For each student get the number of points per date
			fsr.groupedPointCheckpointPairs.map { case (period, pairs) =>
				period -> pairs.groupBy(_._1.startDate).mapValues(_.size)
			}.toSeq
		}).groupBy(_._1).map{ case(period, maxPointsPerDatePerPeriodPerUser) => period -> {
			// For all the date-count pairs, get the max for each date
			maxPointsPerDatePerPeriodPerUser.flatMap(_._2.toSeq).groupBy(_._1).mapValues(_.map(_._2).max)
		}}

		results.map{fsr =>
			FilteredStudentResult(
				fsr.student,
				fsr.groupedPointCheckpointPairs.map{ case (period, pointCheckpointPairs) => period -> {
					requiredDatesByPeriod(period).flatMap(requiredDate => {
						val pointsForDate = pointCheckpointPairs.filter(_._1.startDate == requiredDate)
						// Return all the student's points for this date, followed by enough nulls to pad out to the max for that date
						pointsForDate ++ (pointsForDate.size until maxPointsPerDatePerPeriod(period)(requiredDate)).map(i => null)
					})
				}},
				fsr.attendanceNotes,
				fsr.checkpointTotal
			)
		}
	}
}