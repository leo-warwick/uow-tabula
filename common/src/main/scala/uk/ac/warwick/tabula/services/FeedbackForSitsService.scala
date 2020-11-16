package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.{ClearRecordedModuleMarks, ClearRecordedModuleMarksState, ListAssessmentComponentsCommand, RecordAssessmentComponentMarksPermissions, RecordAssessmentComponentMarksState}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.MarkState.UnconfirmedActual
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent, AutowiringModuleRegistrationMarksServiceComponent}

case class ValidateAndPopulateFeedbackResult(
  valid: Seq[Feedback],
  populated: Map[Feedback, String],
  zero: Map[Feedback, String],
  invalid: Map[Feedback, String],
  notOnScheme: Map[Feedback, String],
  invalidLinkedToMultipleComponents: Seq[Feedback]
)

trait FeedbackForSitsService {
  def getByFeedback(feedback: Feedback): Option[FeedbackForSits]

  def getByFeedbacks(feedbacks: Seq[Feedback]): Map[Feedback, FeedbackForSits]

  def queueFeedback(feedback: Feedback, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks): Option[FeedbackForSits]

  def validateAndPopulateFeedback(feedbacks: Seq[Feedback], assessment: Assignment, gradeGenerator: GeneratesGradesFromMarks): ValidateAndPopulateFeedbackResult
}

trait FeedbackForSitsServiceComponent {
  def feedbackForSitsService: FeedbackForSitsService
}

trait AutowiringFeedbackForSitsServiceComponent extends FeedbackForSitsServiceComponent {
  var feedbackForSitsService: FeedbackForSitsService = Wire[FeedbackForSitsService]
}

abstract class AbstractFeedbackForSitsService extends FeedbackForSitsService {
  self: AssessmentMembershipServiceComponent
    with AssessmentComponentMarksServiceComponent
    with FeedbackServiceComponent =>

  private def getUpstreamAssessmentGroupMembers(feedback: Feedback): Seq[UpstreamAssessmentGroupMember] =
  // feedback.assessmentGroups only includes those where UAGI exists and members contains a matching uni ID, but doesn't verify reassessment
    feedback.assessmentGroups.map(_.toUpstreamAssessmentGroupInfo(feedback.academicYear).get)
      .flatMap { info =>
        if (feedback.assignment.resitAssessment) {
          info.allMembers.filter { uagm =>
            feedback.universityId.contains(uagm.universityId) && uagm.isReassessment
          }.maxByOption(_.resitSequence)
        } else {
          info.allMembers.find { uagm =>
            feedback.universityId.contains(uagm.universityId) && !uagm.isReassessment
          }
        }
      }

  override def getByFeedback(feedback: Feedback): Option[FeedbackForSits] = {
    // feedback.assessmentGroups only includes those where UAGI exists and members contains a matching uni ID, but doesn't verify reassessment
    val upstreamAssessmentGroupMembers: Seq[UpstreamAssessmentGroupMember] =
      getUpstreamAssessmentGroupMembers(feedback)

    val recordedAssessmentComponentStudents: Seq[RecordedAssessmentComponentStudent] =
      upstreamAssessmentGroupMembers.flatMap(assessmentComponentMarksService.getRecordedStudent)

    if (recordedAssessmentComponentStudents.nonEmpty) {
      Some(FeedbackForSits(feedback, recordedAssessmentComponentStudents))
    } else {
      None
    }
  }

  override def getByFeedbacks(feedbacks: Seq[Feedback]): Map[Feedback, FeedbackForSits] =
    feedbacks.flatMap { feedback =>
      getByFeedback(feedback).map(feedback -> _)
    }.toMap

  override def queueFeedback(feedback: Feedback, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks): Option[FeedbackForSits] = transactional() {
    val validatedFeedback = validateAndPopulateFeedback(Seq(feedback), feedback.assignment, gradeGenerator)
    if (validatedFeedback.valid.nonEmpty || feedback.module.adminDepartment.assignmentGradeValidation && validatedFeedback.populated.nonEmpty) {
      if (validatedFeedback.populated.nonEmpty) {
        if (feedback.latestPrivateOrNonPrivateAdjustment.isDefined) {
          feedback.latestPrivateOrNonPrivateAdjustment.foreach(m => {
            m.grade = Some(validatedFeedback.populated(feedback))
            feedbackService.saveOrUpdate(m)
          })
        } else {
          feedback.actualGrade = Some(validatedFeedback.populated(feedback))
        }
      }
      feedbackService.saveOrUpdate(feedback)

      val recordedAssessmentComponentStudents: Seq[RecordedAssessmentComponentStudent] =
        getUpstreamAssessmentGroupMembers(feedback).map { uagm =>
          ExportFeedbackToSitsCommand(feedback, uagm, submitter).apply()
        }

      Some(FeedbackForSits(feedback, recordedAssessmentComponentStudents))
    } else {
      None
    }
  }

  override def validateAndPopulateFeedback(feedbacks: Seq[Feedback], assessment: Assignment, gradeGenerator: GeneratesGradesFromMarks): ValidateAndPopulateFeedbackResult = {

    val studentsMarks = (for (f <- feedbacks; mark <- f.latestMark; uniId <- f.universityId) yield {
      uniId -> mark
    }).toMap

    val validGrades = gradeGenerator.applyForMarks(studentsMarks)

    val invalidLinkedToMultipleComponents = feedbacks.filter(_.assessmentGroups.size > 1)

    val parsedFeedbacks = feedbacks.filter(_.universityId.isDefined).groupBy(f => {
      val upstreamAssessmentGroupMembersForFeedback = getUpstreamAssessmentGroupMembers(f)
      f.latestGrade match {
        case _ if upstreamAssessmentGroupMembersForFeedback.isEmpty => "notOnScheme"
        case Some(_) if f.latestMark.isEmpty => "invalid" // a grade without a mark is invalid
        case Some(grade) =>
          if (validGrades(f._universityId).isEmpty || !validGrades(f._universityId).exists(_.grade == grade))
            "invalid"
          else
            "valid"
        case None =>
          if (f.module.adminDepartment.assignmentGradeValidation) {
            if (f.latestMark.contains(0)) {
              "zero"
            } else if (validGrades.contains(f._universityId) && validGrades(f._universityId).exists(_.isDefault)) {
              "populated"
            } else {
              "invalid"
            }
          } else {
            "invalid"
          }
      }
    })

    val validFeedbacks = parsedFeedbacks.getOrElse("valid", Seq()).filterNot { validItem =>
      invalidLinkedToMultipleComponents.contains(validItem)
    }
    val populatedFeedback = parsedFeedbacks.getOrElse("populated", Seq()).filterNot { validItem =>
      invalidLinkedToMultipleComponents.contains(validItem)
    }.map(f1 => f1 -> validGrades(f1._universityId).find(_.isDefault).map(_.grade).get).toMap

    ValidateAndPopulateFeedbackResult(
      validFeedbacks,
      populatedFeedback,
      parsedFeedbacks.get("zero").map(feedbacksToPopulate =>
        feedbacksToPopulate.map(f => f -> validGrades.get(f._universityId).map(_.map(_.grade).mkString(", ")).getOrElse("")).toMap
      ).getOrElse(Map()),
      parsedFeedbacks.get("invalid").map(feedbacksToPopulate =>
        feedbacksToPopulate.map(f => f -> validGrades.get(f._universityId).map(_.map(_.grade).mkString(", ")).getOrElse("")).toMap
      ).getOrElse(Map()),
      parsedFeedbacks.get("notOnScheme").map(feedbacksToPopulate =>
        feedbacksToPopulate.map(f => f -> validGrades.get(f._universityId).map(_.map(_.grade).mkString(", ")).getOrElse("")).toMap
      ).getOrElse(Map()),
      invalidLinkedToMultipleComponents
    )
  }

}

object ExportFeedbackToSitsCommand {
  type Command = Appliable[RecordedAssessmentComponentStudent]

  def apply(feedback: Feedback, upstreamAssessmentGroupMember: UpstreamAssessmentGroupMember, currentUser: CurrentUser): Command =
    new ExportFeedbackToSitsCommandInternal(feedback, upstreamAssessmentGroupMember, currentUser)
      with ComposableCommand[RecordedAssessmentComponentStudent]
      with RecordAssessmentComponentMarksPermissions
      with ExportFeedbackToSitsDescription
      with ClearRecordedModuleMarks
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringTransactionalComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringModuleRegistrationServiceComponent
}

abstract class ExportFeedbackToSitsCommandInternal(feedback: Feedback, upstreamAssessmentGroupMember: UpstreamAssessmentGroupMember, val currentUser: CurrentUser)
  extends CommandInternal[RecordedAssessmentComponentStudent]
    with RecordAssessmentComponentMarksState
    with ClearRecordedModuleMarksState {
  self: AssessmentComponentMarksServiceComponent
    with TransactionalComponent
    with ClearRecordedModuleMarks =>

  def upstreamAssessmentGroup: UpstreamAssessmentGroup = upstreamAssessmentGroupMember.upstreamAssessmentGroup

  def assessmentComponent: AssessmentComponent = upstreamAssessmentGroup.assessmentComponent.get

  override def applyInternal(): RecordedAssessmentComponentStudent = transactional() {
    val recordedStudent = assessmentComponentMarksService.getRecordedStudent(upstreamAssessmentGroupMember)
    lazy val recordedAssessmentComponentStudent = recordedStudent.getOrElse(assessmentComponentMarksService.getOrCreateRecordedStudent(upstreamAssessmentGroupMember))

    // We just use this to calculate whether the mark has changed in the feedback so no need to worry about the params
    val studentMarkRecord =
      ListAssessmentComponentsCommand.StudentMarkRecord(
        info = UpstreamAssessmentGroupInfo(
          upstreamAssessmentGroup,
          Seq(upstreamAssessmentGroupMember)
        ),
        member = upstreamAssessmentGroupMember,
        recordedStudent = recordedStudent,
        existingResit = None,
        gradeBoundary = None,
      )

    val isUnchanged = {
      !studentMarkRecord.outOfSync &&
      studentMarkRecord.mark == feedback.latestMark &&
      studentMarkRecord.grade == feedback.latestGrade
    }

    if (!isUnchanged) {
      recordedAssessmentComponentStudent.addMark(
        uploader = currentUser.apparentUser,
        mark = feedback.latestMark,
        grade = feedback.latestGrade,
        source = RecordedAssessmentComponentStudentMarkSource.CourseworkMarking,
        markState = recordedAssessmentComponentStudent.latestState.getOrElse(UnconfirmedActual),
      )

      assessmentComponentMarksService.saveOrUpdate(recordedAssessmentComponentStudent)

      // Need to clear the module marks
      clearRecordedModuleMarksFor(recordedAssessmentComponentStudent)
    }

    recordedAssessmentComponentStudent
  }
}

trait ExportFeedbackToSitsDescription extends Describable[RecordedAssessmentComponentStudent] {
  self: RecordAssessmentComponentMarksState =>

  override lazy val eventName: String = "ExportFeedbackToSits"

  override def describe(d: Description): Unit =
    d.assessmentComponent(assessmentComponent)
      .upstreamAssessmentGroup(upstreamAssessmentGroup)

  override def describeResult(d: Description, result: RecordedAssessmentComponentStudent): Unit =
    d.properties(
      "marks" -> Option(result).filter(_.latestMark.nonEmpty).map { student =>
        student.universityId -> student.latestMark.get
      }.toMap,
      "grades" -> Option(result).filter(_.latestGrade.nonEmpty).map { student =>
        student.universityId -> student.latestGrade.get
      }.toMap,
      "state" -> Option(result).filter(_.latestState.nonEmpty).map { student =>
        student.universityId -> student.latestState.get.entryName
      }.toMap
    )
}

@Service("feedbackForSitsService")
class FeedbackForSitsServiceImpl
  extends AbstractFeedbackForSitsService
    with AutowiringAssessmentMembershipServiceComponent
    with AutowiringAssessmentComponentMarksServiceComponent
    with AutowiringFeedbackServiceComponent
