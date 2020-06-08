package uk.ac.warwick.tabula.services.marks

import enumeratum.{Enum, EnumEntry}
import org.joda.time.LocalDate
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.WorkflowStages.StageProgress
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.{AssessmentComponentInfo, StudentMarkRecord}
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.data.model.MarkState.ConfirmedActual
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, DegreeType, GradeBoundary, UpstreamAssessmentGroup}

@Service
class MarksWorkflowProgressService {
  def componentStages(
    assessmentComponent: AssessmentComponent,
    upstreamAssessmentGroup: UpstreamAssessmentGroup,
  ): Seq[ComponentMarkWorkflowStage] = ComponentMarkWorkflowStage.values.filter(_.applies(assessmentComponent, upstreamAssessmentGroup))

  def componentProgress(
    assessmentComponent: AssessmentComponent,
    upstreamAssessmentGroup: UpstreamAssessmentGroup,
    students: Seq[StudentMarkRecord]
  ): WorkflowProgress = {
    val allStages = componentStages(assessmentComponent, upstreamAssessmentGroup)
    val progresses = allStages.map(_.progress(assessmentComponent, upstreamAssessmentGroup, students))

    WorkflowProgress(progresses, allStages)
  }

  def moduleOccurrenceStages(): Seq[ModuleOccurrenceMarkWorkflowStage] = ModuleOccurrenceMarkWorkflowStage.values

  def moduleOccurrenceProgress(students: Seq[StudentModuleMarkRecord], components: Seq[AssessmentComponentInfo]): WorkflowProgress = {
    val allStages = moduleOccurrenceStages()
    val progresses = allStages.map(_.progress(students, components))

    WorkflowProgress(progresses, allStages)
  }
}

sealed abstract class ModuleOccurrenceMarkWorkflowStage extends WorkflowStage with EnumEntry {
  def progress(students: Seq[StudentModuleMarkRecord], components: Seq[AssessmentComponentInfo]): WorkflowStages.StageProgress
  override val actionCode: String = s"workflow.marks.moduleOccurrence.$entryName.action"
}

object ModuleOccurrenceMarkWorkflowStage extends Enum[ModuleOccurrenceMarkWorkflowStage] {
  case object RecordComponentMarks extends ModuleOccurrenceMarkWorkflowStage {
    override def progress(students: Seq[StudentModuleMarkRecord], components: Seq[AssessmentComponentInfo]): StageProgress = {
      // Infer this from the component stage progress
      val stages = components.map(_.stages(ComponentMarkWorkflowStage.RecordMarks.entryName))

      StageProgress(
        stage = RecordComponentMarks,
        started = true, // This always has to be started in order to get nextAction
        messageCode =
          // Use the message from the most pressing component stage
          stages.find(_.health == WorkflowStageHealth.Danger)
            .orElse(stages.find(_.health == WorkflowStageHealth.Warning))
            .orElse(stages.find(_.completed))
            .orElse(stages.find(_.skipped))
            .orElse(stages.find(_.started))
            .getOrElse(stages.head)
            .messageCode,
        health =
          if (stages.exists(_.health == WorkflowStageHealth.Danger)) WorkflowStageHealth.Danger
          else if (stages.exists(_.health == WorkflowStageHealth.Warning)) WorkflowStageHealth.Warning
          else WorkflowStageHealth.Good,
        skipped = stages.forall(_.skipped),
        completed = stages.exists(_.completed) && stages.forall(s => s.completed || s.skipped),
      )
    }
  }

  case object CalculateModuleMarks extends ModuleOccurrenceMarkWorkflowStage {
    override def progress(students: Seq[StudentModuleMarkRecord], components: Seq[AssessmentComponentInfo]): StageProgress =
      if (students.forall(s => s.mark.isEmpty && s.grade.isEmpty && s.result.isEmpty)) {
        // No marks have been recorded
        StageProgress(
          stage = CalculateModuleMarks,
          started = false,
          messageCode = "workflow.marks.moduleOccurrence.CalculateModuleMarks.notStarted",
          health = WorkflowStageHealth.Warning,
        )
      } else if (students.exists(_.needsWritingToSits)) {
        // Needs writing to SITS
        StageProgress(
          stage = CalculateModuleMarks,
          started = true,
          messageCode = "workflow.marks.moduleOccurrence.CalculateModuleMarks.needsWritingToSits",
        )
      } else if (students.exists(_.outOfSync)) {
        // Out of sync with SITS
        StageProgress(
          stage = CalculateModuleMarks,
          started = true,
          messageCode = "workflow.marks.moduleOccurrence.CalculateModuleMarks.outOfSync",
          health = WorkflowStageHealth.Danger,
        )
      } else if (students.exists(s => s.grade.contains(GradeBoundary.ForceMajeureMissingComponentGrade)) && students.forall(s => s.grade.contains(GradeBoundary.ForceMajeureMissingComponentGrade) || s.grade.contains(GradeBoundary.WithdrawnGrade))) {
        // Component is MMA
        StageProgress(
          stage = CalculateModuleMarks,
          started = true,
          messageCode = "workflow.marks.moduleOccurrence.CalculateModuleMarks.missed",
          skipped = true,
        )
      } else if (students.forall(s => s.mark.nonEmpty || s.grade.nonEmpty || s.result.nonEmpty)) {
        // All marks have been recorded
        StageProgress(
          stage = CalculateModuleMarks,
          started = true,
          messageCode = "workflow.marks.moduleOccurrence.CalculateModuleMarks.completed",
          completed = true,
        )
      } else {
        // Some marks have been recorded
        StageProgress(
          stage = CalculateModuleMarks,
          started = true,
          messageCode = "workflow.marks.moduleOccurrence.CalculateModuleMarks.inProgress",
          health = WorkflowStageHealth.Warning,
        )
      }

    override def preconditions: Seq[Seq[WorkflowStage]] = Seq(Seq(RecordComponentMarks))
  }

  case object ConfirmModuleMarks extends ModuleOccurrenceMarkWorkflowStage {
    override def progress(students: Seq[StudentModuleMarkRecord], components: Seq[AssessmentComponentInfo]): StageProgress =
      if (students.nonEmpty && students.forall(s => s.agreed || s.markState.contains(ConfirmedActual))) {
        StageProgress(
          stage = ConfirmModuleMarks,
          started = true,
          messageCode = "workflow.marks.moduleOccurrence.ConfirmModuleMarks.completed",
          completed = true,
        )
      } else if (students.exists(_.markState.contains(ConfirmedActual))) {
        StageProgress(
          stage = ConfirmModuleMarks,
          started = true,
          messageCode = "workflow.marks.moduleOccurrence.ConfirmModuleMarks.inProgress",
        )
      } else {
        StageProgress(
          stage = ConfirmModuleMarks,
          started = false,
          messageCode = "workflow.marks.moduleOccurrence.ConfirmModuleMarks.notStarted",
        )
      }

    override def preconditions: Seq[Seq[WorkflowStage]] = Seq(Seq(CalculateModuleMarks))
  }

  case object ProcessModuleMarks extends ModuleOccurrenceMarkWorkflowStage {
    override def progress(students: Seq[StudentModuleMarkRecord], components: Seq[AssessmentComponentInfo]): StageProgress =
      if (students.nonEmpty && students.forall(_.agreed)) {
        StageProgress(
          stage = ProcessModuleMarks,
          started = true,
          messageCode = "workflow.marks.moduleOccurrence.ProcessModuleMarks.completed",
          completed = true,
        )
      } else if (students.exists(_.agreed)) {
        StageProgress(
          stage = ProcessModuleMarks,
          started = true,
          messageCode = "workflow.marks.moduleOccurrence.ProcessModuleMarks.inProgress",
        )
      } else {
        StageProgress(
          stage = ProcessModuleMarks,
          started = false,
          messageCode = "workflow.marks.moduleOccurrence.ProcessModuleMarks.notStarted",
        )
      }

    override def preconditions: Seq[Seq[WorkflowStage]] = Seq(Seq(CalculateModuleMarks), Seq(ConfirmModuleMarks))
  }

  override val values: IndexedSeq[ModuleOccurrenceMarkWorkflowStage] = findValues
}

sealed abstract class ComponentMarkWorkflowStage extends WorkflowStage with EnumEntry {
  def applies(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup): Boolean = true
  def progress(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup, students: Seq[StudentMarkRecord]): WorkflowStages.StageProgress
  override val actionCode: String = s"workflow.marks.component.$entryName.action"
}

object ComponentMarkWorkflowStage extends Enum[ComponentMarkWorkflowStage] {
  case object SetDeadline extends ComponentMarkWorkflowStage {
    // Only need deadlines for 19/20, and only for UG modules
    override def applies(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup): Boolean = {
      upstreamAssessmentGroup.academicYear == AcademicYear.starting(2019) &&
      Option(assessmentComponent.module.degreeType).forall(_ == DegreeType.Undergraduate)
    }

    override def progress(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup, students: Seq[StudentMarkRecord]): WorkflowStages.StageProgress =
      if (upstreamAssessmentGroup.deadline.isEmpty) {
        StageProgress(
          stage = SetDeadline,
          started = true,
          messageCode = "workflow.marks.component.SetDeadline.notProvided",
          health = WorkflowStageHealth.Danger,
        )
      } else {
        StageProgress(
          stage = SetDeadline,
          started = true,
          messageCode = "workflow.marks.component.SetDeadline.provided",
          completed = true,
        )
      }
  }

  case object RecordMarks extends ComponentMarkWorkflowStage {
    override def progress(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup, students: Seq[StudentMarkRecord]): WorkflowStages.StageProgress = {
      lazy val neededForGraduateBenchmark =
        upstreamAssessmentGroup.academicYear == AcademicYear.starting(2019) &&
        Option(assessmentComponent.module.degreeType).forall(_ == DegreeType.Undergraduate) &&
        upstreamAssessmentGroup.deadline.nonEmpty

      if (students.forall(s => s.mark.isEmpty && s.grade.isEmpty)) {
        val isInPast = upstreamAssessmentGroup.deadline.exists(_.isBefore(LocalDate.now()))

        // No marks have been recorded
        StageProgress(
          stage = RecordMarks,
          started = !neededForGraduateBenchmark || upstreamAssessmentGroup.deadline.nonEmpty,
          messageCode = "workflow.marks.component.RecordMarks.notStarted",
          health =
            if (neededForGraduateBenchmark) WorkflowStageHealth.Danger
            else if (isInPast) WorkflowStageHealth.Warning
            else WorkflowStageHealth.Good,
        )
      } else if (students.exists(_.needsWritingToSits)) {
        // Needs writing to SITS
        StageProgress(
          stage = RecordMarks,
          started = true,
          messageCode = "workflow.marks.component.RecordMarks.needsWritingToSits",
        )
      } else if (students.exists(_.outOfSync)) {
        // Out of sync with SITS
        StageProgress(
          stage = RecordMarks,
          started = true,
          messageCode = "workflow.marks.component.RecordMarks.outOfSync",
          health = WorkflowStageHealth.Danger,
        )
      } else if (students.exists(s => s.grade.contains(GradeBoundary.ForceMajeureMissingComponentGrade)) && students.forall(s => s.grade.contains(GradeBoundary.ForceMajeureMissingComponentGrade) || s.grade.contains(GradeBoundary.WithdrawnGrade))) {
        // Component is MMA
        StageProgress(
          stage = RecordMarks,
          started = true,
          messageCode = "workflow.marks.component.RecordMarks.missed",
          skipped = true,
        )
      } else if (students.forall(s => s.mark.nonEmpty || s.grade.nonEmpty)) {
        // All marks have been recorded
        StageProgress(
          stage = RecordMarks,
          started = true,
          messageCode = "workflow.marks.component.RecordMarks.completed",
          completed = true,
        )
      } else {
        // Some marks have been recorded
        StageProgress(
          stage = RecordMarks,
          started = true,
          messageCode = "workflow.marks.component.RecordMarks.inProgress",
          health = if (neededForGraduateBenchmark) WorkflowStageHealth.Danger else WorkflowStageHealth.Warning,
        )
      }
    }
  }

  case object ConfirmMarks extends ComponentMarkWorkflowStage {
    override def progress(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup, students: Seq[StudentMarkRecord]): StageProgress =
      if (students.nonEmpty && students.forall(s => s.agreed || s.markState.contains(ConfirmedActual))) {
        StageProgress(
          stage = ConfirmMarks,
          started = true,
          messageCode = "workflow.marks.component.ConfirmMarks.completed",
          completed = true,
        )
      } else if (students.exists(_.markState.contains(ConfirmedActual))) {
        StageProgress(
          stage = ConfirmMarks,
          started = true,
          messageCode = "workflow.marks.component.ConfirmMarks.inProgress",
        )
      } else {
        StageProgress(
          stage = ConfirmMarks,
          started = false,
          messageCode = "workflow.marks.component.ConfirmMarks.notStarted",
        )
      }

    override def preconditions: Seq[Seq[WorkflowStage]] = Seq(Seq(RecordMarks))
  }

  case object ProcessMarks extends ComponentMarkWorkflowStage {
    override def progress(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup, students: Seq[StudentMarkRecord]): StageProgress =
      if (students.nonEmpty && students.forall(_.agreed)) {
        StageProgress(
          stage = ProcessMarks,
          started = true,
          messageCode = "workflow.marks.component.ProcessMarks.completed",
          completed = true,
        )
      } else if (students.exists(_.agreed)) {
        StageProgress(
          stage = ProcessMarks,
          started = true,
          messageCode = "workflow.marks.component.ProcessMarks.inProgress",
        )
      } else {
        StageProgress(
          stage = ProcessMarks,
          started = false,
          messageCode = "workflow.marks.component.ProcessMarks.notStarted",
        )
      }

    override def preconditions: Seq[Seq[WorkflowStage]] = Seq(Seq(ConfirmMarks))
  }

  override val values: IndexedSeq[ComponentMarkWorkflowStage] = findValues
}

trait MarksWorkflowProgressServiceComponent {
  def workflowProgressService: MarksWorkflowProgressService
}

trait AutowiringMarksWorkflowProgressServiceComponent extends MarksWorkflowProgressServiceComponent {
  var workflowProgressService: MarksWorkflowProgressService = Wire[MarksWorkflowProgressService]
}



