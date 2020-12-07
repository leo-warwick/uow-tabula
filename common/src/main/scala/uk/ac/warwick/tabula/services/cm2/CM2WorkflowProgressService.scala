package uk.ac.warwick.tabula.services.cm2

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.WorkflowStageHealth._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.markingworkflow.{FinalStage, MarkingWorkflowStage, MarkingWorkflowType, ModerationStage}
import uk.ac.warwick.tabula.data.model.{Assignment, FeedbackForSits, FeedbackForSitsStatus}
import uk.ac.warwick.tabula.helpers.RequestLevelCaching
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.cm2.WorkflowItems
import uk.ac.warwick.tabula.services.IncludeType

import scala.jdk.CollectionConverters._

/**
 * This isn't code for marking workflows. It drives the progress bar and next action on various coursework pages.
 */
@Service
class CM2WorkflowProgressService extends RequestLevelCaching[Assignment, Seq[CM2WorkflowStage]] {

  import CM2WorkflowStages._

  final val MaxPower = 100
  var features: Features = Wire[Features]

  def getStagesFor(assignment: Assignment): Seq[CM2WorkflowStage] = cachedBy(assignment) {
    val stages = Seq.newBuilder[CM2WorkflowStage]

    val hasMarkingWorkflow = features.markingWorkflows && assignment.cm2MarkingWorkflow != null

    if (assignment.collectSubmissions) {
      stages += Submission

      if (features.turnitin && assignment.module.adminDepartment.plagiarismDetectionEnabled) {
        stages += CheckForPlagiarism
      }

      if (!hasMarkingWorkflow) {
        stages += DownloadSubmission
      }
    }

    if (hasMarkingWorkflow) {
      stages += CM2ReleaseForMarking
      stages ++= assignment.cm2MarkingWorkflow.allStages.map(CM2MarkingWorkflowStage.apply)
    } else {
      if (assignment.collectMarks) {
        stages += AddMarks
      }

      stages += AddFeedback
    }

    if (assignment.publishFeedback) {
      stages ++= Seq(ReleaseFeedback, ViewOnlineFeedback, DownloadFeedback)
    }

    if (features.queueFeedbackForSits && (hasMarkingWorkflow || assignment.collectMarks) && assignment.module.adminDepartment.uploadCourseworkMarksToSits && !assignment.assessmentGroups.isEmpty) {
      stages += UploadMarksToSits
    }

    stages.result()
  }

  def progress(assignment: Assignment)(coursework: WorkflowItems): WorkflowProgress = {
    val allStages = getStagesFor(assignment)
    val progresses = allStages.map(_.progress(assignment)(coursework))

    val workflowMap = WorkflowStages.toMap(progresses)

    // Quick exit for if we're at the end
    if (progresses.last.completed) {
      WorkflowProgress(MaxPower, progresses.last.messageCode, progresses.last.health.cssClass, None, workflowMap)
    } else {
      val stagesWithPreconditionsMet = progresses.filter(progress => workflowMap(progress.stage.toString).preconditionsMet)

      progresses.filter(_.started).lastOption match {
        case Some(lastProgress) =>
          val index = progresses.indexOf(lastProgress)

          // If the current stage is complete, the next stage requires action
          val nextProgress = if (lastProgress.completed) {
            val nextProgressCandidate = progresses(index + 1)

            if (stagesWithPreconditionsMet.contains(nextProgressCandidate)) {
              nextProgressCandidate
            } else {
              // The next stage can't start yet because its preconditions are not met.
              // Find the latest incomplete stage from earlier in the workflow whose preconditions are met.
              val earlierReadyStages = progresses.reverse
                .dropWhile(_ != nextProgressCandidate)
                .filterNot(_.completed)
                .filter(stagesWithPreconditionsMet.contains)

              earlierReadyStages.headOption.getOrElse(lastProgress)
            }
          } else {
            lastProgress
          }

          val percentage = ((index + 1) * MaxPower) / allStages.size
          WorkflowProgress(percentage, lastProgress.messageCode, lastProgress.health.cssClass, Some(nextProgress.stage), workflowMap)
        case None =>
          WorkflowProgress(0, progresses.head.messageCode, progresses.head.health.cssClass, None, workflowMap)
      }
    }
  }
}

sealed abstract class CM2WorkflowCategory(val code: String)

object CM2WorkflowCategory {

  case object Submissions extends CM2WorkflowCategory("workflow.categories.Submissions")

  case object Plagiarism extends CM2WorkflowCategory("workflow.categories.Plagiarism")

  case object Marking extends CM2WorkflowCategory("workflow.categories.Marking")

  case object Feedback extends CM2WorkflowCategory("workflow.categories.Feedback")

  // lame manual collection. Keep in sync with the case objects above
  // Don't change this to a val https://warwick.slack.com/archives/C029QTGBN/p1493995125972397
  def members = Seq(Submissions, Plagiarism, Marking, Feedback)
}

sealed abstract class CM2WorkflowStage(val category: CM2WorkflowCategory) extends WorkflowStage {
  def progress(assignment: Assignment)(coursework: WorkflowItems): WorkflowStages.StageProgress

  case class Route(url: String)

  def route(assignment: Assignment): Option[Route]

  def markingRelated: Boolean // Is this a stage that a Marker cares about?
}

object CM2WorkflowStages {

  import CM2WorkflowCategory._
  import WorkflowStages._

  private val ObjectClassPrefix = CM2WorkflowStages.getClass.getName

  /**
   * Create an Permission from an action name (e.g. "Module.Create").
   * Most likely useful in view templates, for permissions checking.
   *
   * Note that, like the templates they're used in, the correctness isn't
   * checked at runtime.
   */
  def of(name: String, markingWorkflowType: MarkingWorkflowType = null): CM2WorkflowStage =
    name match {
      case r"""CM2MarkingWorkflowStage\((.+)${markingWorkflowStageCode}\)""" =>
        CM2MarkingWorkflowStage(MarkingWorkflowStage.fromCode(markingWorkflowStageCode, markingWorkflowType))

      case _ => try {
        // Go through the magical hierarchy
        val clz = Class.forName(ObjectClassPrefix + name.replace('.', '$') + "$")
        clz.getDeclaredField("MODULE$").get(null).asInstanceOf[CM2WorkflowStage]
      } catch {
        case _: ClassNotFoundException => throw new IllegalArgumentException(s"CM2WorkflowStage $name not recognised")
        case _: ClassCastException => throw new IllegalArgumentException(s"CM2WorkflowStage $name is not an endpoint of the hierarchy")
      }
    }

  case object Submission extends CM2WorkflowStage(Submissions) {
    def actionCode = "workflow.Submission.action"

    def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress = coursework.enhancedSubmission match {
      // If the student hasn't submitted, but we have uploaded feedback for them, don't record their submission status
      case None if coursework.enhancedFeedback.exists(!_.feedback.isPlaceholder) =>
        StageProgress(Submission, started = false, messageCode = "workflow.Submission.unsubmitted.withFeedback", completed = true)
      case Some(submission) if submission.submission.isLate =>
        StageProgress(Submission, started = true, messageCode = "workflow.Submission.late", health = Warning, completed = true)
      case Some(submission) if submission.submission.isAuthorisedLate =>
        StageProgress(Submission, started = true, messageCode = "workflow.Submission.authorisedLate", health = Good, completed = true)
      case Some(_) =>
        StageProgress(Submission, started = true, messageCode = "workflow.Submission.onTime", health = Good, completed = true)
      case None if !assignment.isClosed =>
        StageProgress(Submission, started = false, messageCode = "workflow.Submission.unsubmitted.withinDeadline")
      // Not submitted, check extension
      case _ => unsubmittedProgress(assignment)(coursework)
    }

    private def unsubmittedProgress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedExtension match {
      case Some(extension) if extension.within =>
        StageProgress(Submission, started = false, messageCode = "workflow.Submission.unsubmitted.withinExtension")
      case _ if assignment.isClosed && !assignment.allowLateSubmissions =>
        StageProgress(Submission, started = true, messageCode = "workflow.Submission.unsubmitted.failedToSubmit", health = Danger, completed = true)

      case _ => StageProgress(Submission, started = true, messageCode = "workflow.Submission.unsubmitted.late", health = Danger)
    }

    def route(assignment: Assignment): Option[Route] = None

    val markingRelated = true
  }

  case object DownloadSubmission extends CM2WorkflowStage(Marking) {
    def actionCode = "workflow.DownloadSubmission.action"

    def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress = coursework.enhancedSubmission match {
      case Some(submission) if submission.downloaded =>
        StageProgress(DownloadSubmission, started = true, messageCode = "workflow.DownloadSubmission.downloaded", health = Good, completed = true)
      case Some(_) =>
        StageProgress(DownloadSubmission, started = false, messageCode = "workflow.DownloadSubmission.notDownloaded")
      case _ =>
        StageProgress(DownloadSubmission, started = false, messageCode = "workflow.DownloadSubmission.notDownloaded")
    }

    override def preconditions = Seq(Seq(Submission))

    def route(assignment: Assignment): Option[Route] = Some(Route(Routes.admin.assignment.submissionsandfeedback(assignment)))

    val markingRelated = false
  }

  case object CheckForPlagiarism extends CM2WorkflowStage(Plagiarism) {
    def actionCode = "workflow.CheckForPlagiarism.action"

    def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress = coursework.enhancedSubmission match {
      case Some(item) if item.submission.suspectPlagiarised =>
        StageProgress(CheckForPlagiarism, started = true, messageCode = "workflow.CheckForPlagiarism.suspectPlagiarised", health = Danger, completed = true)
      case Some(item) if item.submission.allAttachments.exists(_.turnitinResultReceived) =>
        StageProgress(CheckForPlagiarism, started = true, messageCode = "workflow.CheckForPlagiarism.checked", health = Good, completed = true)
      case Some(item) if (item.submission.allAttachments.nonEmpty && assignment.submitToTurnitin) || item.submission.allAttachments.exists(_.turnitinCheckInProgress) =>
        StageProgress(CheckForPlagiarism, started = true, messageCode = "workflow.CheckForPlagiarism.started", health = Good)
      case None if (assignment.isClosed && !coursework.enhancedExtension.exists(_.within)) && !assignment.allowLateSubmissions =>
        StageProgress(CheckForPlagiarism, started = true, messageCode = "workflow.CheckForPlagiarism.failedToSubmit", health = Good, completed = true)
      case _ => StageProgress(CheckForPlagiarism, started = false, messageCode = "workflow.CheckForPlagiarism.notChecked")
    }

    override def preconditions = Seq(Seq(Submission))

    def route(assignment: Assignment): Option[Route] = Some(Route(Routes.admin.assignment.submitToTurnitin(assignment)))

    val markingRelated = false
  }

  case object CM2ReleaseForMarking extends CM2WorkflowStage(Marking) {
    def actionCode = "workflow.cm2.ReleaseForMarking.action"

    def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress = {
      if (coursework.enhancedFeedback.exists(_.feedback.outstandingStages.asScala.nonEmpty)) {
        StageProgress(CM2ReleaseForMarking, started = true, messageCode = "workflow.cm2.ReleaseForMarking.released", health = Good, completed = true)
      } else {
        StageProgress(CM2ReleaseForMarking, started = false, messageCode = "workflow.cm2.ReleaseForMarking.notReleased")
      }
    }

    override def preconditions = Seq()

    def route(assignment: Assignment): Option[Route] = Some(Route(Routes.admin.assignment.submissionsandfeedback(assignment)))

    val markingRelated = true
  }

  case class CM2MarkingWorkflowStage(markingStage: MarkingWorkflowStage) extends CM2WorkflowStage(Marking) {
    override def actionCode: String = s"workflow.cm2.${markingStage.name}.action"

    override def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress = {
      val currentStages = coursework.enhancedFeedback.toSeq.flatMap(_.feedback.outstandingStages.asScala)
      val workflowStage = CM2MarkingWorkflowStage(markingStage)

      if (currentStages.isEmpty || currentStages.head.order < markingStage.order) {
        // Not released for marking yet or this is a future stage
        StageProgress(workflowStage, started = false, messageCode = s"workflow.cm2.${markingStage.name}.notReady")
      } else if (currentStages.contains(markingStage)) {
        // This is the current stage
        val markerFeedback = coursework.enhancedFeedback.flatMap(_.feedback.markerFeedback.asScala.find(_.stage == markingStage))

        if (markerFeedback.exists(_.hasBeenModified)) {
          StageProgress(
            workflowStage,
            started = true,
            messageCode = s"workflow.cm2.${markingStage.name}.inProgress",
            health = Warning
          )
        } else {
          StageProgress(
            workflowStage,
            started = false,
            messageCode = s"workflow.cm2.${markingStage.name}.incomplete",
            health = Warning
          )
        }
      } else {
        val feedback = coursework.enhancedFeedback.map(_.feedback)
        val completedKey = markingStage.actionCompletedKey(feedback)

        // This is a past stage - if the moderation stage is in the past wasModerated tells us if it was skipped
        val skipped = markingStage.isInstanceOf[ModerationStage] && !feedback.exists(_.wasModerated)
        StageProgress(
          workflowStage,
          started = true,
          messageCode = s"workflow.cm2.${markingStage.name}.$completedKey",
          health = Good,
          completed = !skipped,
          skipped = skipped
        )
      }
    }

    // previousStages isn't recursive, but we expect it to be here
    override def preconditions: Seq[Seq[WorkflowStage]] = Seq(CM2ReleaseForMarking +: markingStage.previousStages.flatMap { s =>
      val previousStage = CM2MarkingWorkflowStage(s)
      previousStage.preconditions.flatten :+ previousStage
    })

    override def route(assignment: Assignment): Option[Route] = markingStage.url(assignment).map(Route.apply)

    val markingRelated = true
  }

  case object AddMarks extends CM2WorkflowStage(Marking) {
    def actionCode = "workflow.AddMarks.action"

    def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress =
      coursework.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
        case Some(item) if item.feedback.hasMarkOrGrade =>
          StageProgress(AddMarks, started = true, messageCode = "workflow.AddMarks.marked", health = Good, completed = true)
        case Some(_) => StageProgress(AddMarks, started = true, messageCode = "workflow.AddMarks.notMarked", health = Warning)
        case _ => StageProgress(AddMarks, started = false, messageCode = "workflow.AddMarks.notMarked")
      }

    def route(assignment: Assignment): Option[Route] = Some(Route(Routes.admin.assignment.marks(assignment)))

    val markingRelated = false
  }

  case object AddFeedback extends CM2WorkflowStage(Marking) {
    def actionCode = "workflow.AddFeedback.action"

    def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress = coursework.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
      case Some(item) if item.feedback.hasAttachments || item.feedback.hasOnlineFeedback =>
        StageProgress(AddFeedback, started = true, messageCode = "workflow.AddFeedback.uploaded", health = Good, completed = true)
      case Some(_) =>
        StageProgress(AddFeedback, started = true, messageCode = "workflow.AddFeedback.notUploaded", health = Warning)
      case _ =>
        StageProgress(AddFeedback, started = false, messageCode = "workflow.AddFeedback.notUploaded")
    }

    def route(assignment: Assignment): Option[Route] = Some(Route(Routes.admin.assignment.feedback.online(assignment)))

    val markingRelated = false
  }

  case object ReleaseFeedback extends CM2WorkflowStage(Feedback) {
    def actionCode = "workflow.ReleaseFeedback.action"

    def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress =
      coursework.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
        case Some(item) if item.feedback.released =>
          StageProgress(ReleaseFeedback, started = true, messageCode = "workflow.ReleaseFeedback.released", health = Good, completed = true)
        case Some(item) if item.feedback.hasContent =>
          StageProgress(ReleaseFeedback, started = true, messageCode = "workflow.ReleaseFeedback.notReleased", health = Warning)
        case _ => StageProgress(ReleaseFeedback, started = false, messageCode = "workflow.ReleaseFeedback.notReleased")
      }

    override def preconditions: Seq[Seq[WorkflowStage]] = Seq(
      Seq(AddMarks), // Assignments with no marking workflow
      Seq(AddFeedback), // Assignments with no marking workflow
    ) ++ MarkingWorkflowStage.values.collect { case f: FinalStage =>
      f.previousStages.map(CM2MarkingWorkflowStage.apply)
    }

    def route(assignment: Assignment): Option[Route] = Some(Route(Routes.admin.assignment.publishFeedback(assignment)))

    val markingRelated = false
  }

  case object ViewOnlineFeedback extends CM2WorkflowStage(Feedback) {
    def actionCode = "workflow.ViewOnlineFeedback.action"

    def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress =
      coursework.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
        case Some(item) if item.feedback.released && item.onlineViewed =>
          StageProgress(ViewOnlineFeedback, started = true, messageCode = "workflow.ViewOnlineFeedback.viewed", health = Good, completed = true)
        case Some(item) if item.feedback.released =>
          StageProgress(ViewOnlineFeedback, started = true, messageCode = "workflow.ViewOnlineFeedback.notViewed", health = Warning)
        case _ => StageProgress(ViewOnlineFeedback, started = false, messageCode = "workflow.ViewOnlineFeedback.notViewed")
      }

    override def preconditions = Seq(Seq(ReleaseFeedback))

    def route(assignment: Assignment): Option[Route] = None

    val markingRelated = false
  }

  case object DownloadFeedback extends CM2WorkflowStage(Feedback) {
    def actionCode = "workflow.DownloadFeedback.action"

    def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress =
      coursework.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
        case Some(item) if !(item.onlineViewed && (item.feedback.hasGenericFeedback || item.feedback.hasOnlineFeedback)) && !item.downloaded =>
          StageProgress(DownloadFeedback, started = false, messageCode = "workflow.DownloadFeedback.notDownloaded")
        case Some(item) if item.downloaded || !item.feedback.hasAttachments =>
          StageProgress(DownloadFeedback, started = true, messageCode = "workflow.DownloadFeedback.downloaded", health = Good, completed = true)
        case Some(item) if item.feedback.released =>
          StageProgress(DownloadFeedback, started = true, messageCode = "workflow.DownloadFeedback.notDownloaded", health = Warning)
        case _ => StageProgress(DownloadFeedback, started = false, messageCode = "workflow.DownloadFeedback.notDownloaded")
      }

    override def preconditions = Seq(Seq(ReleaseFeedback, ViewOnlineFeedback), Seq(ReleaseFeedback))

    def route(assignment: Assignment): Option[Route] = None

    val markingRelated = false
  }

  case object UploadMarksToSits extends CM2WorkflowStage(Feedback) {
    def actionCode = "workflow.UploadMarksToSits.action"

    def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress = {
      def isMarkOrGradeChanged(feedbackForSits: FeedbackForSits): Boolean = {
        // This is safe as you can only have a FFS if you have a Feedback
        val actualFeedback = coursework.enhancedFeedback.map(_.feedback).get

        Option(feedbackForSits.dateOfUpload).nonEmpty &&
          feedbackForSits.status != FeedbackForSitsStatus.UploadNotAttempted &&
          (
            feedbackForSits.actualMarkLastUploaded != actualFeedback.latestMark ||
              feedbackForSits.actualGradeLastUploaded != actualFeedback.latestGrade
            )
      }

      lazy val isManuallyAdded: Boolean =
        assignment.membershipInfo.items
          .filterNot(_.extraneous)
          .find(_.user == coursework.student)
          .exists(_.itemType == IncludeType)

      coursework.enhancedFeedback.flatMap(_.feedbackForSits) match {
        case Some(feedbackForSits) if feedbackForSits.status == FeedbackForSitsStatus.Failed =>
          StageProgress(UploadMarksToSits, started = true, messageCode = "workflow.UploadMarksToSits.failed", health = Danger)

        case Some(feedbackForSits) if isMarkOrGradeChanged(feedbackForSits) =>
          StageProgress(UploadMarksToSits, started = true, messageCode = "workflow.UploadMarksToSits.outOfDate", health = Danger)

        case Some(feedbackForSits) if feedbackForSits.status == FeedbackForSitsStatus.Successful =>
          StageProgress(UploadMarksToSits, started = true, messageCode = "workflow.UploadMarksToSits.successful", health = Good, completed = true)

        case Some(_) =>
          StageProgress(UploadMarksToSits, started = true, messageCode = "workflow.UploadMarksToSits.queued", health = Warning)

        case _ if isManuallyAdded =>
          StageProgress(UploadMarksToSits, started = true, messageCode = "workflow.UploadMarksToSits.manuallyAdded", health = Danger, completed = true)

        case _ if coursework.agreedMarksExist => // marks were entered outside of Tabula
          StageProgress(UploadMarksToSits, started = true, messageCode = "workflow.UploadMarksToSits.uploadedExternally", completed = true, skipped = true)

        case _ => StageProgress(UploadMarksToSits, started = false, messageCode = "workflow.UploadMarksToSits.notQueued")
      }
    }

    override def preconditions: Seq[Seq[WorkflowStage]] = Seq(
      Seq(AddMarks), // Assignments with no marking workflow
      Seq(AddFeedback), // Assignments with no marking workflow
    ) ++ MarkingWorkflowStage.values.collect { case f: FinalStage =>
      f.previousStages.map(CM2MarkingWorkflowStage.apply)
    }

    def route(assignment: Assignment): Option[Route] = Some(Route(Routes.admin.assignment.uploadToSits(assignment)))

    val markingRelated = false
  }

}

trait CM2WorkflowProgressServiceComponent {
  def workflowProgressService: CM2WorkflowProgressService
}

trait AutowiringCM2WorkflowProgressServiceComponent extends CM2WorkflowProgressServiceComponent {
  var workflowProgressService: CM2WorkflowProgressService = Wire[CM2WorkflowProgressService]
}
