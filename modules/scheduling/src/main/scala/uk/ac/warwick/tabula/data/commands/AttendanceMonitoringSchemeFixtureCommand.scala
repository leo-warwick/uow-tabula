package uk.ac.warwick.tabula.data.commands

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringPointStyle, AttendanceMonitoringPointType, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

object AttendanceMonitoringSchemeFixtureCommand {
	def apply() =
		new AttendanceMonitoringSchemeFixtureCommand()
			with ComposableCommand[AttendanceMonitoringScheme]
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringTransactionalComponent
			with PubliclyVisiblePermissions
			with Unaudited

}

class AttendanceMonitoringSchemeFixtureCommand extends CommandInternal[AttendanceMonitoringScheme] {
	self: AttendanceMonitoringServiceComponent with ModuleAndDepartmentServiceComponent with TransactionalComponent =>

	var deptCode: String = _
	var academicYear: AcademicYear = _
	var pointCount: Int = _
	var warwickId: String = _

	def applyInternal() = transactional() {

		val department = moduleAndDepartmentService.getDepartmentByCode(deptCode).getOrElse(throw new IllegalArgumentException)

		for (scheme <- attendanceMonitoringService.listAllSchemes(department)) {
			for (point <- scheme.points){
				for (checkpoint <- attendanceMonitoringService.getAllCheckpoints(point)){
					attendanceMonitoringService.deleteCheckpoint(checkpoint)
				}
			}
			// the points will also be deleted by the cascade
			attendanceMonitoringService.deleteScheme(scheme)
		}

		val scheme = new AttendanceMonitoringScheme
		scheme.academicYear = academicYear
		scheme.department = department
		scheme.createdDate = DateTime.now
		scheme.updatedDate = DateTime.now
		scheme.pointStyle = AttendanceMonitoringPointStyle.Week
		scheme.members = UserGroup.ofUniversityIds
		scheme.members.addUserId(warwickId)
		scheme.members.staticUserIds = Seq(warwickId)

		scheme.points = {
			for (count <- 0 until pointCount) yield {
				val point = new AttendanceMonitoringPoint
				point.name = s"Point ${count+1}"
				point.createdDate = DateTime.now
				point.updatedDate = DateTime.now
				point.scheme = scheme
				point.pointType = AttendanceMonitoringPointType.Meeting
				point.startDate = new LocalDate()
				point.endDate = new LocalDate().plusWeeks(2)
				point.startWeek = count + 1
				point.endWeek = count + 1
				point
			}
		}.asJava

		attendanceMonitoringService.saveOrUpdate(scheme)

		scheme

	}

}
