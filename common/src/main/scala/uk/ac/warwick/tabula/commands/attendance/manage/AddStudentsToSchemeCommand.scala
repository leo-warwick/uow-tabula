package uk.ac.warwick.tabula.commands.attendance.manage

import org.joda.time.{DateTime, LocalDate}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.data.{SchemeMembershipIncludeType, SchemeMembershipItem, SchemeMembershipStaticType}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

object AddStudentsToSchemeCommand {
  def apply(scheme: AttendanceMonitoringScheme, user: CurrentUser) =
    new AddStudentsToSchemeCommandInternal(scheme, user)
      with AutowiringAttendanceMonitoringServiceComponent
      with AutowiringProfileServiceComponent
      with AutowiringSecurityServiceComponent
      with ComposableCommand[AttendanceMonitoringScheme]
      with AddStudentsToSchemeValidation
      with AddStudentsToSchemeDescription
      with AddStudentsToSchemePermissions
      with AddStudentsToSchemeCommandState
}


class AddStudentsToSchemeCommandInternal(val scheme: AttendanceMonitoringScheme, val user: CurrentUser)
  extends CommandInternal[AttendanceMonitoringScheme] with TaskBenchmarking with RequiresCheckpointTotalUpdate {

  self: AddStudentsToSchemeCommandState with AttendanceMonitoringServiceComponent with ProfileServiceComponent =>

  override def applyInternal(): AttendanceMonitoringScheme = {
    val previousUniversityIds = scheme.members.members

    if (linkToSits && !scheme.academicYear.isSITSInFlux(LocalDate.now)) {
      scheme.members.staticUserIds = staticStudentIds.asScala.toSet
      scheme.members.includedUserIds = includedStudentIds.asScala.toSet
      scheme.members.excludedUserIds = excludedStudentIds.asScala.toSet
      scheme.memberQuery = filterQueryString
    } else {
      scheme.members.staticUserIds = Set.empty
      scheme.members.includedUserIds = Set.empty
      scheme.members.excludedUserIds = Set.empty
      scheme.memberQuery = null
      scheme.members.includedUserIds = ((staticStudentIds.asScala diff excludedStudentIds.asScala) ++ includedStudentIds.asScala).toSet
    }

    scheme.updatedDate = DateTime.now
    attendanceMonitoringService.saveOrUpdate(scheme)

    updateCheckpointTotals((previousUniversityIds ++ scheme.members.members).toSeq, scheme.department, scheme.academicYear)

    scheme
  }

}

trait AddStudentsToSchemeValidation extends SelfValidating with TaskBenchmarking {

  self: AddStudentsToSchemeCommandState with ProfileServiceComponent with SecurityServiceComponent =>

  override def validate(errors: Errors): Unit = {
    // In practice there should be no students that fail this validation
    // but this protects against hand-rolled POSTs
    val members = benchmark("profileService.getAllMembersWithUniversityIds") {
      profileService.getAllMembersWithUniversityIds(
        ((staticStudentIds.asScala.toSeq
          diff excludedStudentIds.asScala.toSeq)
          diff includedStudentIds.asScala.toSeq)
          ++ includedStudentIds.asScala.toSeq
      )
    }
    val noPermissionMembers = benchmark("noPermissionMembers") {
      members.filter {
        case student: StudentMember =>
          !student.affiliatedDepartments.contains(scheme.department) &&
            !securityService.can(user, Permissions.MonitoringPoints.Manage, student)
        case _ => false
      }
    }
    if (noPermissionMembers.nonEmpty) {
      errors.rejectValue(
        "staticStudentIds",
        "attendanceMonitoringScheme.student.noPermission",
        Array(noPermissionMembers.map(_.universityId).mkString(", ")),
        "You do not have permission to manage these students' attendance"
      )
    }
  }

}

trait AddStudentsToSchemePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: AddStudentsToSchemeCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.MonitoringPoints.Manage, scheme)
  }

}

trait AddStudentsToSchemeDescription extends Describable[AttendanceMonitoringScheme] {

  self: AddStudentsToSchemeCommandState =>

  override lazy val eventName = "AddStudentsToScheme"

  override def describe(d: Description): Unit = {
    d.attendanceMonitoringScheme(scheme)
      .property("membershipCount" -> ((scheme.members.staticUserIds diff scheme.members.excludedUserIds) ++ scheme.members.includedUserIds).size)
  }

  override def describeResult(d: Description) {
    d.attendanceMonitoringScheme(scheme)
      .property("membershipCount" -> membershipItems.size)
  }
}

trait AddStudentsToSchemeCommandState {

  self: AttendanceMonitoringServiceComponent =>

  def scheme: AttendanceMonitoringScheme

  def user: CurrentUser

  def membershipItems: Seq[SchemeMembershipItem] = {
    val staticMemberItems = attendanceMonitoringService.findSchemeMembershipItems(
      (staticStudentIds.asScala.toSeq diff excludedStudentIds.asScala.toSeq) diff includedStudentIds.asScala.toSeq,
      SchemeMembershipStaticType,
      scheme.academicYear
    )
    val includedMemberItems = attendanceMonitoringService.findSchemeMembershipItems(includedStudentIds.asScala.toSeq, SchemeMembershipIncludeType, scheme.academicYear)

    (staticMemberItems ++ includedMemberItems).sortBy(membershipItem => (membershipItem.lastName, membershipItem.firstName))
  }

  // Bind variables

  var includedStudentIds: JList[String] = LazyLists.create()
  var excludedStudentIds: JList[String] = LazyLists.create()
  var staticStudentIds: JList[String] = LazyLists.create()
  var filterQueryString: String = ""
  var linkToSits: Boolean = _
  var doFind: Boolean = _
}
