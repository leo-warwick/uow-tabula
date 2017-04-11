package uk.ac.warwick.tabula.commands.attendance.manage

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringPointStyle, AttendanceMonitoringPointType, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object CreateAttendancePointCommand {
	def apply(department: Department, academicYear: AcademicYear, schemes: Seq[AttendanceMonitoringScheme]) =
		new CreateAttendancePointCommandInternal(department, academicYear, schemes)
			with ComposableCommand[Seq[AttendanceMonitoringPoint]]
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringProfileServiceComponent
			with AutowiringSmallGroupServiceComponent
			with AutowiringModuleAndDepartmentServiceComponent
			with CreateAttendancePointValidation
			with CreateAttendancePointDescription
			with CreateAttendancePointPermissions
			with CreateAttendancePointCommandState
}


class CreateAttendancePointCommandInternal(val department: Department, val academicYear: AcademicYear, val schemes: Seq[AttendanceMonitoringScheme])
	extends CommandInternal[Seq[AttendanceMonitoringPoint]] with TaskBenchmarking
		with GeneratesAttendanceMonitoringSchemeNotifications with RequiresCheckpointTotalUpdate {

	self: CreateAttendancePointCommandState with AttendanceMonitoringServiceComponent
		with TermServiceComponent with ProfileServiceComponent =>

	override def applyInternal(): Seq[AttendanceMonitoringPoint] = {
		val points = schemes.map(scheme => {
			val point = new AttendanceMonitoringPoint
			point.scheme = scheme
			point.createdDate = DateTime.now
			point.updatedDate = DateTime.now
			copyTo(point)
			attendanceMonitoringService.saveOrUpdate(point)
			point
		})

		generateNotifications(schemes)
		updateCheckpointTotals(schemes)

		points
	}

}

trait CreateAttendancePointValidation extends SelfValidating with AttendanceMonitoringPointValidation {

	self: CreateAttendancePointCommandState with TermServiceComponent with AttendanceMonitoringServiceComponent =>

	override def validate(errors: Errors) {
		validateSchemePointStyles(errors, pointStyle, schemes)

		validateName(errors, name)

		pointStyle match {
			case AttendanceMonitoringPointStyle.Date =>
				validateDate(errors, startDate, academicYear, "startDate")
				validateDate(errors, endDate, academicYear, "endDate")
				if (startDate != null && endDate != null) {
					validateDates(errors, startDate, endDate)
					validateCanPointBeEditedByDate(errors, startDate, schemes.flatMap(_.members.members), academicYear)
					validateDuplicateForDate(errors, name, startDate, endDate, schemes)
				}
			case AttendanceMonitoringPointStyle.Week =>
				validateWeek(errors, startWeek, "startWeek")
				validateWeek(errors, endWeek, "endWeek")
				validateWeeks(errors, startWeek, endWeek)
				validateCanPointBeEditedByWeek(errors, startWeek, schemes.flatMap(_.members.members), academicYear)
				validateDuplicateForWeek(errors, name, startWeek, endWeek, schemes)
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
					validateOverlapMeeting(
						errors,
						startDate,
						endDate,
						meetingRelationships.asScala,
						meetingFormats.asScala,
						schemes
					)
				case AttendanceMonitoringPointType.SmallGroup =>
					validateTypeSmallGroup(
						errors,
						smallGroupEventModules,
						isAnySmallGroupEventModules,
						smallGroupEventQuantity
					)
					validateOverlapSmallGroup(
						errors,
						startDate,
						endDate,
						smallGroupEventModules,
						isAnySmallGroupEventModules,
						schemes
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
					validateOverlapAssignment(
						errors,
						startDate,
						endDate,
						assignmentSubmissionType,
						assignmentSubmissionModules,
						assignmentSubmissionAssignments,
						isAssignmentSubmissionDisjunction,
						schemes
					)
				case _ =>
			}
		}
	}

}

trait CreateAttendancePointPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: CreateAttendancePointCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, department)
	}

}

trait CreateAttendancePointDescription extends Describable[Seq[AttendanceMonitoringPoint]] {

	self: CreateAttendancePointCommandState =>

	override lazy val eventName = "CreateAttendancePoint"

	override def describe(d: Description) {
		d.attendanceMonitoringSchemes(schemes)
	}
	override def describeResult(d: Description, points: Seq[AttendanceMonitoringPoint]) {
		d.attendanceMonitoringPoints(points, verbose = true)
	}
}

trait CreateAttendancePointCommandState extends AttendancePointCommandState {

	self: TermServiceComponent with SmallGroupServiceComponent with ModuleAndDepartmentServiceComponent =>

	def department: Department
	def academicYear: AcademicYear
	def schemes: Seq[AttendanceMonitoringScheme]
	lazy val pointStyle: AttendanceMonitoringPointStyle = schemes.head.pointStyle
}
