package uk.ac.warwick.tabula.commands.attendance.manage

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.data.{SchemeMembershipExcludeType, SchemeMembershipIncludeType, SchemeMembershipItem, SchemeMembershipStaticType}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.permissions.Permissions.Profiles
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

case class FindStudentsForSchemeCommandResult(
  staticStudentIds: JList[String],
  membershipItems: Seq[SchemeMembershipItem]
)

object FindStudentsForSchemeCommand {
  def apply(scheme: AttendanceMonitoringScheme, user: CurrentUser) =
    new FindStudentsForSchemeCommandInternal(scheme, user)
      with AutowiringProfileServiceComponent
      with AutowiringAttendanceMonitoringServiceComponent
      with AutowiringDeserializesFilterImpl
      with AutowiringUserLookupComponent
      with AutowiringSecurityServiceComponent
      with ComposableCommand[FindStudentsForSchemeCommandResult]
      with PopulateFindStudentsForSchemeCommand
      with UpdatesFindStudentsForSchemeCommand
      with FindStudentsForSchemePermissions
      with FindStudentsForSchemeCommandState
      with Unaudited with ReadOnly
}


class FindStudentsForSchemeCommandInternal(val scheme: AttendanceMonitoringScheme, val user: CurrentUser)
  extends CommandInternal[FindStudentsForSchemeCommandResult] with TaskBenchmarking {

  self: ProfileServiceComponent with FindStudentsForSchemeCommandState with AttendanceMonitoringServiceComponent with UserLookupComponent =>

  override def applyInternal(): FindStudentsForSchemeCommandResult = {
    // Remove any duplicate routes caused by search for routes already displayed
    routes = routes.asScala.distinct.asJava

    if (!doFind && (serializeFilter.isEmpty || findStudents.isEmpty)) {
      FindStudentsForSchemeCommandResult(staticStudentIds, Seq())
    } else {
      doFind = true

      staticStudentIds = benchmarkTask("profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments") {
        profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(
          department = department,
          restrictions = buildRestrictions(user, Seq(scheme.department), scheme.academicYear),

          orders = buildOrders()
        ).filter(userLookup.getUserByWarwickUniId(_).isFoundUser)
      }.asJava

      val startResult = studentsPerPage * (page - 1)
      val staticMembershipItemsToDisplay = attendanceMonitoringService.findSchemeMembershipItems(
        staticStudentIds.asScala.toSeq.slice(startResult, startResult + studentsPerPage),
        SchemeMembershipStaticType,
        scheme.academicYear
      )

      val membershipItems: Seq[SchemeMembershipItem] = {
        staticMembershipItemsToDisplay.map { item =>
          if (excludedStudentIds.asScala.contains(item.universityId))
            SchemeMembershipItem(SchemeMembershipExcludeType, item.firstName, item.lastName, item.universityId, item.userId, item.existingSchemes)
          else if (includedStudentIds.asScala.contains(item.universityId))
            SchemeMembershipItem(SchemeMembershipIncludeType, item.firstName, item.lastName, item.universityId, item.userId, item.existingSchemes)
          else
            item
        }
      }

      FindStudentsForSchemeCommandResult(staticStudentIds, membershipItems)
    }
  }

}

trait PopulateFindStudentsForSchemeCommand extends PopulateOnForm {

  self: FindStudentsForSchemeCommandState =>

  override def populate(): Unit = {
    staticStudentIds = scheme.members.staticUserIds.toSeq.asJava
    includedStudentIds = scheme.members.includedUserIds.toSeq.asJava
    excludedStudentIds = scheme.members.excludedUserIds.toSeq.asJava
    filterQueryString = scheme.memberQuery
    linkToSits = scheme.memberQuery.hasText || scheme.members.members.isEmpty
    // Should only be true when editing
    if (linkToSits && scheme.members.members.nonEmpty) {
      doFind = true
    }
    if(filterQueryString.hasText) deserializeFilter(filterQueryString)
  }

}

trait UpdatesFindStudentsForSchemeCommand {

  self: FindStudentsForSchemeCommandState =>

  def update(editSchemeMembershipCommandResult: EditSchemeMembershipCommandResult): Any = {
    includedStudentIds = editSchemeMembershipCommandResult.includedStudentIds
    excludedStudentIds = editSchemeMembershipCommandResult.excludedStudentIds
    if(filterQueryString.hasText) deserializeFilter(filterQueryString)
  }

}

trait FindStudentsForSchemePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: FindStudentsForSchemeCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.MonitoringPoints.Manage, scheme)
  }

}

trait FindStudentsForSchemeCommandState extends FiltersStudents with DeserializesFilter {
  def scheme: AttendanceMonitoringScheme

  def user: CurrentUser

  def department: Department = scheme.department
  def includeTier4Filters: Boolean = securityService.can(user, Profiles.Read.Tier4VisaRequirement, department)

  // Bind variables

  var includedStudentIds: JList[String] = LazyLists.create()
  var excludedStudentIds: JList[String] = LazyLists.create()
  var staticStudentIds: JList[String] = LazyLists.create()
  var filterQueryString: String = ""
  var linkToSits: Boolean = true
  var findStudents: String = ""
  var doFind: Boolean = false

  // Filter properties
  val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
  var sortOrder: JList[Order] = JArrayList()
  var page = 1

  def totalResults: Int = staticStudentIds.size

  val studentsPerPage: Int = FiltersStudents.DefaultStudentsPerPage

  // Filter binds
  var courseTypes: JList[CourseType] = JArrayList()
  var specificCourseTypes: JList[SpecificCourseType] = JArrayList()
  var routes: JList[Route] = JArrayList()
  lazy val outOfDepartmentRoutes: Seq[Route] = routes.asScala.toSeq.diff(allRoutes)
  var courses: JList[Course] = JArrayList()
  var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
  var yearsOfStudy: JList[JInteger] = JArrayList()
  var levelCodes: JList[String] = JArrayList()
  var studyLevelCodes: JList[String] = JArrayList()
  var sprStatuses: JList[SitsStatus] = JArrayList()
  var modules: JList[Module] = JArrayList()
  var hallsOfResidence: JList[String] = JArrayList()
  var postcodes: JList[String] = JArrayList()
  var filterChanged: Boolean = false
}
