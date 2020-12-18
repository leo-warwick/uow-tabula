package uk.ac.warwick.tabula.commands.profiles

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order._
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AliasAndJoinType, ScalaRestriction}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.permissions.Permissions.Profiles
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringSecurityServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

case class FilterStudentsResults(
  students: Seq[StudentMember],
  totalResults: Int
)

object FilterStudentsCommand {
  def apply(department: Department, year: AcademicYear, user: CurrentUser) =
    new FilterStudentsCommand(department, year, user)
      with ComposableCommand[FilterStudentsResults]
      with FilterStudentsPermissions
      with AutowiringProfileServiceComponent
      with AutowiringSecurityServiceComponent
      with ReadOnly with Unaudited
}

abstract class FilterStudentsCommand(val department: Department, val year: AcademicYear, val user: CurrentUser)
  extends CommandInternal[FilterStudentsResults] with FilterStudentsState with BindListener with TaskBenchmarking {
  self: ProfileServiceComponent =>

  def applyInternal(): FilterStudentsResults = {
    val restrictions = buildRestrictions(user, Seq(department), year, Seq(hasAdminNoteRestriction).flatten)

    val totalResults = benchmarkTask("countStudentsByRestrictions") {
      profileService.countStudentsByRestrictions(
        department = department,
        academicYear = year,
        restrictions = restrictions
      )
    }

    val (offset, students) = benchmarkTask("findStudentsByRestrictions") {
      profileService.findStudentsByRestrictions(
        department = department,
        academicYear = year,
        restrictions = restrictions,
        orders = buildOrders(),
        maxResults = studentsPerPage,
        startResult = studentsPerPage * (page - 1)
      )
    }

    if (offset == 0) page = 1

    FilterStudentsResults(students, totalResults)
  }

  def onBind(result: BindingResult): Unit = {
    // Add all non-withdrawn codes to SPR statuses by default
    if (!hasBeenFiltered) {
      allSprStatuses.filterNot(SitsStatus.isWithdrawnStatusOnRoute).foreach {
        sprStatuses.add
      }
    }
  }
}

trait FilterStudentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: FilterStudentsState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Profiles.Search, department)
  }
}

trait FilterStudentsState extends ProfileFilterExtras {
  override def department: Department
  def user: CurrentUser

  var studentsPerPage: Int = FiltersStudents.DefaultStudentsPerPage
  var page = 1

  val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
  var sortOrder: JList[Order] = JArrayList()

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

  var hasBeenFiltered = false
}

trait ProfileFilterExtras extends FiltersStudents {

  self: FilterStudentsState =>

  final val HAS_ADMIN_NOTE = "Has administrative note"

  def includeTier4Filters: Boolean = securityService.can(user, Profiles.Read.Tier4VisaRequirement, department)
  override lazy val allOtherCriteria: Seq[String] = (if (includeTier4Filters) Seq("Tier 4 only") else Seq()) ++ Seq(
    "Visiting",
    "Enrolled for year or course completed",
    HAS_ADMIN_NOTE
  )

  override def getAliasPaths(table: String): Seq[(String, AliasAndJoinType)] = {
    (FiltersStudents.AliasPaths ++ Map(
      "memberNotes" -> Seq(
        "memberNotes" -> AliasAndJoinType("memberNotes")
      )
    )) (table)
  }

  def hasAdminNoteRestriction: Option[ScalaRestriction] = if (otherCriteria.contains(HAS_ADMIN_NOTE)) {
    ScalaRestriction.notEmpty(
      "memberNotes",
      getAliasPaths("memberNotes"): _*
    )
  } else {
    None
  }
}
