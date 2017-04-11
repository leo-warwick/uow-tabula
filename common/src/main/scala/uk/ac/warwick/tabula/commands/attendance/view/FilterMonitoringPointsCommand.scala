package uk.ac.warwick.tabula.commands.attendance.view

import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.{CurrentUser, AcademicYear}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent, TermServiceComponent, AutowiringTermServiceComponent}
import uk.ac.warwick.tabula.commands.{TaskBenchmarking, CommandInternal, Unaudited, ReadOnly, ComposableCommand}
import uk.ac.warwick.tabula.commands.attendance.{GroupsPoints, GroupedPoint}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.hibernate.criterion.Order._
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.JavaImports._

case class FilterMonitoringPointsCommandResult(
	studentDatas: Seq[AttendanceMonitoringStudentData],
	pointMap: Map[String, Seq[GroupedPoint]]
)

object FilterMonitoringPointsCommand {
	def apply(department: Department, academicYear: AcademicYear, user: CurrentUser) =
		new FilterMonitoringPointsCommandInternal(department, academicYear, user)
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[FilterMonitoringPointsCommandResult]
			with FilterMonitoringPointsPermissions
			with FilterMonitoringPointsCommandState
			with OnBindFilterMonitoringPointsCommand
			with ReadOnly with Unaudited
}

class FilterMonitoringPointsCommandInternal(val department: Department, val academicYear: AcademicYear, val user: CurrentUser)
	extends CommandInternal[FilterMonitoringPointsCommandResult] with GroupsPoints with TaskBenchmarking {

	self: ProfileServiceComponent with FilterMonitoringPointsCommandState with AttendanceMonitoringServiceComponent with TermServiceComponent =>

	override def applyInternal(): FilterMonitoringPointsCommandResult = {
		if (serializeFilter.isEmpty) {
			filterTooVague = true
			FilterMonitoringPointsCommandResult(Seq(), Map())
		} else {
			val studentDatas = benchmarkTask("profileService.findAllStudentDataByRestrictionsInAffiliatedDepartments") {
				profileService.findAllStudentDataByRestrictionsInAffiliatedDepartments(
					department = department,
					restrictions = buildRestrictions(academicYear),
					academicYear = academicYear
				)
			}

			if (studentDatas.size > MaxStudentsFromFilter) {
				filterTooVague = true
				FilterMonitoringPointsCommandResult(Seq(), Map())
			} else {
				val points = benchmarkTask("List all students points") {
					studentDatas.flatMap { studentData =>
						attendanceMonitoringService.listStudentsPoints(studentData, department, academicYear)
					}.distinct
				}
				FilterMonitoringPointsCommandResult(
					studentDatas,
					groupByMonth(points, groupSimilar = true) ++ groupByTerm(points, groupSimilar = true)
				)
			}
		}
	}
}

trait OnBindFilterMonitoringPointsCommand extends BindListener {

	self: FilterMonitoringPointsCommandState =>

	override def onBind(result: BindingResult): Unit = {
		if (!hasBeenFiltered) {
			allSprStatuses.filter { status => !status.code.startsWith("P") && !status.code.startsWith("T") }.foreach { sprStatuses.add }
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

	val defaultOrder = Seq(asc("lastName"), asc("firstName"))
	var sortOrder: JList[Order] = null // No sorting in this command

	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()

	var filterTooVague = false
	var hasBeenFiltered = false

}
