package uk.ac.warwick.tabula.commands.attendance.manage

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringPointStyle, AttendanceMonitoringPointType, AttendanceMonitoringScheme, AttendanceMonitoringTemplate}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._


object AddTemplatePointsToSchemesCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new AddTemplatePointsToSchemesCommandInternal(department, academicYear)
		with ComposableCommand[Seq[AttendanceMonitoringPoint]]
		with AddTemplatePointsToSchemesCommandState
		with AddTemplatePointsToSchemesPermissions
		with AutowiringAttendanceMonitoringServiceComponent
		with AutowiringProfileServiceComponent
		with AutowiringTermServiceComponent
		with AddTemplatePointsToSchemesDescription
		with AddTemplatePointsToSchemesValidation
}

class AddTemplatePointsToSchemesCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[Seq[AttendanceMonitoringPoint]] with TaskBenchmarking
		with GeneratesAttendanceMonitoringSchemeNotifications with RequiresCheckpointTotalUpdate {

	self: AddTemplatePointsToSchemesCommandState with AttendanceMonitoringServiceComponent
		with ProfileServiceComponent with TermServiceComponent =>

	override def applyInternal(): Seq[AttendanceMonitoringPoint] = {

		val attendanceMonitoringPoints = attendanceMonitoringService.generatePointsFromTemplateScheme(templateScheme, academicYear)

		val newPoints = schemes.asScala.flatMap { scheme =>
			attendanceMonitoringPoints.map { point =>
				val newPoint = point.cloneTo(Option(scheme))
				newPoint.pointType = AttendanceMonitoringPointType.Standard
				newPoint.createdDate = new DateTime()
				newPoint.updatedDate = new DateTime()
				attendanceMonitoringService.saveOrUpdate(newPoint)
				newPoint
			}
		}

		generateNotifications(schemes.asScala)
		updateCheckpointTotals(schemes.asScala)

		newPoints
	}
}

trait AddTemplatePointsToSchemesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AddTemplatePointsToSchemesCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, department)
	}
}

trait AddTemplatePointsToSchemesCommandState {
	self: AttendanceMonitoringServiceComponent =>

	def department: Department
	def academicYear: AcademicYear

	var schemes: JList[AttendanceMonitoringScheme] = new JArrayList()
	var templateScheme: AttendanceMonitoringTemplate = _

	def templateSchemeItems(): Seq[AttendanceMonitoringTemplate] = attendanceMonitoringService.listTemplateSchemesByStyle(schemes.get(0).pointStyle)
}

trait AddTemplatePointsToSchemesDescription extends Describable[Seq[AttendanceMonitoringPoint]] {
	self: AddTemplatePointsToSchemesCommandState =>

	override lazy val eventName = "AddTemplatePointsToScheme"
	override def describe(d: Description) {
		schemes.asScala.foreach(d.attendanceMonitoringScheme)
	}
	override def describeResult(d: Description, points: Seq[AttendanceMonitoringPoint]) {
		d.attendanceMonitoringPoints(points, verbose = true)
	}
}


trait AddTemplatePointsToSchemesValidation extends AttendanceMonitoringPointValidation with SelfValidating {
	self: AddTemplatePointsToSchemesCommandState with AttendanceMonitoringServiceComponent with TermServiceComponent =>

	override def validate(errors: Errors) {
		if (templateScheme == null) {
			errors.reject("attendanceMonitoringPoints.templateScheme.Empty")
		} else {
			validateSchemePointStyles(errors, templateScheme.pointStyle, schemes.asScala)

			attendanceMonitoringService.generatePointsFromTemplateScheme(templateScheme, academicYear).foreach{ point =>
				templateScheme.pointStyle match {
					case AttendanceMonitoringPointStyle.Date =>
						validateDuplicateForDate(errors, point.name, point.startDate, point.endDate, schemes.asScala, global = true)
					case AttendanceMonitoringPointStyle.Week =>
						validateDuplicateForWeek(errors, point.name, point.startWeek, point.endWeek, schemes.asScala, global = true)
				}
			}

		}
	}

}
