package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.roles.FeedbackRecipient
import uk.ac.warwick.tabula.roles.Submitter
import uk.ac.warwick.tabula.roles.SettingsOwner
import uk.ac.warwick.tabula.helpers.Promises._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesStudent
import uk.ac.warwick.tabula.helpers.MutablePromise

/**
  * A special multi-purpose role provider that provides users access to their own data, generally this isn't an explicit permission.
  */
@Component
class OwnDataRoleProvider extends RoleProvider with TaskBenchmarking {

  val departmentService: MutablePromise[ModuleAndDepartmentService] = promise {
    Wire[ModuleAndDepartmentService]
  }

  def getRolesFor(user: CurrentUser, scope: PermissionsTarget): LazyList[Role] = benchmarkTask("Get roles for OwnDataRoleProvider") {
    lazy val department =
      user.departmentCode.maybeText.flatMap { code => departmentService.get.getDepartmentByCode(code.toLowerCase) }

    scope match {
      // You can view your own submission
      case submission: Submission =>
        if (submission.usercode == user.userId)
          LazyList(customRoleFor(department)(SubmitterRoleDefinition, submission).getOrElse(Submitter(submission)))
        else LazyList.empty

      // You can view feedback to your work, but only if it's released
      case feedback: Feedback =>
        if (feedback.usercode == user.userId && feedback.released)
          LazyList(customRoleFor(department)(FeedbackRecipientRoleDefinition, feedback).getOrElse(FeedbackRecipient(feedback)))
        else LazyList.empty

      // You can change your own user settings
      case settings: UserSettings =>
        if (user.apparentId.hasText && settings.userId == user.apparentId)
          LazyList(customRoleFor(department)(SettingsOwnerRoleDefinition, settings).getOrElse(SettingsOwner(settings)))
        else LazyList.empty

      // You can view small groups that you are a member of as long as it's visible to students
      case smallGroup: SmallGroup =>
        val studentId = user.apparentUser.getWarwickId
        if (studentId.hasText && smallGroup.groupSet.visibleToStudents && smallGroup.students.includesUser(user.apparentUser))
          LazyList(customRoleFor(smallGroup.groupSet.module.adminDepartment)(SmallGroupMemberRoleDefinition, smallGroup).getOrElse(SmallGroupMember(smallGroup)))
        else LazyList.empty

      // TAB-2122
      case note: AbstractMemberNote =>
        if (user.apparentId.hasText && note.creator.getUserId == user.apparentId)
          LazyList(customRoleFor(department)(MemberNoteCreatorRoleDefinition, note).getOrElse(MemberNoteCreator(note)))
        else LazyList.empty

      case mitCircsStudent: MitigatingCircumstancesStudent =>
        val student = mitCircsStudent.student
        if (user.apparentId == student.userId && Option(student.mostSignificantCourse).flatMap(c => Option(c.department)).exists(_.subDepartmentsContaining(student).exists(_.enableMitCircs)))
          LazyList(MitigatingCircumstancesSubmitter(mitCircsStudent))
        else
          LazyList.empty

      case _ => LazyList.empty
    }
  }

  def rolesProvided: Set[Class[_ <: Role]] = Set(
    classOf[Submitter],
    classOf[FeedbackRecipient],
    classOf[SettingsOwner],
    classOf[SmallGroupMember],
    classOf[MemberNoteCreator],
    classOf[MitigatingCircumstancesSubmitter]
  )

}
