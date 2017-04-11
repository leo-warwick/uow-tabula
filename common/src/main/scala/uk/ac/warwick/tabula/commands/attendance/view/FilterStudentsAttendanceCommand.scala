package uk.ac.warwick.tabula.commands.attendance.view

import org.hibernate.criterion.Order._
import org.hibernate.criterion.{Order, Restrictions}
import org.hibernate.sql.JoinType
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{AutowiringSecurityServicePermissionsAwareRoutes, _}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AliasAndJoinType, ScalaRestriction}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringTermServiceComponent, ProfileServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._
import scala.collection.mutable

object FilterStudentsAttendanceCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new FilterStudentsAttendanceCommandInternal(department, academicYear)
			with AutowiringSecurityServicePermissionsAwareRoutes
			with AutowiringProfileServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with ComposableCommand[FilteredStudentsAttendanceResult]
			with FilterStudentsAttendancePermissions
			with FilterStudentsAttendanceCommandState
			with OnBindFilterStudentsAttendanceCommand
			with ReadOnly with Unaudited
}


class FilterStudentsAttendanceCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[FilteredStudentsAttendanceResult] with BuildsFilteredStudentsAttendanceResult with TaskBenchmarking {

	self: FilterStudentsAttendanceCommandState with TermServiceComponent
		with ProfileServiceComponent with AttendanceMonitoringServiceComponent =>

	override def applyInternal(): FilteredStudentsAttendanceResult = {
		val totalResults = benchmarkTask("profileService.countStudentsByRestrictionsInAffiliatedDepartments") {
			profileService.countStudentsByRestrictionsInAffiliatedDepartments(
				department = department,
				restrictions = buildRestrictions(academicYear)
			)
		}

		val (offset, students) = benchmarkTask("profileService.findStudentsByRestrictionsInAffiliatedDepartments") {
			profileService.findStudentsByRestrictionsInAffiliatedDepartments(
				department = department,
				restrictions = buildRestrictions(academicYear),
				orders = buildOrders(),
				maxResults = studentsPerPage,
				startResult = studentsPerPage * (page - 1)
			)
		}

		if (offset == 0) page = 1

		buildAttendanceResult(totalResults, students, Option(department), academicYear)
	}

}

trait OnBindFilterStudentsAttendanceCommand extends BindListener {

	self: FilterStudentsAttendanceCommandState =>

	override def onBind(result: BindingResult): Unit = {
		if (!hasBeenFiltered) {
			allSprStatuses.filter { status => !status.code.startsWith("P") && !status.code.startsWith("T") }.foreach { sprStatuses.add }
		}
	}

}

trait FilterStudentsAttendancePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: FilterStudentsAttendanceCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, department)
	}

}

trait FilterStudentsAttendanceCommandState extends AttendanceFilterExtras {

	var studentsPerPage = FiltersStudents.DefaultStudentsPerPage
	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm

	// Bind variables

	var page = 1
	var sortOrder: JList[Order] = JArrayList()
	var hasBeenFiltered = false

	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()

}

trait AttendanceFilterExtras extends FiltersStudents {

	def department: Department
	def academicYear: AcademicYear

	final val UNAUTHORISED = "Missed (unauthorised)"
	final val AUTHORISED = "Missed (authorised)"
	final val UNRECORDED = "Unrecorded"
	final val UNAUTHORISED3 = "Missed (unauthorised) 3 or more"
	final val UNAUTHORISED6 = "Missed (unauthorised) 6 or more"

	// For Attendance Monitoring, we shouldn't consider sub-departments
	// but we will use the root department if the current dept has no routes at all
	override lazy val allRoutes: mutable.Buffer[Route] =
		if (department.routes.isEmpty) {
			department.rootDepartment.routes.asScala.sorted(Route.DegreeTypeOrdering)
		} else department.routes.asScala.sorted(Route.DegreeTypeOrdering)

	override lazy val allOtherCriteria: Seq[String] = Seq(
		"Tier 4 only",
		"Visiting",
		"Enrolled for year or course completed",
		UNAUTHORISED,
		AUTHORISED,
		UNRECORDED,
		UNAUTHORISED3,
		UNAUTHORISED6
	)

	override def getAliasPaths(table: String): Seq[(String, AliasAndJoinType)] = {
		(FiltersStudents.AliasPaths ++ Map(
			"attendanceCheckpointTotals" -> Seq(
				"attendanceCheckpointTotals" -> AliasAndJoinType(
					"attendanceCheckpointTotals",
					JoinType.LEFT_OUTER_JOIN,
					Option(Restrictions.conjunction(
						Restrictions.eq("attendanceCheckpointTotals.department", department),
						Restrictions.eq("attendanceCheckpointTotals.academicYear", academicYear)
					))
				)
			)
		))(table)
	}

	override def buildRestrictions(year: AcademicYear): Seq[ScalaRestriction] = {
		super.buildRestrictions(year) ++ Seq(
			unrecordedAttendanceRestriction,
			authorisedAttendanceRestriction,
			unauthorisedAttendanceRestriction,
			unauthorisedAttendance3Restriction,
			unauthorisedAttendance6Restriction
		).flatten
	}

	def unrecordedAttendanceRestriction: Option[ScalaRestriction] = otherCriteria.contains(UNRECORDED) match {
		case false => None
		case true => ScalaRestriction.gt(
			"attendanceCheckpointTotals.unrecorded",
			0,
			getAliasPaths("attendanceCheckpointTotals"): _*
		)
	}

	def authorisedAttendanceRestriction: Option[ScalaRestriction] = otherCriteria.contains(AUTHORISED) match {
		case false => None
		case true => ScalaRestriction.gt(
			"attendanceCheckpointTotals.authorised",
			0,
			getAliasPaths("attendanceCheckpointTotals"): _*
		)
	}

	def unauthorisedAttendanceRestriction: Option[ScalaRestriction] = otherCriteria.contains(UNAUTHORISED) match {
		case false => None
		case true => ScalaRestriction.gt(
			"attendanceCheckpointTotals.unauthorised",
			0,
			getAliasPaths("attendanceCheckpointTotals"): _*
		)
	}

	def unauthorisedAttendance3Restriction: Option[ScalaRestriction] = otherCriteria.contains(UNAUTHORISED3) match {
		case false => None
		case true => ScalaRestriction.gt(
			"attendanceCheckpointTotals.unauthorised",
			2,
			getAliasPaths("attendanceCheckpointTotals"): _*
		)
	}

	def unauthorisedAttendance6Restriction: Option[ScalaRestriction] = otherCriteria.contains(UNAUTHORISED6) match {
		case false => None
		case true => ScalaRestriction.gt(
			"attendanceCheckpointTotals.unauthorised",
			5,
			getAliasPaths("attendanceCheckpointTotals"): _*
		)
	}
}
