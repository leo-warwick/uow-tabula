package uk.ac.warwick.tabula.commands

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.UserGroupMembershipType._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroupSet, SmallGroupSet}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

case class FindStudentsForUserGroupCommandResult(
  staticStudentIds: JList[String],
  membershipItems: Seq[UserGroupMembershipItem]
)

object FindStudentsForUserGroupCommand {
  def apply(department: Department, theModule: Module, theSet: SmallGroupSet) =
    new FindStudentsForUserGroupCommandInternal(department, theSet.academicYear, MemberQueryMembershipAdapter(theSet))
      with AutowiringProfileServiceComponent
      with AutowiringDeserializesFilterImpl
      with AutowiringUserLookupComponent
      with AutowiringSecurityServiceComponent
      with ComposableCommand[FindStudentsForUserGroupCommandResult]
      with PopulateFindStudentsForUserGroupCommand
      with UpdatesFindStudentsForUserGroupCommand
      with FindStudentsForSmallGroupSetPermissions
      with FindStudentsForUserGroupCommandState
      with FiltersStudents
      with DeserializesFilter
      with Unaudited with ReadOnly {
      override def set: SmallGroupSet = theSet
      override def module: Module = theModule
    }

  def apply(department: Department, theSet: DepartmentSmallGroupSet) =
    new FindStudentsForUserGroupCommandInternal(department, theSet.academicYear, MemberQueryMembershipAdapter(theSet))
      with AutowiringProfileServiceComponent
      with AutowiringDeserializesFilterImpl
      with AutowiringUserLookupComponent
      with AutowiringSecurityServiceComponent
      with ComposableCommand[FindStudentsForUserGroupCommandResult]
      with PopulateFindStudentsForUserGroupCommand
      with UpdatesFindStudentsForUserGroupCommand
      with FindStudentsForDepartmentSmallGroupSetPermissions
      with FindStudentsForUserGroupCommandState
      with FiltersStudents
      with DeserializesFilter
      with Unaudited with ReadOnly {
      override def set: DepartmentSmallGroupSet = theSet
    }
}

trait FindStudentsForUserGroupCommandFactory {
  def apply(department: Department, module: Module, set: SmallGroupSet): Appliable[FindStudentsForUserGroupCommandResult]
    with FindStudentsForUserGroupCommandState
    with PopulateOnForm

  def apply(department: Department, set: DepartmentSmallGroupSet): Appliable[FindStudentsForUserGroupCommandResult]
    with FindStudentsForUserGroupCommandState
    with PopulateOnForm
}

object FindStudentsForUserGroupCommandFactoryImpl
  extends FindStudentsForUserGroupCommandFactory {

  override def apply(department: Department, module: Module, set: SmallGroupSet) =
    FindStudentsForUserGroupCommand(department, module, set)

  override def apply(department: Department, set: DepartmentSmallGroupSet) =
    FindStudentsForUserGroupCommand(department, set)
}

class FindStudentsForUserGroupCommandInternal(val department: Department, val academicYear: AcademicYear, val adapter: MemberQueryMembershipAdapter)
  extends CommandInternal[FindStudentsForUserGroupCommandResult]
    with FindStudentsForUserGroupCommandState
    with TaskBenchmarking {

  self: FiltersStudents with ProfileServiceComponent with UserLookupComponent =>

  override def applyInternal(): FindStudentsForUserGroupCommandResult = {
    if (!doFind && (serializeFilter.isEmpty || findStudents.isEmpty)) {
      FindStudentsForUserGroupCommandResult(staticStudentIds, Seq())
    } else {
      doFind = true

      staticStudentIds = benchmarkTask("profileService.findAllUniversityIdsByRestrictionsInTouchedDepartmentsOrModules") {
        profileService.findAllUniversityIdsByRestrictionsInTouchedDepartmentsOrModules(
          department = department,
          modules.asScala.toSet,
          academicYear,
          restrictions = buildRestrictionsNoTier4(academicYear),
          orders = buildOrders()
        ).filter(userLookup.getUserByWarwickUniId(_).isFoundUser)
      }.asJava

      def toMembershipItem(universityId: String, itemType: UserGroupMembershipType) = {
        val user = userLookup.getUserByWarwickUniId(universityId)
        UserGroupMembershipItem(itemType, user.getFirstName, user.getLastName, user.getWarwickId, user.getUserId)
      }

      val startResult = studentsPerPage * (page - 1)
      val staticMembershipItemsToDisplay =
        staticStudentIds.asScala.toSeq.slice(startResult, startResult + studentsPerPage).map(toMembershipItem(_, Static))

      val membershipItems: Seq[UserGroupMembershipItem] = {
        staticMembershipItemsToDisplay.map { item =>
          if (excludedStudentIds.asScala.contains(item.universityId))
            UserGroupMembershipItem(Exclude, item.firstName, item.lastName, item.universityId, item.userId)
          else if (includedStudentIds.asScala.contains(item.universityId))
            UserGroupMembershipItem(Include, item.firstName, item.lastName, item.universityId, item.userId)
          else
            item
        }
      }

      FindStudentsForUserGroupCommandResult(staticStudentIds, membershipItems)
    }
  }

}

trait PopulateFindStudentsForUserGroupCommand extends PopulateOnForm {

  self: FindStudentsForUserGroupCommandState with FiltersStudents with DeserializesFilter =>

  override def populate(): Unit = {
    staticStudentIds = adapter.staticUserIds.toSeq.asJava
    includedStudentIds = adapter.includedUserIds.toSeq.asJava
    excludedStudentIds = adapter.excludedUserIds.toSeq.asJava
    filterQueryString = Option(adapter.memberQuery).getOrElse("")
    linkToSits = adapter.users.isEmpty || adapter.memberQuery.hasText

    // Should only be true when editing
    if (linkToSits && adapter.users.nonEmpty) {
      doFind = true
    }

    if(filterQueryString.hasText) deserializeFilter(filterQueryString)
  }

}

trait UpdatesFindStudentsForUserGroupCommand {

  self: FindStudentsForUserGroupCommandState with FiltersStudents with DeserializesFilter =>

  def update(result: EditUserGroupMembershipCommandResult): Any = {
    includedStudentIds = result.includedStudentIds
    excludedStudentIds = result.excludedStudentIds

    if(filterQueryString.hasText) deserializeFilter(filterQueryString)
  }

}

trait FindStudentsForSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  def set: SmallGroupSet

  def module: Module

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    mustBeLinked(set, module)
    p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
  }
}

trait FindStudentsForDepartmentSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  def set: DepartmentSmallGroupSet

  def department: Department

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    mustBeLinked(set, department)
    p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
  }
}

trait FindStudentsForUserGroupCommandState {
  def academicYear: AcademicYear

  def adapter: MemberQueryMembershipAdapter

  // Bind variables

  // Store original students for reset
  var includedStudentIds: JList[String] = LazyLists.create()
  var excludedStudentIds: JList[String] = LazyLists.create()
  var staticStudentIds: JList[String] = LazyLists.create()
  var filterQueryString: String = ""
  var linkToSits = true
  var findStudents: String = ""
  var doFind: Boolean = false

  // Filter properties
  val defaultOrder: Seq[Order] = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
  var sortOrder: JList[Order] = JArrayList()
  var page = 1

  def totalResults: Int = staticStudentIds.size

  val studentsPerPage: Int = FiltersStudents.DefaultStudentsPerPage

  // Filter binds
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
  val includeTier4Filters: Boolean = false
}

case class EditUserGroupMembershipCommandResult(
  includedStudentIds: JList[String],
  excludedStudentIds: JList[String],
  membershipItems: Seq[UserGroupMembershipItem]
)
