package uk.ac.warwick.tabula.commands.marks

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.marks.MissingMarkAdjustmentCommand.Result
import uk.ac.warwick.tabula.commands.{Appliable, CommandInternal, ComposableCommand, SelfValidating}
import uk.ac.warwick.tabula.data.model.MarkState.UnconfirmedActual
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.services.AutowiringModuleRegistrationServiceComponent
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent, AutowiringModuleRegistrationMarksServiceComponent}

import scala.jdk.CollectionConverters._

object MissingMarkAdjustmentCommand {
  type Result = RecordAssessmentComponentMarksCommand.Result
  type Command = Appliable[Result] with SelfValidating with MissingMarkAdjustmentStudentsToSet

  def apply(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup, currentUser: CurrentUser): Command =
    new MissingMarkAdjustmentCommandInternal(assessmentComponent, upstreamAssessmentGroup, currentUser)
      with ComposableCommand[Result]
      with MissingMarkAdjustmentValidation
      with MissingMarkAdjustmentDescription
      with MissingMarkAdjustmentStudentsToSet
      with RecordAssessmentComponentMarksPermissions
      with ClearRecordedModuleMarks
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringTransactionalComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringModuleRegistrationServiceComponent
}

abstract class MissingMarkAdjustmentCommandInternal(val assessmentComponent: AssessmentComponent, val upstreamAssessmentGroup: UpstreamAssessmentGroup, val currentUser: CurrentUser)
  extends CommandInternal[Result]
    with RecordAssessmentComponentMarksState
    with ClearRecordedModuleMarksState {
  self: AssessmentComponentMarksServiceComponent
    with MissingMarkAdjustmentStudentsToSet
    with TransactionalComponent
    with ClearRecordedModuleMarks =>

  override def applyInternal(): Result = transactional() {
    studentsToSet.map { case (upstreamAssessmentGroupMember, _, _) =>
      val recordedAssessmentComponentStudent: RecordedAssessmentComponentStudent =
        assessmentComponentMarksService.getOrCreateRecordedStudent(upstreamAssessmentGroupMember)

      recordedAssessmentComponentStudent.addMark(
        uploader = currentUser.apparentUser,
        mark = None,
        grade = Some(GradeBoundary.ForceMajeureMissingComponentGrade),
        source = RecordedAssessmentComponentStudentMarkSource.MissingMarkAdjustment,
        markState = recordedAssessmentComponentStudent.latestState.getOrElse(UnconfirmedActual),
        comments = "Assessment did not take place because of force majeure",
      )

      assessmentComponentMarksService.saveOrUpdate(recordedAssessmentComponentStudent)

      clearRecordedModuleMarksFor(recordedAssessmentComponentStudent)

      recordedAssessmentComponentStudent
    }
  }
}

trait MissingMarkAdjustmentStudentsToSet {
  self: RecordAssessmentComponentMarksState
    with AssessmentComponentMarksServiceComponent =>

  // All students in this existing group and their latest mark and grade and whether they're agreed
  lazy val allStudents: Seq[(UpstreamAssessmentGroupMember, Option[Int], Option[String], Boolean)] = {
    val allRecordedStudents = assessmentComponentMarksService.getAllRecordedStudents(upstreamAssessmentGroup)

    upstreamAssessmentGroup.members.asScala.map { upstreamAssessmentGroupMember =>
      val latestMark =
        allRecordedStudents.find(_.matchesIdentity(upstreamAssessmentGroupMember))
          .flatMap(_.latestMark)
          .orElse(upstreamAssessmentGroupMember.firstDefinedMark)

      val latestGrade =
        allRecordedStudents.find(_.matchesIdentity(upstreamAssessmentGroupMember))
          .flatMap(_.latestGrade)
          .orElse(upstreamAssessmentGroupMember.firstDefinedGrade)

      val isAgreed =
        upstreamAssessmentGroupMember.isAgreedMark ||
        upstreamAssessmentGroupMember.isAgreedGrade ||
        allRecordedStudents.find(_.matchesIdentity(upstreamAssessmentGroupMember)).flatMap(_.latestState).contains(MarkState.Agreed)

      (upstreamAssessmentGroupMember, latestMark, latestGrade, isAgreed)
    }.toSeq.sortBy(_._1.universityId)
  }

  // We don't let this happen if there are any existing student marks other 0/W or if it's a no-op
  lazy val studentsToSet: Seq[(UpstreamAssessmentGroupMember, Option[Int], Option[String])] =
    allStudents.filterNot { case (_, latestMark, latestGrade, isAgreed) =>
      isAgreed ||
      (latestMark.contains(0) && latestGrade.contains(GradeBoundary.WithdrawnGrade)) ||
      (latestMark.isEmpty && latestGrade.contains(GradeBoundary.ForceMajeureMissingComponentGrade))
    }.map { case (uagm, latestMark, latestGrade, _) => (uagm, latestMark, latestGrade) }

}

trait MissingMarkAdjustmentValidation extends SelfValidating {
  self: MissingMarkAdjustmentStudentsToSet =>

  override def validate(errors: Errors): Unit = {
    val studentsWithExistingMarks = allStudents.filter { case (_, latestMark, latestGrade, isAgreed) =>
      !isAgreed && (latestMark.exists(_ > 0) || latestGrade.exists(g => g != GradeBoundary.WithdrawnGrade && g != GradeBoundary.ForceMajeureMissingComponentGrade))
    }

    val hasChanges = studentsToSet.nonEmpty

    if (studentsWithExistingMarks.nonEmpty) {
      errors.reject("missingMarks.studentsWithMarks", Array(studentsWithExistingMarks.map(_._1.universityId).mkString(", ")), "")
    } else if (!hasChanges) {
      errors.reject("missingMarks.noChanges")
    }
  }
}

trait MissingMarkAdjustmentDescription extends RecordAssessmentComponentMarksDescription {
  self: RecordAssessmentComponentMarksState =>

  override lazy val eventName: String = "MissingMarkAdjustment"
}
