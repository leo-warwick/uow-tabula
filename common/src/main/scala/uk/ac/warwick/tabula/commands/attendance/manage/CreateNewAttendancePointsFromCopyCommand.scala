package uk.ac.warwick.tabula.commands.attendance.manage

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object CreateNewAttendancePointsFromCopyCommand {
	def apply(
		department: Department,
		academicYear: AcademicYear,
		schemes: Seq[AttendanceMonitoringScheme]
	) =
		new CreateNewAttendancePointsFromCopyCommandInternal(department, academicYear, schemes)
			with ComposableCommand[Seq[AttendanceMonitoringPoint]]
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringProfileServiceComponent
			with CreateNewAttendancePointsFromCopyValidation
			with CreateNewAttendancePointsFromCopyDescription
			with CreateNewAttendancePointsFromCopyPermissions
			with CreateNewAttendancePointsFromCopyCommandState
			with SetsFindPointsResultOnCommandState
}


class CreateNewAttendancePointsFromCopyCommandInternal(
	val department: Department,
	val academicYear: AcademicYear,
	val schemes: Seq[AttendanceMonitoringScheme]
) extends CommandInternal[Seq[AttendanceMonitoringPoint]] with GetsPointsToCreate with TaskBenchmarking
		with GeneratesAttendanceMonitoringSchemeNotifications with RequiresCheckpointTotalUpdate {

	self: CreateNewAttendancePointsFromCopyCommandState with TermServiceComponent
		with AttendanceMonitoringServiceComponent with ProfileServiceComponent =>

	override def applyInternal(): Seq[AttendanceMonitoringPoint] = {
		val points = getPoints(findPointsResult, schemes, pointStyle, academicYear, addToScheme = true)
		points.foreach(attendanceMonitoringService.saveOrUpdate)

		generateNotifications(schemes)
		updateCheckpointTotals(schemes)

		points
	}

}

trait CreateNewAttendancePointsFromCopyValidation extends SelfValidating with GetsPointsToCreate with AttendanceMonitoringPointValidation {

	self: CreateNewAttendancePointsFromCopyCommandState with TermServiceComponent with AttendanceMonitoringServiceComponent =>

	override def validate(errors: Errors) {
		val points = getPoints(findPointsResult, schemes, pointStyle, academicYear, addToScheme = false)
		points.foreach(point => {
			validateSchemePointStyles(errors, pointStyle, schemes)

			pointStyle match {
				case AttendanceMonitoringPointStyle.Date =>
					validateCanPointBeEditedByDate(errors, point.startDate, schemes.flatMap(_.members.members), academicYear, "")
					validateDuplicateForDate(errors, point.name, point.startDate, point.endDate, schemes, global = true)
				case AttendanceMonitoringPointStyle.Week =>
					validateCanPointBeEditedByWeek(errors, point.startWeek, schemes.flatMap(_.members.members), academicYear, "")
					validateDuplicateForWeek(errors, point.name, point.startWeek, point.endWeek, schemes, global = true)
			}
		})
	}

}

trait GetsPointsToCreate {

	self: TermServiceComponent =>

	def getPoints(
		findPointsResult: FindPointsResult,
		schemes: Seq[AttendanceMonitoringScheme],
		pointStyle: AttendanceMonitoringPointStyle,
		academicYear: AcademicYear,
		addToScheme: Boolean = true
	): Seq[AttendanceMonitoringPoint] = {
		val weekPoints = findPointsResult.termGroupedPoints.flatMap(_._2).map(_.templatePoint).toSeq
		val datePoints = findPointsResult.monthGroupedPoints.flatMap(_._2).map(_.templatePoint).toSeq
		if (pointStyle == AttendanceMonitoringPointStyle.Week) {
			// Week points
			schemes.flatMap { scheme =>
				val weeksForYear = termService.getAcademicWeeksForYear(scheme.academicYear.dateInTermOne).toMap
				weekPoints.map { weekPoint =>
					val newPoint = addToScheme match {
						case true => weekPoint.cloneTo(Option(scheme))
						case false => weekPoint.cloneTo(None)
					}
					newPoint.createdDate = DateTime.now
					newPoint.updatedDate = DateTime.now
					// Fix new points date
					newPoint.startDate = weeksForYear(weekPoint.startWeek).getStart.withDayOfWeek(DayOfWeek.Monday.jodaDayOfWeek).toLocalDate
					newPoint.endDate = weeksForYear(weekPoint.endWeek).getStart.withDayOfWeek(DayOfWeek.Monday.jodaDayOfWeek).toLocalDate.plusDays(6)
					newPoint
				}
			}
		} else {
			// Date points
			schemes.flatMap { scheme =>
				datePoints.map { datePoint =>
					val newPoint = addToScheme match {
						case true => datePoint.cloneTo(Option(scheme))
						case false => datePoint.cloneTo(None)
					}
					newPoint.createdDate = DateTime.now
					newPoint.updatedDate = DateTime.now
					// Fix new points year
					val academicYearDifference = academicYear.startYear - datePoint.scheme.academicYear.startYear
					newPoint.startDate = newPoint.startDate.withYear(newPoint.startDate.getYear + academicYearDifference)
					newPoint.endDate = newPoint.endDate.withYear(newPoint.endDate.getYear + academicYearDifference)
					newPoint
				}
			}
		}
	}
}

trait CreateNewAttendancePointsFromCopyPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: CreateNewAttendancePointsFromCopyCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, department)
	}

}

trait CreateNewAttendancePointsFromCopyDescription extends Describable[Seq[AttendanceMonitoringPoint]] {

	self: CreateNewAttendancePointsFromCopyCommandState =>

	override lazy val eventName = "CreateNewAttendancePointsFromCopy"

	override def describe(d: Description) {
		d.attendanceMonitoringSchemes(schemes)
	}
	override def describeResult(d: Description, points: Seq[AttendanceMonitoringPoint]) {
		d.attendanceMonitoringPoints(points, verbose = true)
	}
}

trait CreateNewAttendancePointsFromCopyCommandState extends FindPointsResultCommandState {
	def department: Department
	def academicYear: AcademicYear
	def schemes: Seq[AttendanceMonitoringScheme]
	lazy val pointStyle: AttendanceMonitoringPointStyle = schemes.head.pointStyle
}
