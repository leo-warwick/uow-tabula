package uk.ac.warwick.tabula.commands.attendance.manage

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.{ItemNotFoundException, AcademicYear}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringPointStyle, AttendanceMonitoringPointType, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object EditAttendancePointCommand {
	def apply(department: Department, academicYear: AcademicYear, templatePoint: AttendanceMonitoringPoint) =
		new EditAttendancePointCommandInternal(department, academicYear, templatePoint)
			with ComposableCommand[Seq[AttendanceMonitoringPoint]]
			with PopulatesEditAttendancePointCommand
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
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

	self: EditAttendancePointCommandState with AttendanceMonitoringServiceComponent
		with TermServiceComponent with ProfileServiceComponent =>

	override def applyInternal(): Seq[AttendanceMonitoringPoint] = {
		val editedPoints = pointsToEdit.map(point => {
			copyTo(point)
			point.updatedDate = DateTime.now
			attendanceMonitoringService.saveOrUpdate(point)
			point
		})

		generateNotifications(schemesToEdit)
		updateCheckpointTotals(schemesToEdit)

		editedPoints
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

	self: EditAttendancePointCommandState with TermServiceComponent with AttendanceMonitoringServiceComponent =>

	override def validate(errors: Errors) {
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
					validateCanPointBeEditedByDate(errors, startDate, schemes.map{_.members.members}.flatten, academicYear)
					points.exists(p => validateDuplicateForDateForEdit(errors, name, startDate, endDate, p))
				}
			case AttendanceMonitoringPointStyle.Week =>
				validateWeek(errors, startWeek, "startWeek")
				validateWeek(errors, endWeek, "endWeek")
				validateWeeks(errors, startWeek, endWeek)
				validateCanPointBeEditedByWeek(errors, startWeek, schemes.map{_.members.members}.flatten, academicYear)
				points.exists(p => validateDuplicateForWeekForEdit(errors, name, startWeek, endWeek, p))
		}

		if (!errors.hasErrors) {
			if (pointStyle == AttendanceMonitoringPointStyle.Week) {
				val weeksForYear = termService.getAcademicWeeksForYear(schemes.head.academicYear.dateInTermOne).toMap
				startDate = weeksForYear(startWeek).getStart.withDayOfWeek(DayOfWeek.Monday.jodaDayOfWeek).toLocalDate
				endDate = weeksForYear(endWeek).getStart.withDayOfWeek(DayOfWeek.Monday.jodaDayOfWeek).toLocalDate.plusDays(6)
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
						assignmentSubmissionAssignments
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

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, department)
	}

}

trait EditAttendancePointDescription extends Describable[Seq[AttendanceMonitoringPoint]] {

	self: EditAttendancePointCommandState =>

	override lazy val eventName = "EditAttendancePoint"

	override def describe(d: Description) {
		d.attendanceMonitoringSchemes(schemesToEdit)
	}
	override def describeResult(d: Description, points: Seq[AttendanceMonitoringPoint]) {
		d.attendanceMonitoringPoints(points, verbose = true)
	}
}

trait EditAttendancePointCommandState extends AttendancePointCommandState with FindPointsResultCommandState {

	self: TermServiceComponent with SmallGroupServiceComponent with ModuleAndDepartmentServiceComponent =>

	def department: Department
	def academicYear: AcademicYear
	def templatePoint: AttendanceMonitoringPoint
	lazy val pointStyle: AttendanceMonitoringPointStyle = templatePoint.scheme.pointStyle

	def pointsToEdit: Seq[AttendanceMonitoringPoint] = (pointStyle match {
		case AttendanceMonitoringPointStyle.Week => findPointsResult.termGroupedPoints
		case AttendanceMonitoringPointStyle.Date => findPointsResult.monthGroupedPoints
	}).flatMap(_._2)
		.find(p => p.templatePoint.id == templatePoint.id)
		.map { _.points }
		.getOrElse(Nil)

	def schemesToEdit: Seq[AttendanceMonitoringScheme] = pointsToEdit.map(_.scheme)
}
