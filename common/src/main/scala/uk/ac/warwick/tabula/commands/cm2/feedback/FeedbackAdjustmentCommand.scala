package uk.ac.warwick.tabula.commands.cm2.feedback

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.cm2.Cm2StudentFeedbackAdjustmentNotification
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

object FeedbackAdjustmentCommand {

  final val REASON_SIZE_LIMIT = 600

  def apply(thisAssignment: Assignment, student: User, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
    new FeedbackAdjustmentCommandInternal(thisAssignment, student, submitter, gradeGenerator)
      with CopiesMarkToFeedback
      with ComposableCommand[Feedback]
      with FeedbackAdjustmentCommandPermissions
      with FeedbackAdjustmentCommandDescription
      with FeedbackAdjustmentCommandValidation
      with FeedbackAdjustmentNotifier
      with AutowiringFeedbackServiceComponent
      with AutowiringZipServiceComponent
      with AutowiringFeedbackForSitsServiceComponent
      with AutowiringProfileServiceComponent
      with QueuesFeedbackForSits
      with FeedbackAdjustmentSitsGradeValidation
      with SubmissionState {
      override val submission: Option[Submission] = thisAssignment.findSubmission(student.getUserId)
      override val assignment: Assignment = thisAssignment
    }
}

class FeedbackAdjustmentCommandInternal(val assessment: Assessment, val student: User, val submitter: CurrentUser, val gradeGenerator: GeneratesGradesFromMarks)
  extends CommandInternal[Feedback] with FeedbackAdjustmentCommandState {

  self: FeedbackServiceComponent with ZipServiceComponent with QueuesFeedbackForSits with CopiesMarkToFeedback with FeedbackAdjustmentSitsGradeValidation =>

  val feedback: Feedback = assessment.findFeedback(student.getUserId)
    .getOrElse(throw new ItemNotFoundException("Can't adjust for non-existent feedback"))

  //Allowing PWD upload to SITS as long as record exists there
  lazy val canBeUploadedToSits: Boolean = assessment.assessmentGroups.asScala.map(_.toUpstreamAssessmentGroupInfo(assessment.academicYear)).exists(_.exists(_.upstreamAssessmentGroup.membersIncludes(student)))

  def applyInternal(): Feedback = {
    val newMark = copyTo(feedback)

    assessment match {
      case assignment: Assignment =>
        // if we are updating existing feedback then invalidate any cached feedback zips
        if (feedback.id != null) {
          zipService.invalidateIndividualFeedbackZip(feedback)
        }
      case _ =>
    }

    feedback.updatedDate = DateTime.now
    feedbackService.saveOrUpdate(feedback)
    newMark.foreach(feedbackService.saveOrUpdate)
    if (sendToSits) queueFeedback(feedback, submitter, gradeGenerator)
    feedback
  }

}

trait FeedbackAdjustmentCommandValidation extends SelfValidating {
  self: FeedbackAdjustmentCommandState with FeedbackAdjustmentSitsGradeValidation with CopiesMarkToFeedback =>
  def validate(errors: Errors): Unit = {
    if (!reason.hasText)
      errors.rejectValue("reason", "feedback.adjustment.reason.empty")
    else if (reason.length > FeedbackAdjustmentCommand.REASON_SIZE_LIMIT)
      errors.rejectValue("reason", "feedback.adjustment.reason.tooBig")
    if (!comments.hasText) errors.rejectValue("comments", "feedback.adjustment.comments.empty")
    // validate mark (must be int between 0 and 100)
    if (adjustedMark.hasText) {
      try {
        val asInt = adjustedMark.toInt
        if (asInt < 0 || asInt > 100) {
          errors.rejectValue("adjustedMark", "actualMark.range")
        }
      } catch {
        case _@(_: NumberFormatException | _: IllegalArgumentException) =>
          errors.rejectValue("adjustedMark", "actualMark.format")
      }
    } else {
      errors.rejectValue("adjustedMark", "actualMark.range")
    }

    // validate grade is department setting is true
    if (!errors.hasErrors && adjustedGrade.hasText && assessment.module.adminDepartment.assignmentGradeValidation) {
      val validGrades = gradeGenerator.applyForMarks(Map(student.getWarwickId -> adjustedMark.toInt))(student.getWarwickId)
      if (validGrades.nonEmpty && !validGrades.exists(_.grade == adjustedGrade)) {
        errors.rejectValue("adjustedGrade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
      }
    }

    if (!assessment.collectMarks) {
      errors.rejectValue("adjustedMark", "actualMark.assessmentInvalid")
    }

    if (sendToSits && gradeValidation.exists(_.valid.isEmpty)) {
      errors.reject("feedback.adjustment.invalidSITS")
    }
  }
}

trait FeedbackAdjustmentCommandState {
  val assessment: Assessment
  val student: User
  val feedback: Feedback
  val gradeGenerator: GeneratesGradesFromMarks

  var adjustedMark: String = _
  var adjustedGrade: String = _

  var actualMark: String = _
  var actualGrade: String = _

  var reason: String = _
  var comments: String = _

  val submitter: CurrentUser
  var sendToSits: Boolean = false
}

trait FeedbackAdjustmentCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: FeedbackAdjustmentCommandState =>
  override def permissionsCheck(p: PermissionsChecking): Unit = {
    HibernateHelpers.initialiseAndUnproxy(mandatory(assessment)) match {
      case assignment: Assignment =>
        p.PermissionCheck(Permissions.Feedback.Manage, assignment)
    }
  }
}

trait FeedbackAdjustmentCommandDescription extends Describable[Feedback] {
  self: FeedbackAdjustmentCommandState =>

  override lazy val eventName: String = "FeedbackAdjustment"

  def describe(d: Description): Unit =
    d.feedback(feedback)
     .properties(
       "adjustmentReason" -> reason,
       "adjustmentComments" -> comments
     )
}

trait FeedbackAdjustmentNotifier extends Notifies[Feedback, Feedback] {
  self: FeedbackAdjustmentCommandState =>

  def emit(feedback: Feedback): Seq[NotificationWithTarget[Feedback, Assignment] with SingleItemNotification[Feedback] with AutowiringUserLookupComponent] = {
    if (feedback.released) {
      Seq(Notification.init(new Cm2StudentFeedbackAdjustmentNotification, submitter.apparentUser, feedback, feedback.assignment))
    } else {
      Nil
    }
  }
}

trait CopiesMarkToFeedback {
  self: FeedbackAdjustmentCommandState =>

  var addedMark: Option[Mark] = None

  def copyTo(feedback: Feedback): Option[Mark] = {
    addedMark.orElse {
      // save mark and grade
      if (assessment.collectMarks) {
        addedMark = Some(feedback.addMark(submitter.userId, MarkType.Adjustment, adjustedMark.toInt, adjustedGrade.maybeText, reason, comments))
      }
      addedMark
    }
  }

  def removeMark(): Unit = {
    addedMark.foreach(feedback.marks.remove)
    addedMark = None
  }
}

trait FeedbackAdjustmentGradeValidation {
  val gradeValidation: Option[ValidateAndPopulateFeedbackResult]
}

trait FeedbackAdjustmentSitsGradeValidation extends FeedbackAdjustmentGradeValidation {
  self: FeedbackAdjustmentCommandState with FeedbackForSitsServiceComponent with CopiesMarkToFeedback =>

  lazy val gradeValidation: Option[ValidateAndPopulateFeedbackResult] = {
    try {
      copyTo(feedback)
      val result = feedbackForSitsService.validateAndPopulateFeedback(Seq(feedback), assessment, gradeGenerator)
      removeMark()
      Some(result)
    } catch {
      case _: NumberFormatException => None // adjusted mark is invalid
    }
  }
}
