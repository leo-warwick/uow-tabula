package uk.ac.warwick.tabula.attendance.commands.view

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{CurrentUser, AcademicYear}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent, TermServiceComponent, AttendanceMonitoringServiceComponent, AutowiringTermServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.commands.{TaskBenchmarking, CommandInternal, Unaudited, ReadOnly, ComposableCommand}
import uk.ac.warwick.tabula.attendance.commands.{GroupsPoints, GroupedPoint}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.hibernate.criterion.Order._
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.JavaImports._

object FilterMonitoringPointsCommand {
	def apply(department: Department, academicYear: AcademicYear, user: CurrentUser) =
		new FilterMonitoringPointsCommandInternal(department, academicYear, user)
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[Map[String, Seq[GroupedPoint]]]
			with FilterMonitoringPointsPermissions
			with FilterMonitoringPointsCommandState
			with ReadOnly with Unaudited
}

class FilterMonitoringPointsCommandInternal(val department: Department, val academicYear: AcademicYear, val user: CurrentUser)
	extends CommandInternal[Map[String, Seq[GroupedPoint]]] with GroupsPoints with TaskBenchmarking {

	self: ProfileServiceComponent with FilterMonitoringPointsCommandState with AttendanceMonitoringServiceComponent with TermServiceComponent =>

	override def applyInternal() = {
		if (serializeFilter.isEmpty) {
			Map()
		} else {
			val studentDatas = benchmarkTask("profileService.findAllStudentDataByRestrictionsInAffiliatedDepartments") {
				profileService.findAllStudentDataByRestrictionsInAffiliatedDepartments(
					department = department,
					restrictions = buildRestrictions()
				)
			}

			if (studentDatas.size > MaxStudentsFromFilter) {
				filterTooVague = true
				Map()
			} else {
				val points = benchmarkTask("List all students points") {
					studentDatas.flatMap { studentData =>
						attendanceMonitoringService.listStudentsPoints(studentData, department, academicYear)
					}.distinct
				}
				groupByMonth(points, groupSimilar = true) ++ groupByTerm(points, groupSimilar = true)
			}
		}
	}
}

trait FilterMonitoringPointsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: FilterMonitoringPointsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, department)
	}

}

trait FilterMonitoringPointsCommandState extends AttendanceFilterExtras {

	final val MaxStudentsFromFilter = 600

	def department: Department
	def academicYear: AcademicYear

	val defaultOrder = Seq(asc("lastName"), asc("firstName"))
	var sortOrder: JList[Order] = null // No sorting in this command

	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()

	var filterTooVague = false

}

