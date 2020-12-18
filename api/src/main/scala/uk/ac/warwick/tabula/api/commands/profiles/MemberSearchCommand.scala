package uk.ac.warwick.tabula.api.commands.profiles

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order.asc
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.api.commands.profiles.MemberSearchCommand._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringMemberDaoComponent, MemberDaoComponent}
import uk.ac.warwick.tabula.permissions.Permissions.Profiles
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

object MemberSearchCommand {
  val MaxLimit = 100
  val DefaultLimit = 10

  def apply(departments: Seq[Department], user: CurrentUser) =
    new MemberSearchCommandInternal(departments, user)
      with ComposableCommand[Seq[Member]]
      with AutowiringProfileServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringMemberDaoComponent
      with AutowiringSecurityServiceComponent
      with MemberSearchCommandRequest
      with MemberSearchCommandValidation
      with ReadOnly with Unaudited
}

abstract class MemberSearchCommandInternal(override val departments: Seq[Department], val user: CurrentUser) extends CommandInternal[Seq[Member]] with FiltersStudents {

  self: MemberSearchCommandRequest with ModuleAndDepartmentServiceComponent with MemberDaoComponent =>

  def department: Department = null

  override def applyInternal(): Seq[Member] = {
    if (departments.isEmpty && serializeFilter.isEmpty) {
      // This is validated in MemberSearchCommandValidation
      throw new IllegalArgumentException("At least one filter value must be defined")
    }

    val restrictions = buildRestrictions(user, departments, academicYear)
    sortOrder = JList(asc("userId"), asc("universityId"))

    departments match {
      case Nil => profileService.findAllMembersByRestrictions(restrictions, buildOrders(), limit, offset)
      case departments => departments.flatMap { department =>
        profileService.findAllMembersByRestrictionsInAffiliatedDepartments(department, restrictions, buildOrders(), limit, offset)
      }
    }
  }

  lazy val total: Int = {
    val restrictions = buildRestrictions(user, departments, academicYear)
    departments match {
      case Nil => profileService.countAllMembersByRestrictions(restrictions)
      case departments => departments.map(profileService.countAllMembersByRestrictionsInAffiliatedDepartments(_, restrictions)).sum
    }
  }

}

trait MemberSearchCommandRequest extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: SecurityServiceComponent =>

  def departments: Seq[Department]
  def user: CurrentUser
  val includeTier4Filters: Boolean = departments.forall(d => securityService.can(user, Profiles.Read.Tier4VisaRequirement, d))

  val defaultOrder: Seq[Order] = Seq(asc("lastName"), asc("firstName"))

  var academicYear: AcademicYear = AcademicYear.now()
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

  var offset: Int = 0
  var limit: Int = DefaultLimit
  var fields: String = "member"

  override def permissionsCheck(p: PermissionsChecking): Unit =
    if (departments.isEmpty) {
      p.PermissionCheck(Permissions.Profiles.ViewSearchResults, PermissionsTarget.Global)
    } else departments.foreach { department =>
      p.PermissionCheck(Permissions.Profiles.ViewSearchResults, department)
    }
}

trait MemberSearchCommandValidation extends SelfValidating {
  self: MemberSearchCommandRequest with FiltersStudents =>

  override def validate(errors: Errors): Unit = {
    if (offset < 0) errors.rejectValue("offset", "offset.min", Array[Object](0: JInteger), null)
    if (limit < 1) errors.rejectValue("limit", "limit.min", Array[Object](1: JInteger), null)

    if (limit > MaxLimit) {
      errors.rejectValue("limit", "limit.max", Array[Object](MaxLimit: JInteger), null)
    } else if (limit > DefaultLimit && (fields == "" || fields == "member")) {
      errors.rejectValue("limit", "limit.specifyFields", Array[Object](DefaultLimit: JInteger), null)
    }

    if (departments.isEmpty && serializeFilter.isEmpty) {
      errors.reject("memberSearch.mustFilter")
    }
  }
}
