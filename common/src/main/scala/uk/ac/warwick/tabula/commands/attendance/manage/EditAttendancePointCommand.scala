package uk.ac.warwick.tabula.commands.attendance.manage

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringPointStyle, AttendanceMonitoringPointType, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}

import scala.jdk.CollectionConverters._

object EditAttendancePointCommand {
  def apply(department: Department, academicYear: AcademicYear, templatePoint: AttendanceMonitoringPoint) =
    new EditAttendancePointCommandInternal(department, academicYear, templatePoint)
      with ComposableCommand[Seq[AttendanceMonitoringPoint]]
      with PopulatesEditAttendancePointCommand
      with AutowiringAttendanceMonitoringServiceComponent
      with AutowiringSmallGroupServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringProfileServiceComponent
      with EditAttendancePointValidation
      with EditAttendancePointDescription
      with EditAttendancePointPermissions
      with EditAttendancePointCommandState
      with SetsFindPointsResultOnCommandState
}


class EditAttendancePointCommandInternal(
  val department: Department,
  val academicYear: AcademicYear,
  val templatePoint: AttendanceMonitoringPoint
) extends CommandInternal[Seq[AttendanceMonitoringPoint]] with GeneratesAttendanceMonitoringSchemeNotifications with RequiresCheckpointTotalUpdate {

  self: EditAttendancePointCommandState with AttendanceMonitoringServiceComponent with ProfileServiceComponent =>

  override def applyInternal(): Seq[AttendanceMonitoringPoint] = {
    val editedPoints = pointsToEdit.flatMap(point => {
      val clonedBefore = point.cloneTo(None)
      copyTo(point)
      val clonedAfter = point.cloneTo(None)

      point.updatedDate = DateTime.now
      attendanceMonitoringService.saveOrUpdate(point)

      if (!autoRecordConditionsIdentical(clonedBefore, clonedAfter)) {
        Some(point)
      } else {
        None
      }
    })

    generateNotifications(schemesToEdit)
    updateCheckpointTotals(schemesToEdit)

    editedPoints
  }

  private def autoRecordConditionsIdentical(a: AttendanceMonitoringPoint, b: AttendanceMonitoringPoint) = {
    a.startDate == b.startDate && a.endDate == b.endDate && a.pointType == b.pointType && a.settingsIterator.sameElements(b.settingsIterator)
  }
}

trait PopulatesEditAttendancePointCommand extends PopulateOnForm {

  self: EditAttendancePointCommandState =>

  override def populate(): Unit = {
    copyFrom(templatePoint)

    if (pointsToEdit.isEmpty) throw new ItemNotFoundException
  }
}

trait EditAttendancePointValidation extends SelfValidating with AttendanceMonitoringPointValidation {

  self: EditAttendancePointCommandState with AttendanceMonitoringServiceComponent =>

  override def validate(errors: Errors): Unit = {
    val points = pointsToEdit
    val schemes = schemesToEdit
    validateSchemePointStyles(errors, pointStyle, schemes)

    validateName(errors, name)

    pointStyle match {
      case AttendanceMonitoringPointStyle.Date =>
        validateDate(errors, startDate, academicYear, "startDate")
        validateDate(errors, endDate, academicYear, "endDate")
        if (startDate != null && endDate != null) {
          validateDates(errors, startDate, endDate)
          validateCanPointBeEditedByDate(errors, startDate, schemes.flatMap(_.members.members), academicYear)
          points.exists(p => validateDuplicateForDateForEdit(errors, name, startDate, endDate, p))
        }
      case AttendanceMonitoringPointStyle.Week =>
        validateWeek(errors, startWeek, academicYear, "startWeek")
        validateWeek(errors, endWeek, academicYear, "endWeek")
        validateWeeks(errors, startWeek, endWeek)
        validateCanPointBeEditedByWeek(errors, startWeek, schemes.flatMap(_.members.members), academicYear)
        points.exists(p => validateDuplicateForWeekForEdit(errors, name, startWeek, endWeek, p))
    }

    if (!errors.hasErrors) {
      if (pointStyle == AttendanceMonitoringPointStyle.Week) {
        val weeksForYear = schemes.head.academicYear.weeks
        startDate = weeksForYear(startWeek).firstDay
        endDate = weeksForYear(endWeek).lastDay
      }
      validateTypeForEndDate(errors, pointType, endDate)
      pointType match {
        case AttendanceMonitoringPointType.Meeting =>
          validateTypeMeeting(
            errors,
            meetingRelationships.asScala,
            meetingFormats.asScala,
            meetingQuantity,
            department
          )
          points.exists(p =>
            validateOverlapMeetingForEdit(
              errors,
              startDate,
              endDate,
              meetingRelationships.asScala,
              meetingFormats.asScala,
              p
            )
          )
        case AttendanceMonitoringPointType.SmallGroup =>
          validateTypeSmallGroup(
            errors,
            smallGroupEventModules,
            isAnySmallGroupEventModules,
            smallGroupEventQuantity
          )
          points.exists(p =>
            validateOverlapSmallGroupForEdit(
              errors,
              startDate,
              endDate,
              smallGroupEventModules,
              isAnySmallGroupEventModules,
              p
            )
          )
        case AttendanceMonitoringPointType.AssignmentSubmission =>
          validateTypeAssignmentSubmission(
            errors,
            assignmentSubmissionType,
            assignmentSubmissionTypeAnyQuantity,
            assignmentSubmissionTypeModulesQuantity,
            assignmentSubmissionModules,
            assignmentSubmissionAssignments,
            academicYear
          )
          points.exists(p =>
            validateOverlapAssignmentForEdit(
              errors,
              startDate,
              endDate,
              assignmentSubmissionType,
              assignmentSubmissionModules,
              assignmentSubmissionAssignments,
              isAssignmentSubmissionDisjunction,
              p
            )
          )
        case _ =>
      }
    }
  }

}

trait EditAttendancePointPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: EditAttendancePointCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.MonitoringPoints.Manage, department)
  }

}

trait EditAttendancePointDescription extends Describable[Seq[AttendanceMonitoringPoint]] {

  self: EditAttendancePointCommandState =>

  override lazy val eventName = "EditAttendancePoint"

  override def describe(d: Description): Unit = {
    d.attendanceMonitoringSchemes(schemesToEdit)
  }

  override def describeResult(d: Description, points: Seq[AttendanceMonitoringPoint]): Unit = {
    d.attendanceMonitoringPoints(points, verbose = true)
  }
}

trait EditAttendancePointCommandState extends AttendancePointCommandState with FindPointsResultCommandState {

  self: SmallGroupServiceComponent with ModuleAndDepartmentServiceComponent =>

  def department: Department

  def academicYear: AcademicYear

  def templatePoint: AttendanceMonitoringPoint

  lazy val pointStyle: AttendanceMonitoringPointStyle = templatePoint.scheme.pointStyle

  def pointsToEdit: Seq[AttendanceMonitoringPoint] = (pointStyle match {
    case AttendanceMonitoringPointStyle.Week => findPointsResult.termGroupedPoints
    case AttendanceMonitoringPointStyle.Date => findPointsResult.monthGroupedPoints
  }).flatMap(_._2)
    .find(p => p.templatePoint.id == templatePoint.id)
    .map(_.points)
    .getOrElse(Nil)

  def schemesToEdit: Seq[AttendanceMonitoringScheme] = pointsToEdit.map(_.scheme)
}
