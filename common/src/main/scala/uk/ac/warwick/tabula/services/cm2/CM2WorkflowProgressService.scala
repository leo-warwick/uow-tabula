package uk.ac.warwick.tabula.services.cm2

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.WorkflowStageHealth._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.MarkingMethod.{ModeratedMarking, SeenSecondMarking}
import uk.ac.warwick.tabula.data.model.MarkingState.{MarkingCompleted, Rejected}
import uk.ac.warwick.tabula.data.model.markingworkflow.{FinalStage, MarkingWorkflowStage}
import uk.ac.warwick.tabula.data.model.{Assignment, FeedbackForSits, FeedbackForSitsStatus}
import uk.ac.warwick.tabula.helpers.RequestLevelCaching
import uk.ac.warwick.tabula.helpers.cm2.WorkflowItems
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.collection.JavaConverters._

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

		val hasCM1MarkingWorkflow = features.markingWorkflows && assignment.markingWorkflow != null
		val hasCM2MarkingWorkflow = features.markingWorkflows && assignment.cm2MarkingWorkflow != null
		val hasMarkingWorkflow = hasCM1MarkingWorkflow || hasCM2MarkingWorkflow

		if (assignment.collectSubmissions) {
			stages += Submission

			if (features.turnitin && assignment.module.adminDepartment.plagiarismDetectionEnabled) {
				stages += CheckForPlagiarism
			}

			if (!hasMarkingWorkflow) {
				stages += DownloadSubmission
			}
		}

		if (hasCM1MarkingWorkflow) {
			stages ++= Seq(CM1ReleaseForMarking, CM1FirstMarking)

			if (assignment.markingWorkflow.hasSecondMarker) {
				if (assignment.markingWorkflow.markingMethod == ModeratedMarking) {
					stages += CM1Moderation
				} else {
					stages += CM1SecondMarking
				}
			}

			if (assignment.markingWorkflow.markingMethod == SeenSecondMarking) {
				stages += CM1FinaliseSeenSecondMarking
			}
		} else if (hasCM2MarkingWorkflow) {
			stages += CM2ReleaseForMarking
			stages ++= assignment.cm2MarkingWorkflow.allStages.map(CM2MarkingWorkflowStage.apply)
		}

		if (!hasMarkingWorkflow) {
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
		val progresses = allStages.map { _.progress(assignment)(coursework) }

		val workflowMap = WorkflowStages.toMap(progresses)

		// Quick exit for if we're at the end
		if (progresses.last.completed) {
			WorkflowProgress(MaxPower, progresses.last.messageCode, progresses.last.health.cssClass, None, workflowMap)
		} else {
			// get the last started stage
			val stageIndex = progresses.lastIndexWhere(_.started)
			if (stageIndex == -1) WorkflowProgress(0, progresses.head.messageCode, progresses.head.health.cssClass, None, workflowMap)
			else {
				val lastProgress = progresses(stageIndex)
				val nextProgress = if (lastProgress.completed) progresses(stageIndex + 1) else lastProgress

				val percentage = ((stageIndex + 1) * MaxPower) / allStages.size
				WorkflowProgress(percentage, lastProgress.messageCode, lastProgress.health.cssClass, Some(nextProgress.stage), workflowMap)
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

	case class Route(title: String, url: String)
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
	def of(name: String): CM2WorkflowStage =
		name match {
			case r"""CM2MarkingWorkflowStage\((.+)${markingWorkflowStageCode}\)""" =>
				CM2MarkingWorkflowStage(MarkingWorkflowStage.fromCode(markingWorkflowStageCode))

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

			case _ => StageProgress(Submission, started = true, messageCode = "workflow.Submission.unsubmitted.late", health = Danger, completed = false)
		}

		def route(assignment: Assignment): Option[Route] = None
		val markingRelated = false
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
		def route(assignment: Assignment): Option[Route] = Some(Route("Download submissions", Routes.admin.assignment.submissionsandfeedback(assignment)))
		val markingRelated = false
	}

	case object CheckForPlagiarism extends CM2WorkflowStage(Plagiarism) {
		def actionCode = "workflow.CheckForPlagiarism.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress = coursework.enhancedSubmission match {
			case Some(item) if item.submission.suspectPlagiarised =>
				StageProgress(CheckForPlagiarism, started = true, messageCode = "workflow.CheckForPlagiarism.suspectPlagiarised", health = Danger, completed = true)
			case Some(item) if item.submission.allAttachments.exists(_.originalityReportReceived) =>
				StageProgress(CheckForPlagiarism, started = true, messageCode = "workflow.CheckForPlagiarism.checked", health = Good, completed = true)
			case Some(item) if item.submission.allAttachments.nonEmpty && assignment.submitToTurnitin =>
				StageProgress(CheckForPlagiarism, started = true, messageCode = "workflow.CheckForPlagiarism.started", health = Good)
			case Some(_) => StageProgress(CheckForPlagiarism, started = false, messageCode = "workflow.CheckForPlagiarism.notChecked")
			case _ => StageProgress(CheckForPlagiarism, started = false, messageCode = "workflow.CheckForPlagiarism.notChecked")
		}
		override def preconditions = Seq(Seq(Submission))
		def route(assignment: Assignment): Option[Route] = Some(Route("Check for plagiarism", Routes.admin.assignment.submitToTurnitin(assignment)))
		val markingRelated = false
	}

	case object CM1ReleaseForMarking extends CM2WorkflowStage(Marking) {
		def actionCode = "workflow.cm1.ReleaseForMarking.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress = {
			if (assignment.isReleasedForMarking(coursework.student.getUserId)) {
				StageProgress(CM1ReleaseForMarking, started = true, messageCode = "workflow.cm1.ReleaseForMarking.released", health = Good, completed = true)
			} else {
				StageProgress(CM1ReleaseForMarking, started = false, messageCode = "workflow.cm1.ReleaseForMarking.notReleased")
			}
		}
		override def preconditions = Seq()
		def route(assignment: Assignment): Option[Route] = Some(Route("Release for marking", Routes.admin.assignment.submissionsandfeedback(assignment)))
		val markingRelated = true
	}

	case object CM1FirstMarking extends CM2WorkflowStage(Marking) {
		def actionCode = "workflow.cm1.FirstMarking.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress = coursework.enhancedFeedback match {
			case Some(item) =>
				if (item.feedback.getFirstMarkerFeedback.exists(_.state == MarkingCompleted))
					StageProgress(CM1FirstMarking, started = true, messageCode = "workflow.cm1.FirstMarking.marked", health = Good, completed = true)
				else
					StageProgress(CM1FirstMarking, started = true, messageCode = "workflow.cm1.FirstMarking.notMarked", health = Warning, completed = false)
			case _ => StageProgress(CM1FirstMarking, started = false, messageCode = "workflow.cm1.FirstMarking.notMarked")
		}
		override def preconditions = Seq(Seq(CM1ReleaseForMarking))
		def route(assignment: Assignment): Option[Route] = None
		val markingRelated = true
	}

	case object CM1SecondMarking extends CM2WorkflowStage(Marking) {
		def actionCode = "workflow.cm1.SecondMarking.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress = {
			val released = assignment.isReleasedToSecondMarker(coursework.student.getUserId)
			coursework.enhancedFeedback match {
				case Some(item) if released && item.feedback.getSecondMarkerFeedback.exists(_.state != Rejected) =>
					if (item.feedback.getSecondMarkerFeedback.exists(_.state == MarkingCompleted))
						StageProgress(
							CM1SecondMarking,
							started = true,
							messageCode = "workflow.cm1.SecondMarking.marked",
							health = Good,
							completed = true
						)
					else
						StageProgress(
							CM1SecondMarking,
							started = item.feedback.getFirstMarkerFeedback.exists(_.state == MarkingCompleted),
							messageCode = "workflow.cm1.SecondMarking.notMarked",
							health = Warning,
							completed = false
						)
				case _ => StageProgress(CM1SecondMarking, started = false, messageCode = "workflow.cm1.SecondMarking.notMarked")
			}
		}
		override def preconditions = Seq(Seq(CM1ReleaseForMarking, CM1FirstMarking))
		def route(assignment: Assignment): Option[Route] = None
		val markingRelated = true
	}

	case object CM1Moderation extends CM2WorkflowStage(Marking) {
		def actionCode = "workflow.cm1.ModeratedMarking.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress = {
			val released = assignment.isReleasedToSecondMarker(coursework.student.getUserId)
			coursework.enhancedFeedback match {
				case Some(item) if released && item.feedback.getSecondMarkerFeedback.exists(_.state != Rejected) =>
					if (item.feedback.getSecondMarkerFeedback.exists(_.state == MarkingCompleted))
						StageProgress(
							CM1Moderation,
							started = true,
							messageCode = "workflow.cm1.ModeratedMarking.marked",
							health = Good,
							completed = true
						)
					else
						StageProgress(
							CM1Moderation,
							started = item.feedback.getFirstMarkerFeedback.exists(_.state == MarkingCompleted),
							messageCode = "workflow.cm1.ModeratedMarking.notMarked",
							health = Warning,
							completed = false
						)
				case _ => StageProgress(CM1Moderation, started = false, messageCode = "workflow.cm1.ModeratedMarking.notMarked")
			}
		}
		override def preconditions = Seq(Seq(CM1ReleaseForMarking, CM1FirstMarking))
		def route(assignment: Assignment): Option[Route] = None
		val markingRelated = true
	}

	case object CM1FinaliseSeenSecondMarking extends CM2WorkflowStage(Marking) {
		def actionCode = "workflow.cm1.FinaliseSeenSecondMarking.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress = {
			val released = assignment.isReleasedToThirdMarker(coursework.student.getUserId)
			coursework.enhancedFeedback match {
				case Some(item) if released && item.feedback.getThirdMarkerFeedback.exists(_.state != Rejected) =>
					if (item.feedback.getThirdMarkerFeedback.exists(_.state == MarkingCompleted))
						StageProgress(
							CM1FinaliseSeenSecondMarking,
							started = true,
							messageCode = "workflow.cm1.FinaliseSeenSecondMarking.finalised",
							health = Good,
							completed = true
						)
					else
						StageProgress(
							CM1FinaliseSeenSecondMarking,
							started = item.feedback.getSecondMarkerFeedback.exists(_.state == MarkingCompleted),
							messageCode = "workflow.cm1.FinaliseSeenSecondMarking.notFinalised",
							health = Warning,
							completed = false
						)
				case _ => StageProgress(CM1FinaliseSeenSecondMarking, started = false, messageCode = "workflow.cm1.FinaliseSeenSecondMarking.notFinalised")
			}
		}
		override def preconditions = Seq(Seq(CM1ReleaseForMarking, CM1FirstMarking, CM1SecondMarking))
		def route(assignment: Assignment): Option[Route] = None
		val markingRelated = true
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
		def route(assignment: Assignment): Option[Route] = Some(Route("Release for marking", Routes.admin.assignment.submissionsandfeedback(assignment)))
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
				val completedKey = markingStage.actionCompletedKey(coursework.enhancedFeedback.map(_.feedback))
				// This is a past stage
				StageProgress(
					workflowStage,
					started = true,
					messageCode = s"workflow.cm2.${markingStage.name}.$completedKey",
					health = Good,
					completed = true
				)
			}
		}

		// previousStages isn't recursive, but we expect it to be here
		override def preconditions: Seq[Seq[WorkflowStage]] = Seq(CM2ReleaseForMarking +: markingStage.previousStages.flatMap { s =>
			val previousStage = CM2MarkingWorkflowStage(s)
			previousStage.preconditions.flatten :+ previousStage
		})

		override def route(assignment: Assignment): Option[Route] = markingStage.url(assignment).map(Route("", _))
		val markingRelated = true
	}

	case object AddMarks extends CM2WorkflowStage(Marking) {
		def actionCode = "workflow.AddMarks.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress =
			coursework.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
				case Some(item) if item.feedback.hasMarkOrGrade =>
					StageProgress(AddMarks, started = true, messageCode = "workflow.AddMarks.marked", health = Good, completed = true)
				case Some(_) => StageProgress(AddMarks, started = true, messageCode = "workflow.AddMarks.notMarked", health = Warning, completed = false)
				case _ => StageProgress(AddMarks, started = false, messageCode = "workflow.AddMarks.notMarked")
			}

		def route(assignment: Assignment): Option[Route] = Some(Route("Add marks", Routes.admin.assignment.marks(assignment)))
		val markingRelated = false
	}

	case object AddFeedback extends CM2WorkflowStage(Marking) {
		def actionCode = "workflow.AddFeedback.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress = coursework.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
			case Some(item) if item.feedback.hasAttachments || item.feedback.hasOnlineFeedback =>
				StageProgress(AddFeedback, started = true, messageCode = "workflow.AddFeedback.uploaded", health = Good, completed = true)
			case Some(_) =>
				StageProgress(AddFeedback, started = true, messageCode = "workflow.AddFeedback.notUploaded", health = Warning, completed = false)
			case _ =>
				StageProgress(AddFeedback, started = false, messageCode = "workflow.AddFeedback.notUploaded")
		}

		def route(assignment: Assignment): Option[Route] = Some(Route("Add feedback", Routes.admin.assignment.feedback.online(assignment)))
		val markingRelated = false
	}

	case object ReleaseFeedback extends CM2WorkflowStage(Feedback) {
		def actionCode = "workflow.ReleaseFeedback.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress =
			coursework.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
				case Some(item) if item.feedback.released =>
					StageProgress(ReleaseFeedback, started = true, messageCode = "workflow.ReleaseFeedback.released", health = Good, completed = true)
				case Some(item) if item.feedback.hasContent =>
					StageProgress(ReleaseFeedback, started = true, messageCode = "workflow.ReleaseFeedback.notReleased", health = Warning, completed = false)
				case _ => StageProgress(ReleaseFeedback, started = false, messageCode = "workflow.ReleaseFeedback.notReleased")
			}
		override def preconditions: Seq[Seq[WorkflowStage]] = Seq(
			Seq(AddMarks), // Assignments with no marking workflow
			Seq(AddFeedback), // Assignments with no marking workflow
			Seq(CM1FirstMarking, CM1SecondMarking, CM1FinaliseSeenSecondMarking), // CM1 seen second marking
			Seq(CM1FirstMarking) // CM1 single marker
		) ++ MarkingWorkflowStage.values.collect { case f: FinalStage =>
			f.previousStages.map(CM2MarkingWorkflowStage.apply)
		}

		def route(assignment: Assignment): Option[Route] = Some(Route("Release feedback", Routes.admin.assignment.publishFeedback(assignment)))
		val markingRelated = false
	}

	case object ViewOnlineFeedback extends CM2WorkflowStage(Feedback) {
		def actionCode = "workflow.ViewOnlineFeedback.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems): StageProgress =
			coursework.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
				case Some(item) if item.feedback.released && item.onlineViewed =>
					StageProgress(ViewOnlineFeedback, started = true, messageCode = "workflow.ViewOnlineFeedback.viewed", health = Good, completed = true)
				case Some(item) if item.feedback.released =>
					StageProgress(ViewOnlineFeedback, started = true, messageCode = "workflow.ViewOnlineFeedback.notViewed", health = Warning, completed = false)
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
				case Some(item) if !(item.onlineViewed && (item.feedback.hasGenericFeedback || item.feedback.hasOnlineFeedback)) && !item.downloaded  =>
					StageProgress(DownloadFeedback, started = false, messageCode = "workflow.DownloadFeedback.notDownloaded")
				case Some(item) if item.downloaded || !item.feedback.hasAttachments =>
					StageProgress(DownloadFeedback, started = true, messageCode = "workflow.DownloadFeedback.downloaded", health = Good, completed = true)
				case Some(item) if item.feedback.released =>
					StageProgress(DownloadFeedback, started = true, messageCode = "workflow.DownloadFeedback.notDownloaded", health = Warning, completed = false)
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
						Option(feedbackForSits.actualMarkLastUploaded).getOrElse(0) != actualFeedback.latestMark.getOrElse(0) ||
						Option(feedbackForSits.actualGradeLastUploaded).getOrElse("") != actualFeedback.latestGrade.getOrElse("")
					)
			}

			coursework.enhancedFeedback.flatMap(_.feedbackForSits) match {
				case Some(feedbackForSits) if feedbackForSits.status == FeedbackForSitsStatus.Failed =>
					StageProgress(UploadMarksToSits, started = true, messageCode = "workflow.UploadMarksToSits.failed", health = Danger)

				case Some(feedbackForSits) if isMarkOrGradeChanged(feedbackForSits) =>
					StageProgress(UploadMarksToSits, started = true, messageCode = "workflow.UploadMarksToSits.outOfDate", health = Danger)

				case Some(feedbackForSits) if feedbackForSits.status == FeedbackForSitsStatus.Successful =>
					StageProgress(UploadMarksToSits, started = true, messageCode = "workflow.UploadMarksToSits.successful", health = Good, completed = true)

				case Some(_) =>
					StageProgress(UploadMarksToSits, started = true, messageCode = "workflow.UploadMarksToSits.queued", health = Warning)

				case _ => StageProgress(UploadMarksToSits, started = false, messageCode = "workflow.UploadMarksToSits.notQueued")
			}
		}

		override def preconditions: Seq[Seq[WorkflowStage]] = Seq(
			Seq(AddMarks), // Assignments with no marking workflow
			Seq(AddFeedback), // Assignments with no marking workflow
			Seq(CM1FirstMarking, CM1SecondMarking, CM1FinaliseSeenSecondMarking), // CM1 seen second marking
			Seq(CM1FirstMarking) // CM1 single marker
		) ++ MarkingWorkflowStage.values.collect { case f: FinalStage =>
			f.previousStages.map(CM2MarkingWorkflowStage.apply)
		}

		def route(assignment: Assignment): Option[Route] = Some(Route("Upload feedback to SITS", Routes.admin.assignment.uploadToSits(assignment)))
		val markingRelated = false
	}
}

trait CM2WorkflowProgressServiceComponent {
	def workflowProgressService: CM2WorkflowProgressService
}

trait AutowiringCM2WorkflowProgressServiceComponent extends CM2WorkflowProgressServiceComponent {
	var workflowProgressService: CM2WorkflowProgressService = Wire[CM2WorkflowProgressService]
}