package uk.ac.warwick.tabula.commands.marks

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.AssessmentComponentInfo
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.marks._
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringSecurityServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, WorkflowStage, WorkflowStages}

import scala.collection.immutable.ListMap

object MarksDepartmentHomeCommand {
  case class MarksWorkflowProgress(percentage: Int, t: String, messageCode: String)

  case class ModuleOccurrence(
    moduleCode: String,
    module: Module,
    cats: BigDecimal,
    occurrence: String,

    // Progress
    progress: MarksWorkflowProgress,
    nextStage: Option[WorkflowStage],
    stages: ListMap[String, WorkflowStages.StageProgress],

    // Assessment components grouped by assessment group
    assessmentComponents: Seq[(String, Seq[AssessmentComponentInfo])],
  )

  case class StudentModuleMarkRecord(
    scjCode: String,
    mark: Option[Int],
    grade: Option[String],
    result: Option[ModuleResult],
    needsWritingToSits: Boolean,
    outOfSync: Boolean,
    agreed: Boolean,
    history: Seq[RecordedModuleMark] // Most recent first
  )
  object StudentModuleMarkRecord {
    def apply(moduleRegistration: ModuleRegistration, recordedModuleRegistration: Option[RecordedModuleRegistration]): StudentModuleMarkRecord =
      StudentModuleMarkRecord(
        scjCode = moduleRegistration._scjCode,
        mark =
          recordedModuleRegistration.filter(_.needsWritingToSits).flatMap(_.latestMark)
            .orElse(moduleRegistration.agreedMark)
            .orElse(recordedModuleRegistration.flatMap(_.latestMark))
            .orElse(moduleRegistration.firstDefinedMark),
        grade =
          recordedModuleRegistration.filter(_.needsWritingToSits).flatMap(_.latestGrade)
            .orElse(moduleRegistration.agreedGrade)
            .orElse(recordedModuleRegistration.flatMap(_.latestGrade))
            .orElse(moduleRegistration.firstDefinedGrade),
        result =
          recordedModuleRegistration.filter(_.needsWritingToSits).flatMap(_.latestResult)
            .orElse(Option(moduleRegistration.moduleResult))
            .orElse(recordedModuleRegistration.flatMap(_.latestResult)),
        needsWritingToSits = recordedModuleRegistration.exists(_.needsWritingToSits),
        outOfSync = recordedModuleRegistration.exists(!_.needsWritingToSits) && (
          recordedModuleRegistration.flatMap(_.latestMark).exists(m => !moduleRegistration.firstDefinedMark.contains(m)) ||
            recordedModuleRegistration.flatMap(_.latestGrade).exists(g => !moduleRegistration.firstDefinedGrade.contains(g))
        ),
        agreed = recordedModuleRegistration.forall(!_.needsWritingToSits) && moduleRegistration.agreedMark.nonEmpty,
        history = recordedModuleRegistration.map(_.marks).getOrElse(Seq.empty),
      )
  }

  def studentModuleMarkRecords(module: Module, cats: BigDecimal, academicYear: AcademicYear, occurrence: String, moduleRegistrations: Seq[ModuleRegistration], moduleRegistrationMarksService: ModuleRegistrationMarksService): Seq[StudentModuleMarkRecord] = {
    val recordedModuleRegistrations = moduleRegistrationMarksService.getAllRecordedModuleRegistrations(module, cats, academicYear, occurrence)

    moduleRegistrations.sortBy(_._scjCode).map { moduleRegistration =>
      val recordedModuleRegistration = recordedModuleRegistrations.find(_.scjCode == moduleRegistration._scjCode)

      StudentModuleMarkRecord(moduleRegistration, recordedModuleRegistration)
    }
  }

  type Result = Seq[ModuleOccurrence]
  type Command = Appliable[Result]

  def apply(department: Department, academicYear: AcademicYear, currentUser: CurrentUser): Command =
    new MarksDepartmentHomeCommandInternal(department, academicYear, currentUser)
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

abstract class MarksDepartmentHomeCommandInternal(val department: Department, val academicYear: AcademicYear, val currentUser: CurrentUser)
  extends CommandInternal[Result]
    with ListAssessmentComponentsState
    with ListAssessmentComponentsForModulesWithPermission {
  self: AssessmentComponentMarksServiceComponent
    with AssessmentMembershipServiceComponent
    with MarksWorkflowProgressServiceComponent
    with ListAssessmentComponentsModulesWithPermission =>

  override def applyInternal(): Result = {
    assessmentComponentInfos
      .groupBy { info => (info.assessmentComponent.moduleCode, info.upstreamAssessmentGroup.occurrence) }
      .map { case ((moduleCode, occurrence), infos) =>
        val progress = workflowProgressService.moduleOccurrenceProgress(infos)

        ModuleOccurrence(
          moduleCode = moduleCode,
          module = infos.head.assessmentComponent.module,
          cats = infos.head.assessmentComponent.cats.map(BigDecimal(_).setScale(1, BigDecimal.RoundingMode.HALF_UP)).get,
          occurrence = occurrence,

          progress = MarksWorkflowProgress(progress.percentage, progress.cssClass, progress.messageCode),
          nextStage = progress.nextStage,
          stages = progress.stages,

          assessmentComponents =
            infos.groupBy(_.assessmentComponent.assessmentGroup)
              .toSeq
              .sortBy { case (assessmentGroup, _) => assessmentGroup },
        )
      }
      .toSeq.sortBy { mo => (mo.moduleCode, mo.occurrence) }
  }
}
