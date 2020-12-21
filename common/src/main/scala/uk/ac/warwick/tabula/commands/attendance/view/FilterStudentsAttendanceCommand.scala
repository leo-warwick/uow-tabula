package uk.ac.warwick.tabula.commands.attendance.view

import org.hibernate.criterion.Order._
import org.hibernate.criterion.{Order, Restrictions}
import org.hibernate.sql.JoinType
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{AutowiringSecurityServicePermissionsAwareRoutes, _}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AliasAndJoinType, ScalaRestriction}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.permissions.Permissions.Profiles
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.jdk.CollectionConverters._

object FilterStudentsAttendanceCommand {
  def apply(department: Department, academicYear: AcademicYear, user: CurrentUser) =
    new FilterStudentsAttendanceCommandInternal(department, academicYear, user)
      with AutowiringSecurityServicePermissionsAwareRoutes
      with AutowiringProfileServiceComponent
      with AutowiringAttendanceMonitoringServiceComponent
      with ComposableCommand[FilteredStudentsAttendanceResult]
      with FilterStudentsAttendancePermissions
      with FilterStudentsAttendanceCommandState
      with OnBindFilterStudentsAttendanceCommand
      with ReadOnly with Unaudited
}


class FilterStudentsAttendanceCommandInternal(val department: Department, val academicYear: AcademicYear, val user: CurrentUser)
  extends CommandInternal[FilteredStudentsAttendanceResult] with BuildsFilteredStudentsAttendanceResult with TaskBenchmarking {

  self: FilterStudentsAttendanceCommandState with ProfileServiceComponent with AttendanceMonitoringServiceComponent =>

  override def applyInternal(): FilteredStudentsAttendanceResult = {
    val totalResults = benchmarkTask("profileService.countStudentsByRestrictionsInAffiliatedDepartments") {
      profileService.countStudentsByRestrictionsInAffiliatedDepartments(
        department = department,
        restrictions = buildRestrictions(user, Seq(department), academicYear, additionalRestrictions)
      )
    }

    val (offset, students) = benchmarkTask("profileService.findStudentsByRestrictionsInAffiliatedDepartments") {
      profileService.findStudentsByRestrictionsInAffiliatedDepartments(
        department = department,
        restrictions = buildRestrictions(user, Seq(department), academicYear, additionalRestrictions),
        orders = buildOrders(),
        maxResults = studentsPerPage,
        startResult = studentsPerPage * (page - 1)
      )
    }

    if (offset == 0) page = 1

    buildAttendanceResult(totalResults, students, None, academicYear)
  }

}

trait OnBindFilterStudentsAttendanceCommand extends BindListener {

  self: FilterStudentsAttendanceCommandState =>

  override def onBind(result: BindingResult): Unit = {
    if (!hasBeenFiltered) {
      allSprStatuses.filterNot(SitsStatus.isWithdrawnStatusOnRoute).foreach {
        sprStatuses.add
      }
    }
  }

}

trait FilterStudentsAttendancePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: FilterStudentsAttendanceCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.MonitoringPoints.View, department)
  }

}

trait FilterStudentsAttendanceCommandState extends AttendanceFilterExtras {

  var studentsPerPage: Int = FiltersStudents.DefaultStudentsPerPage
  val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm

  // Bind variables

  var page = 1
  var sortOrder: JList[Order] = JArrayList()
  var hasBeenFiltered = false

  var courseTypes: JList[CourseType] = JArrayList()
  var specificCourseTypes: JList[SpecificCourseType] = JArrayList()
  var routes: JList[Route] = JArrayList()
  var courses: JList[Course] = JArrayList()
  var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
  var yearsOfStudy: JList[JInteger] = JArrayList()
  var levelCodes: JList[String] = JArrayList()
  var studyLevelCodes: JList[String] = JArrayList()
  var sprStatuses: JList[SitsStatus] = JArrayList()
  var modules: JList[Module] = JArrayList()
  var hallsOfResidence: JList[String] = JArrayList()
  var postcodes: JList[String] = JArrayList()

}

trait AttendanceFilterExtras extends FiltersStudents {

  def department: Department

  def academicYear: AcademicYear

  def user: CurrentUser

  final val UNAUTHORISED = "Missed (unauthorised)"
  final val AUTHORISED = "Missed (authorised)"
  final val UNRECORDED = "Unrecorded"
  final val UNAUTHORISED3 = "Missed (unauthorised) 3 or more"
  final val UNAUTHORISED6 = "Missed (unauthorised) 6 or more"

  // For Attendance Monitoring, we shouldn't consider sub-departments
  // but we will use the root department if the current dept has no routes at all
  // TAB-6907 but only active ones
  override lazy val allRoutes: Seq[Route] = {
    if (department.routes.isEmpty) {
      department.rootDepartment.routes.asScala.toSeq.sorted(Route.DegreeTypeOrdering)
    } else department.routes.asScala.toSeq.sorted(Route.DegreeTypeOrdering)
  }.filter(_.active)

  def includeTier4Filters: Boolean = securityService.can(user, Profiles.Read.Tier4VisaRequirement, department)
  override lazy val allOtherCriteria: Seq[String] = (if(includeTier4Filters) Seq("Tier 4 only") else Seq()) ++ Seq(
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
    )) (table)
  }

  protected def additionalRestrictions: Seq[ScalaRestriction] = ScalaRestriction.anyOf(Seq(
      unrecordedAttendanceRestriction,
      authorisedAttendanceRestriction,
      unauthorisedAttendanceRestriction,
      unauthorisedAttendance3Restriction,
      unauthorisedAttendance6Restriction
    ).flatten: _*).toSeq

  def unrecordedAttendanceRestriction: Option[ScalaRestriction] = if (otherCriteria.contains(UNRECORDED)) {
    ScalaRestriction.gt(
      "attendanceCheckpointTotals.unrecorded",
      0,
      getAliasPaths("attendanceCheckpointTotals"): _*
    )
  } else {
    None
  }

  def authorisedAttendanceRestriction: Option[ScalaRestriction] = if (otherCriteria.contains(AUTHORISED)) {
    ScalaRestriction.gt(
      "attendanceCheckpointTotals.authorised",
      0,
      getAliasPaths("attendanceCheckpointTotals"): _*
    )
  } else {
    None
  }

  def unauthorisedAttendanceRestriction: Option[ScalaRestriction] = if (otherCriteria.contains(UNAUTHORISED)) {
    ScalaRestriction.gt(
      "attendanceCheckpointTotals.unauthorised",
      0,
      getAliasPaths("attendanceCheckpointTotals"): _*
    )
  } else {
    None
  }

  def unauthorisedAttendance3Restriction: Option[ScalaRestriction] = if (otherCriteria.contains(UNAUTHORISED3)) {
    ScalaRestriction.gt(
      "attendanceCheckpointTotals.unauthorised",
      2,
      getAliasPaths("attendanceCheckpointTotals"): _*
    )
  } else {
    None
  }

  def unauthorisedAttendance6Restriction: Option[ScalaRestriction] = if (otherCriteria.contains(UNAUTHORISED6)) {
    ScalaRestriction.gt(
      "attendanceCheckpointTotals.unauthorised",
      5,
      getAliasPaths("attendanceCheckpointTotals"): _*
    )
  } else {
    None
  }
}
