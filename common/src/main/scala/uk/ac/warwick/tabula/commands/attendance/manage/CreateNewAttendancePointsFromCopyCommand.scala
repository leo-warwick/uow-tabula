package uk.ac.warwick.tabula.commands.attendance.manage

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance._
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

  self: CreateNewAttendancePointsFromCopyCommandState with AttendanceMonitoringServiceComponent with ProfileServiceComponent with AttendanceMonitoringPointValidation =>

  override def applyInternal(): Seq[AttendanceMonitoringPoint] = {
    val points = getPoints(findPointsResult, schemes, pointStyle, academicYear, addToScheme = true, None)
    points.foreach(attendanceMonitoringService.saveOrUpdate)

    generateNotifications(schemes)
    updateCheckpointTotals(schemes)

    points
  }

}

trait CreateNewAttendancePointsFromCopyValidation extends SelfValidating with GetsPointsToCreate with AttendanceMonitoringPointValidation {

  self: CreateNewAttendancePointsFromCopyCommandState with AttendanceMonitoringServiceComponent =>

  override def validate(errors: Errors): Unit = {
    val points = getPoints(findPointsResult, schemes, pointStyle, academicYear, addToScheme = false, Some(errors))
    if (!errors.hasErrors) {
      points.foreach(point => {
        validateSchemePointStyles(errors, pointStyle, schemes)

        pointStyle match {
          case AttendanceMonitoringPointStyle.Date =>
            validateCanPointBeEditedByDate(errors, point.startDate, schemes.flatMap(_.members.members), academicYear, "")
            validateDuplicateForDate(errors, point.name, point.startDate, point.endDate, schemes, global = true)
            validateDate(errors, point.startDate, academicYear, "")
            validateDate(errors, point.endDate, academicYear, "")
          case AttendanceMonitoringPointStyle.Week =>
            validateCanPointBeEditedByWeek(errors, point.startWeek, schemes.flatMap(_.members.members), academicYear, "")
            validateDuplicateForWeek(errors, point.name, point.startWeek, point.endWeek, schemes, global = true)
        }
      })
    }
  }

}

trait GetsPointsToCreate {
  self: AttendanceMonitoringPointValidation =>

  def convertSpecificAssignmentPointToAnyAssignment(point: AttendanceMonitoringPoint): Unit = {
    // convert dedicated assignment to any assignment
    if (point.isSpecificAssignmentPoint) {
      point.assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Any

      //if any assignment specified just set the new quantity as 1 otherwise based on total assignments required previously
      if (point.assignmentSubmissionIsDisjunction) {
        point.assignmentSubmissionTypeAnyQuantity = 1
      } else {
        point.assignmentSubmissionTypeAnyQuantity = point.assignmentSubmissionAssignments.size
      }
      point.assignmentSubmissionAssignments = Seq()
    }
  }

  def getPoints(
    findPointsResult: FindPointsResult,
    schemes: Seq[AttendanceMonitoringScheme],
    pointStyle: AttendanceMonitoringPointStyle,
    academicYear: AcademicYear,
    addToScheme: Boolean = true,
    errors: Option[Errors]
  ): Seq[AttendanceMonitoringPoint] = {
    val weekPoints = findPointsResult.termGroupedPoints.flatMap(_._2).map(_.templatePoint).toSeq
    val datePoints = findPointsResult.monthGroupedPoints.flatMap(_._2).map(_.templatePoint).toSeq
    if (pointStyle == AttendanceMonitoringPointStyle.Week) {
      // Week points
      schemes.flatMap { scheme =>
        val weeksForYear = scheme.academicYear.weeks
        weekPoints.map { weekPoint =>
          val newPoint = if (addToScheme) weekPoint.cloneTo(Option(scheme)) else weekPoint.cloneTo(None)
          newPoint.createdDate = DateTime.now
          newPoint.updatedDate = DateTime.now

          // Fix new points date
          if (!weeksForYear.contains(weekPoint.startWeek)) {
            errors.foreach(_.rejectValue("", "attendanceMonitoringPoint.date.min"))
          } else {
            newPoint.startDate = weeksForYear(weekPoint.startWeek).firstDay
          }

          if (!weeksForYear.contains(weekPoint.endWeek)) {
            errors.foreach(_.rejectValue("", "attendanceMonitoringPoint.date.max"))
          } else {
            newPoint.endDate = weeksForYear(weekPoint.endWeek).lastDay
          }
          convertSpecificAssignmentPointToAnyAssignment(newPoint)
          newPoint
        }
      }
    } else {
      // Date points
      schemes.flatMap { scheme =>
        datePoints.map { datePoint =>
          val newPoint =
            if (addToScheme) datePoint.cloneTo(Option(scheme))
            else datePoint.cloneTo(None)

          newPoint.createdDate = DateTime.now
          newPoint.updatedDate = DateTime.now
          // Fix new points year
          val academicYearDifference = academicYear.startYear - datePoint.scheme.academicYear.startYear
          newPoint.startDate = newPoint.startDate.withYear(newPoint.startDate.getYear + academicYearDifference)
          newPoint.endDate = newPoint.endDate.withYear(newPoint.endDate.getYear + academicYearDifference)
          convertSpecificAssignmentPointToAnyAssignment(newPoint)
          newPoint
        }
      }
    }
  }
}

trait CreateNewAttendancePointsFromCopyPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: CreateNewAttendancePointsFromCopyCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.MonitoringPoints.Manage, department)
  }

}

trait CreateNewAttendancePointsFromCopyDescription extends Describable[Seq[AttendanceMonitoringPoint]] {

  self: CreateNewAttendancePointsFromCopyCommandState =>

  override lazy val eventName = "CreateNewAttendancePointsFromCopy"

  override def describe(d: Description): Unit = {
    d.attendanceMonitoringSchemes(schemes)
  }

  override def describeResult(d: Description, points: Seq[AttendanceMonitoringPoint]): Unit = {
    d.attendanceMonitoringPoints(points, verbose = true)
    val assignmentPoint = points.filter(_.isSpecificAssignmentPoint)
    if (assignmentPoint.nonEmpty) {
      d.property("attendanceMonitoringAssignmentPoint", assignmentPoint.map(point => Map(
        "id" -> point.id,
        "name" -> point.name
      )))
    }

  }
}

trait CreateNewAttendancePointsFromCopyCommandState extends FindPointsResultCommandState {
  def department: Department

  def academicYear: AcademicYear

  def schemes: Seq[AttendanceMonitoringScheme]

  lazy val pointStyle: AttendanceMonitoringPointStyle = schemes.head.pointStyle
}
