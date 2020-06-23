package uk.ac.warwick.tabula.commands.marks

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand._
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.MarksWorkflowProgress
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.marks._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, WorkflowStage, WorkflowStages}

import scala.collection.immutable.ListMap

object ListAssessmentComponentsCommand {
  case class StudentMarkRecord(
    universityId: String,
    resitSequence: Option[String],
    position: Option[Int],
    currentMember: Boolean,
    resitExpected: Boolean,
    furtherFirstSit: Boolean,
    mark: Option[Int],
    grade: Option[String],
    needsWritingToSits: Boolean,
    outOfSync: Boolean,
    markState: Option[MarkState],
    agreed: Boolean,
    resitMark: Boolean, // the current mark is a resit mark (not the same as a resit being expected)
    history: Seq[RecordedAssessmentComponentStudentMark], // Most recent first
    upstreamAssessmentGroupMember: UpstreamAssessmentGroupMember
  )
  object StudentMarkRecord {
    def apply(info: UpstreamAssessmentGroupInfo, member: UpstreamAssessmentGroupMember, recordedStudent: Option[RecordedAssessmentComponentStudent]): StudentMarkRecord = {
      val reassessment = member.isReassessment
      val furtherFirstSit = reassessment && member.currentResitAttempt.exists(_ <= 1)
      val isAgreedSITS = recordedStudent.forall(!_.needsWritingToSits) && (member.agreedMark.nonEmpty || member.agreedGrade.nonEmpty)

      StudentMarkRecord(
        universityId = member.universityId,
        resitSequence = member.resitSequence,
        position = member.position,
        currentMember = info.currentMembers.contains(member),
        resitExpected = reassessment,
        furtherFirstSit = furtherFirstSit,

        // These are needlessly verbose but thought better to be explicit on the order
        mark = recordedStudent match {
          case Some(marks) if marks.needsWritingToSits => marks.latestMark
          case _ if isAgreedSITS => member.agreedMark
          case Some(marks) => marks.latestMark
          case _ if member.firstDefinedMark.nonEmpty => member.firstDefinedMark
          case _ => None
        },
        grade = recordedStudent match {
          case Some(marks) if marks.needsWritingToSits => marks.latestGrade
          case _ if isAgreedSITS => member.agreedGrade
          case Some(marks) => marks.latestGrade
          case _ if member.firstDefinedGrade.nonEmpty => member.firstDefinedGrade
          case _ => None
        },

        needsWritingToSits = recordedStudent.exists(_.needsWritingToSits),
        outOfSync =
          recordedStudent.exists(!_.needsWritingToSits) && (
            recordedStudent.flatMap(_.latestMark).exists(m => !member.firstDefinedMark.contains(m)) ||
            recordedStudent.flatMap(_.latestGrade).exists(g => !member.firstDefinedGrade.contains(g))
          ),
        markState = recordedStudent.flatMap(_.latestState),
        agreed = isAgreedSITS,
        resitMark = reassessment,
        history = recordedStudent.map(_.marks).getOrElse(Seq.empty),
        member
      )
    }
  }

  def studentMarkRecords(info: UpstreamAssessmentGroupInfo, assessmentComponentMarksService: AssessmentComponentMarksService): Seq[StudentMarkRecord] = {
    val recordedStudents = assessmentComponentMarksService.getAllRecordedStudents(info.upstreamAssessmentGroup)

    info.allMembers.sortBy { uagm => (uagm.universityId, uagm.resitSequence.getOrElse("000")) }.map { member =>
      val recordedStudent = recordedStudents.find(_.matchesIdentity(member))

      StudentMarkRecord(info, member, recordedStudent)
    }
  }

  case class AssessmentComponentInfo(
    assessmentComponent: AssessmentComponent,
    upstreamAssessmentGroup: UpstreamAssessmentGroup,
    students: Seq[StudentMarkRecord],

    // Progress
    progress: MarksWorkflowProgress,
    nextStage: Option[WorkflowStage],
    stages: ListMap[String, WorkflowStages.StageProgress],
  ) {
    val studentsWithMarks: Seq[StudentMarkRecord] = students.filter(s => s.mark.nonEmpty || s.grade.nonEmpty)

    val needsWritingToSits: Boolean = students.exists(_.needsWritingToSits)
    val outOfSync: Boolean = students.exists(_.outOfSync)
    val allAgreed: Boolean = students.nonEmpty && students.forall(_.agreed)
  }
  type Result = Seq[AssessmentComponentInfo]
  type Command = Appliable[Result]

  val AdminPermission: Permission = Permissions.Feedback.Manage

  def apply(department: Department, academicYear: AcademicYear, currentUser: CurrentUser): Command =
    new ListAssessmentComponentsCommandInternal(department, academicYear, currentUser)
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringSecurityServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringMarksWorkflowProgressServiceComponent
      with ComposableCommand[Result]
      with ListAssessmentComponentsModulesWithPermission
      with ListAssessmentComponentsPermissions
      with Unaudited with ReadOnly
}

abstract class ListAssessmentComponentsCommandInternal(val department: Department, val academicYear: AcademicYear, val currentUser: CurrentUser)
  extends CommandInternal[Result]
    with ListAssessmentComponentsState
    with ListAssessmentComponentsForModulesWithPermission {
  self: AssessmentComponentMarksServiceComponent
    with AssessmentMembershipServiceComponent
    with MarksWorkflowProgressServiceComponent
    with ListAssessmentComponentsModulesWithPermission =>

  override def applyInternal(): Result = assessmentComponentInfos

}

trait ListAssessmentComponentsForModulesWithPermission {
  self: ListAssessmentComponentsState
    with AssessmentMembershipServiceComponent
    with AssessmentComponentMarksServiceComponent
    with MarksWorkflowProgressServiceComponent
    with ListAssessmentComponentsModulesWithPermission =>

  lazy val assessmentComponentInfos: Seq[AssessmentComponentInfo] = {
    val assessmentComponents: Seq[AssessmentComponent] =
      assessmentMembershipService.getAssessmentComponents(department, includeSubDepartments = false, inUseOnly = false)
        .filter { ac =>
          ac.sequence != AssessmentComponent.NoneAssessmentGroup &&
          (canAdminDepartment || modulesWithPermission.contains(ac.module))
        }

    val assessmentComponentsByKey: Map[AssessmentComponentKey, AssessmentComponent] =
      assessmentComponents.map { ac =>
        AssessmentComponentKey(ac) -> ac
      }.toMap

    assessmentMembershipService.getUpstreamAssessmentGroupInfoForComponents(assessmentComponents, academicYear)
      .filter(_.allMembers.nonEmpty)
      .map { upstreamAssessmentGroupInfo =>
        val assessmentComponent = assessmentComponentsByKey(AssessmentComponentKey(upstreamAssessmentGroupInfo.upstreamAssessmentGroup))
        val upstreamAssessmentGroup = upstreamAssessmentGroupInfo.upstreamAssessmentGroup
        val students = studentMarkRecords(upstreamAssessmentGroupInfo, assessmentComponentMarksService)

        val progress = workflowProgressService.componentProgress(assessmentComponent, upstreamAssessmentGroup, students)

        AssessmentComponentInfo(
          assessmentComponent,
          upstreamAssessmentGroup,
          students,
          progress = MarksWorkflowProgress(progress.percentage, progress.cssClass, progress.messageCode),
          nextStage = progress.nextStage,
          stages = progress.stages,
        )
      }
      .sortBy { info =>
        // module_code, assessment_group, sequence, mav_occurrence
        (info.assessmentComponent.moduleCode, info.assessmentComponent.assessmentGroup, info.assessmentComponent.sequence, info.upstreamAssessmentGroup.occurrence)
      }
  }
}

trait ListAssessmentComponentsState {
  def department: Department
  def academicYear: AcademicYear
  def currentUser: CurrentUser
}

trait ListAssessmentComponentsModulesWithPermission {
  self: ListAssessmentComponentsState
    with SecurityServiceComponent
    with ModuleAndDepartmentServiceComponent =>

  lazy val canAdminDepartment: Boolean = securityService.can(currentUser, AdminPermission, department)
  lazy val modulesWithPermission: Set[Module] = moduleAndDepartmentService.modulesWithPermission(currentUser, AdminPermission, department)
}

trait ListAssessmentComponentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ListAssessmentComponentsModulesWithPermission
    with ListAssessmentComponentsState
    with SecurityServiceComponent
    with ModuleAndDepartmentServiceComponent =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    if (canAdminDepartment || modulesWithPermission.isEmpty) p.PermissionCheck(AdminPermission, mandatory(department))
    else p.PermissionCheckAll(AdminPermission, modulesWithPermission)
}