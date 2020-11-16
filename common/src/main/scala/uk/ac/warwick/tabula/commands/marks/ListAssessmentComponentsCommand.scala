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
    isReassessment: Boolean,
    currentResitAttempt: Option[Int],
    mark: Option[Int],
    grade: Option[String],
    requiresResit: Boolean, // the grade assigned indicates that further assessment is required
    incrementsAttempt: Boolean, // the grade assigned indicates that the attempt should be incremented
    existingResit: Option[RecordedResit], // a new reassessment record for the next exam period
    needsWritingToSits: Boolean,
    outOfSync: Boolean,
    sitsWriteError: Option[RecordedAssessmentComponentStudentMarkSitsError],
    markState: Option[MarkState],
    agreed: Boolean,
    recordedStudent: Option[RecordedAssessmentComponentStudent],
    history: Seq[RecordedAssessmentComponentStudentMark], // Most recent first
    upstreamAssessmentGroupMember: UpstreamAssessmentGroupMember
  ) {
    val furtherFirstSit: Boolean = isReassessment && currentResitAttempt.exists(_ <= 1)
  }
  object StudentMarkRecord {
    def apply(
      info: UpstreamAssessmentGroupInfo,
      member: UpstreamAssessmentGroupMember,
      recordedStudent: Option[RecordedAssessmentComponentStudent],
      existingResit: Option[RecordedResit],
      gradeBoundary: Option[GradeBoundary]
    ): StudentMarkRecord = {
      val reassessment = member.isReassessment
      val isAgreedSITS = recordedStudent.forall(!_.needsWritingToSits) && (member.agreedMark.nonEmpty || member.agreedGrade.nonEmpty)
      val requiresResit: Boolean = gradeBoundary.exists(_.generatesResit)
      val incrementsAttempt: Boolean = gradeBoundary.exists(_.incrementsAttempt)

      StudentMarkRecord(
        universityId = member.universityId,
        resitSequence = member.resitSequence,
        position = member.position,
        currentMember = info.currentMembers.contains(member),
        isReassessment = reassessment,
        currentResitAttempt = member.currentResitAttempt,
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
        requiresResit,
        incrementsAttempt,
        existingResit,
        needsWritingToSits = recordedStudent.exists(_.needsWritingToSits),
        outOfSync =
          recordedStudent.exists(!_.needsWritingToSits) && (
            recordedStudent.flatMap(_.latestState).exists {
              // State is agreed and we have a mark or grade but UAGM has no agreed marks
              case MarkState.Agreed if recordedStudent.exists(r => r.latestMark.nonEmpty || r.latestGrade.nonEmpty) =>
                member.agreedMark.isEmpty && member.agreedGrade.isEmpty

              // State is not agreed but UAGM has agreed marks
              case _ => member.agreedMark.nonEmpty || member.agreedGrade.nonEmpty
            } ||
            recordedStudent.flatMap(_.latestMark).exists(m => !member.firstDefinedMark.contains(m)) ||
            recordedStudent.flatMap(_.latestGrade).exists(g => !member.firstDefinedGrade.contains(g))
          ),
        sitsWriteError = recordedStudent.flatMap(_.lastSitsWriteError),
        markState = recordedStudent.flatMap(_.latestState),
        agreed = isAgreedSITS,
        recordedStudent = recordedStudent,
        history = recordedStudent.map(_.marks).getOrElse(Seq.empty),
        member
      )
    }
  }

  def studentMarkRecords(
    info: UpstreamAssessmentGroupInfo,
    assessmentComponentMarksService: AssessmentComponentMarksService,
    resitService: ResitService,
    assessmentMembershipService: AssessmentMembershipService
  ): Seq[StudentMarkRecord] = {
    val recordedStudents =
      assessmentComponentMarksService.getAllRecordedStudents(info.upstreamAssessmentGroup)
        .map { student =>
          (student.universityId, student.assessmentType, student.resitSequence) -> student
        }.toMap

    val resits =
      resitService.getAllResits(info.upstreamAssessmentGroup)
        .filter(_.universityId.nonEmpty)
        .map { resit =>
          (resit.universityId.get, resit.sequence) -> resit
        }.toMap

    val gradeBoundaries = info.upstreamAssessmentGroup.assessmentComponent.map(_.marksCode).map(assessmentMembershipService.markScheme).getOrElse(Nil)
    studentMarkRecords(info, recordedStudents, resits, gradeBoundaries)
  }

  def studentMarkRecords(
    info: UpstreamAssessmentGroupInfo,
    recordedStudents: Map[(String, UpstreamAssessmentGroupMemberAssessmentType, Option[String]), RecordedAssessmentComponentStudent],
    resits: Map[(String, String), RecordedResit],
    gradeBoundaries: Seq[GradeBoundary]
  ): Seq[StudentMarkRecord] =
    info.allMembers.sortBy { uagm => (uagm.universityId, uagm.resitSequence.getOrElse("000")) }.map { member =>
      val recordedStudent = recordedStudents.get((member.universityId, member.assessmentType, member.resitSequence))
      val existingResit = resits.get((member.universityId, member.upstreamAssessmentGroup.sequence))
        .filterNot(r => member.resitSequence.exists(r.resitSequence.contains))
      val gradeBoundary = {
        val process = if (member.isReassessment) GradeBoundaryProcess.Reassessment else GradeBoundaryProcess.StudentAssessment
        val grade = recordedStudent.flatMap(_.latestGrade)
        grade.flatMap(g => gradeBoundaries.find(gb => gb.grade == g && gb.process == process))
      }
      StudentMarkRecord(info, member, recordedStudent, existingResit, gradeBoundary)
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
    val sitsWriteError: Option[RecordedAssessmentComponentStudentMarkSitsError] = students.flatMap(_.sitsWriteError).headOption
    val allAgreed: Boolean = students.nonEmpty && students.forall(_.agreed)
  }
  type Result = Seq[AssessmentComponentInfo]
  type Command = Appliable[Result]

  val AdminPermission: Permission = Permissions.Feedback.Manage

  def apply(department: Department, academicYear: AcademicYear, currentUser: CurrentUser): Command =
    new ListAssessmentComponentsCommandInternal(department, academicYear, currentUser)
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringResitServiceComponent
      with AutowiringSecurityServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringMarksWorkflowProgressServiceComponent
      with AutowiringModuleRegistrationServiceComponent
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
    with ResitServiceComponent
    with AssessmentMembershipServiceComponent
    with MarksWorkflowProgressServiceComponent
    with ModuleRegistrationServiceComponent
    with ListAssessmentComponentsModulesWithPermission =>

  override def applyInternal(): Result = assessmentComponentInfos

}

trait ListAssessmentComponentsForModulesWithPermission extends TaskBenchmarking {
  self: ListAssessmentComponentsState
    with AssessmentMembershipServiceComponent
    with AssessmentComponentMarksServiceComponent
    with ResitServiceComponent
    with MarksWorkflowProgressServiceComponent
    with ModuleRegistrationServiceComponent
    with ListAssessmentComponentsModulesWithPermission =>

  lazy val allModuleRegistrations: Map[(String, AcademicYear, String), Seq[ModuleRegistration]] = benchmarkTask("Load all module registrations for department") {
    moduleRegistrationService.getByDepartmentAndYear(department, academicYear)
      .groupBy(mr => (mr.sitsModuleCode, mr.academicYear, mr.occurrence))
  }

  lazy val assessmentComponentInfos: Seq[AssessmentComponentInfo] = {
    val assessmentComponents: Seq[AssessmentComponent] = benchmarkTask("Get assessment components") {
      assessmentMembershipService.getAssessmentComponents(department, includeSubDepartments = false, inUseOnly = false)
        .filter { ac =>
          ac.sequence != AssessmentComponent.NoneAssessmentGroup &&
          (canAdminDepartment || modulesWithPermission.contains(ac.module))
        }
    }

    val assessmentComponentsByKey: Map[AssessmentComponentKey, AssessmentComponent] = benchmarkTask("Group assessment components") {
      assessmentComponents.map { ac =>
        AssessmentComponentKey(ac) -> ac
      }.toMap
    }

    val upstreamAssessmentGroupInfos: Seq[UpstreamAssessmentGroupInfo] = benchmarkTask("Get UAG infos for components") {
      assessmentMembershipService.getUpstreamAssessmentGroupInfoForComponents(assessmentComponents, academicYear)
        .filter(_.allMembers.nonEmpty)
    }
    val recordedStudentsByGroup: Map[UpstreamAssessmentGroup, Map[(String, UpstreamAssessmentGroupMemberAssessmentType, Option[String]), RecordedAssessmentComponentStudent]] = benchmarkTask("Get all recorded students") {
      assessmentComponentMarksService.getAllRecordedStudentsByGroup(upstreamAssessmentGroupInfos.map(_.upstreamAssessmentGroup).distinct)
        .view
        .mapValues { students =>
          students.map { student =>
            (student.universityId, student.assessmentType, student.resitSequence) -> student
          }.toMap
        }.toMap
    }

    val resitsByGroup: Map[UpstreamAssessmentGroup, Map[(String, String), RecordedResit]] = benchmarkTask("Get all recorded resits") {
      resitService.getAllResitsByGroup(upstreamAssessmentGroupInfos.map(_.upstreamAssessmentGroup).distinct)
        .view
        .mapValues { resits =>
          resits.filter(_.universityId.nonEmpty).groupBy(r => (r.universityId.get, r.sequence))
            .view
            .mapValues(_.maxBy(_.currentResitAttempt.value))
            .toMap
        }.toMap
    }

    val gradeBoundariesByMarksCode: Map[String, Seq[GradeBoundary]] = benchmarkTask("Get all grade boundaries") {
      upstreamAssessmentGroupInfos.flatMap(_.upstreamAssessmentGroup.assessmentComponent.map(_.marksCode)).distinct.map { marksCode =>
        marksCode -> assessmentMembershipService.markScheme(marksCode)
      }.toMap
    }

    upstreamAssessmentGroupInfos
      .map { upstreamAssessmentGroupInfo => benchmarkTask(s"Process ${upstreamAssessmentGroupInfo.upstreamAssessmentGroup}") {
        val assessmentComponent = assessmentComponentsByKey(AssessmentComponentKey(upstreamAssessmentGroupInfo.upstreamAssessmentGroup))
        val upstreamAssessmentGroup = upstreamAssessmentGroupInfo.upstreamAssessmentGroup
        val students: Seq[StudentMarkRecord] = benchmarkTask("Get student mark records") {
          val recordedStudents = recordedStudentsByGroup.getOrElse(upstreamAssessmentGroup, Map.empty)
          val resits = resitsByGroup.getOrElse(upstreamAssessmentGroup, Map.empty)
          val gradeBoundaries = upstreamAssessmentGroup.assessmentComponent.map(_.marksCode).flatMap(gradeBoundariesByMarksCode.get).getOrElse(Seq.empty)

          studentMarkRecords(upstreamAssessmentGroupInfo, recordedStudents, resits, gradeBoundaries)
        }

        val moduleRegistrations = allModuleRegistrations.getOrElse((assessmentComponent.moduleCode, academicYear, upstreamAssessmentGroup.occurrence), Seq.empty).sortBy(_.sprCode)
        val progress = benchmarkTask("Progress") { workflowProgressService.componentProgress(assessmentComponent, upstreamAssessmentGroup, students, moduleRegistrations) }

        AssessmentComponentInfo(
          assessmentComponent,
          upstreamAssessmentGroup,
          students,
          progress = MarksWorkflowProgress(progress.percentage, progress.cssClass, progress.messageCode),
          nextStage = progress.nextStage,
          stages = progress.stages,
        )
      }}
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
