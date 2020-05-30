package uk.ac.warwick.tabula.commands.marks

import javax.validation.constraints.{Max, Min, NotNull}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.marks.ComponentScalingCommand.Result
import uk.ac.warwick.tabula.commands.{Appliable, CommandInternal, ComposableCommand, SelfValidating}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent, ProgressionService}

object ComponentScalingCommand {
  type Result = RecordAssessmentComponentMarksCommand.Result
  type Command = Appliable[Result] with SelfValidating with ComponentScalingRequest with ComponentScalingAlgorithm with MissingMarkAdjustmentStudentsToSet

  def apply(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup, currentUser: CurrentUser): Command =
    new ComponentScalingCommandInternal(assessmentComponent, upstreamAssessmentGroup, currentUser)
      with ComposableCommand[Result]
      with ComponentScalingRequest
      with ComponentScalingAlgorithm
      with ComponentScalingValidation
      with ComponentScalingDescription
      with MissingMarkAdjustmentStudentsToSet
      with RecordAssessmentComponentMarksPermissions
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringTransactionalComponent
}

abstract class ComponentScalingCommandInternal(val assessmentComponent: AssessmentComponent, val upstreamAssessmentGroup: UpstreamAssessmentGroup, currentUser: CurrentUser)
  extends CommandInternal[Result]
    with RecordAssessmentComponentMarksState {
  self: AssessmentComponentMarksServiceComponent
    with ComponentScalingRequest
    with ComponentScalingAlgorithm
    with MissingMarkAdjustmentStudentsToSet
    with TransactionalComponent =>

  // Set the default pass mark depending on the module
  passMark = assessmentComponent.module.degreeType match {
    case DegreeType.Undergraduate => ProgressionService.UndergradPassMark
    case DegreeType.Postgraduate => ProgressionService.PostgraduatePassMark
    case _ => ProgressionService.DefaultPassMark
  }

  override def applyInternal(): Result = transactional() {
    studentsToSet.map { case (upstreamAssessmentGroupMember, mark, grade) =>
      val recordedAssessmentComponentStudent: RecordedAssessmentComponentStudent =
        assessmentComponentMarksService.getOrCreateRecordedStudent(upstreamAssessmentGroupMember)

      val (scaledMark, scaledGrade) = scale(mark, grade, upstreamAssessmentGroupMember.resitExpected.getOrElse(false))

      recordedAssessmentComponentStudent.addMark(
        uploader = currentUser.apparentUser,
        mark = scaledMark,
        grade = scaledGrade,
        comments = comment(mark)
      )

      assessmentComponentMarksService.saveOrUpdate(recordedAssessmentComponentStudent)

      recordedAssessmentComponentStudent
    }
  }
}

trait ComponentScalingRequest {
  var calculate: Boolean = false

  @NotNull
  var passMarkAdjustment: Int = 0

  @NotNull
  var upperClassAdjustment: Int = 0

  @NotNull
  @Min(40)
  @Max(50)
  var passMark: Int = ProgressionService.DefaultPassMark

  def comment(originalMark: Option[Int]): String = s"Assessment component scaled from original mark ${originalMark.getOrElse("-")} (pass mark: $passMark, pass mark adjustment: ${if (passMarkAdjustment > 0) "+" else ""}$passMarkAdjustment, upper class adjustment: ${if (upperClassAdjustment > 0) "+" else ""}$upperClassAdjustment)"
}

// Dave's Amazing Scaling algorithm
trait ComponentScalingAlgorithm {
  self: ComponentScalingRequest
    with RecordAssessmentComponentMarksState
    with AssessmentMembershipServiceComponent =>

  def shouldScale(mark: Option[Int], grade: Option[String]): Boolean = (mark, grade) match {
    case (None, _) => false
    case (_, Some(GradeBoundary.ForceMajeureMissingComponentGrade)) | (_, Some(GradeBoundary.WithdrawnGrade)) => false
    case _ => true
  }

  def scale(mark: Option[Int], grade: Option[String], isResit: Boolean): (Option[Int], Option[String]) =
    if (shouldScale(mark, grade)) {
      val scaledMark = mark.map(scaleMark)
      val scaledGrade =
        assessmentMembershipService.gradesForMark(assessmentComponent, scaledMark, isResit)
          .find(_.isDefault)
          .map(_.grade)
          .orElse(grade) // Use the old grade if necessary (it shouldn't be)

      (scaledMark, scaledGrade)
    } else (mark, grade)

  def scaleMark(mark: Int): Int = {
    require(mark >= 0 && mark <= 100)

    val upperClassThreshold = 70
    val passMarkRange = upperClassThreshold - passMark

    val scaledMark: BigDecimal =
      if (mark <= passMark - passMarkAdjustment) {
        BigDecimal(mark * passMark) / (passMark - passMarkAdjustment)
      } else if (mark >= upperClassThreshold - upperClassAdjustment) {
        BigDecimal((mark * (100 - upperClassThreshold)) + 100 * upperClassAdjustment) / ((100 - upperClassThreshold) + upperClassAdjustment)
      } else {
        passMark + passMarkRange * (BigDecimal(passMarkAdjustment + mark - passMark) / (passMarkRange - upperClassAdjustment + passMarkAdjustment))
      }

    scaledMark.setScale(0, BigDecimal.RoundingMode.HALF_UP).toInt
  }
}

trait ComponentScalingValidation extends SelfValidating {
  self: ComponentScalingRequest
    with MissingMarkAdjustmentStudentsToSet
    with ComponentScalingAlgorithm =>

  override def validate(errors: Errors): Unit = {
    // Everyone must have an existing mark
    val studentsWithMissingMarks = studentsToSet.filter { case (_, latestMark, _) => latestMark.isEmpty }
    val hasChanges = studentsToSet.exists { case (_, mark, grade) => shouldScale(mark, grade) }

    if (studentsWithMissingMarks.nonEmpty) {
      errors.reject("scaling.studentsWithMissingMarks", Array(studentsWithMissingMarks.map(_._1.universityId).mkString(", ")), "")
    } else if (!hasChanges) {
      errors.reject("scaling.noChanges")
    }

    if (calculate && (passMarkAdjustment == 0 && upperClassAdjustment == 0)) {
      errors.rejectValue("upperClassAdjustment", "scaling.noAdjustments")
    }
  }
}

trait ComponentScalingDescription extends RecordAssessmentComponentMarksDescription {
  self: RecordAssessmentComponentMarksState =>

  override lazy val eventName: String = "ComponentScaling"
}
