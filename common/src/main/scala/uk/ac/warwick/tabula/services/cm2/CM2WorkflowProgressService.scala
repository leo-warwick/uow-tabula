package uk.ac.warwick.tabula.services.cm2

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.WorkflowStageHealth._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.MarkingMethod.{ModeratedMarking, SeenSecondMarking}
import uk.ac.warwick.tabula.data.model.MarkingState.{MarkingCompleted, Rejected}
import uk.ac.warwick.tabula.helpers.cm2.WorkflowItems


//TODO- This might need further refactoring. Currently just copied  from existing coursework
/**
	* This isn't code for marking workflows. It drives the progress bar and next action on various coursework pages.
	*/
@Service
class CM2WorkflowProgressService {
	import CM2WorkflowStages._

	final val MaxPower = 100
	var features: Features = Wire.auto[Features]

	def getStagesFor(assignment: Assignment): Seq[CM2WorkflowStage] = {
		var stages = Seq[CM2WorkflowStage]()
		if (assignment.collectSubmissions) {
			stages = stages ++ Seq(Submission)

			if (features.turnitin && assignment.module.adminDepartment.plagiarismDetectionEnabled) {
				stages = stages ++ Seq(CheckForPlagiarism)
			}

			stages = stages ++ Seq(DownloadSubmission)

			if (features.markingWorkflows && assignment.markingWorkflow != null) {
				stages = stages ++ Seq(ReleaseForMarking, FirstMarking)

				if (assignment.markingWorkflow.hasSecondMarker) {
					if (assignment.markingWorkflow.markingMethod == ModeratedMarking) {
						stages = stages ++ Seq(Moderation)
					} else {
						stages = stages ++ Seq(SecondMarking)
					}
				}

				if (assignment.markingWorkflow.markingMethod == SeenSecondMarking) {
					stages = stages ++ Seq(FinaliseSeenSecondMarking)
				}
			}
		}

		if (assignment.collectMarks) {
			stages = stages ++ Seq(AddMarks)
		}

		stages = stages ++ Seq(AddFeedback, ReleaseFeedback, ViewOnlineFeedback, DownloadFeedback)

		stages
	}

	def progress(assignment: Assignment)(cm2: WorkflowItems): WorkflowProgress = {
		val allStages = getStagesFor(assignment)
		val progresses = allStages map { _.progress(assignment)(cm2) }

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

sealed abstract class CM2WorkflowStage extends WorkflowStage {
	def progress(assignment: Assignment)(cm2: WorkflowItems): WorkflowStages.StageProgress
}

object CM2WorkflowStages {
	import WorkflowStages._

	case object Submission extends CM2WorkflowStage {
		def actionCode = "workflow.Submission.action"
		def progress(assignment: Assignment)(cm2: WorkflowItems): StageProgress = cm2.enhancedSubmission match {
			// If the student hasn't submitted, but we have uploaded feedback for them, don't record their submission status
			case None if cm2.enhancedFeedback.exists(!_.feedback.isPlaceholder) =>
				StageProgress(Submission, started = false, messageCode = "workflow.Submission.unsubmitted.withFeedback")
			case Some(submission) if submission.submission.isLate =>
				StageProgress(Submission, started = true, messageCode = "workflow.Submission.late", health = Warning, completed = true)
			case Some(submission) if submission.submission.isAuthorisedLate =>
				StageProgress(Submission, started = true, messageCode = "workflow.Submission.authorisedLate", health = Good, completed = true)
			case Some(_) =>
				StageProgress(Submission, started = true, messageCode = "workflow.Submission.onTime", health = Good, completed = true)
			case None if !assignment.isClosed =>
				StageProgress(Submission, started = false, messageCode = "workflow.Submission.unsubmitted.withinDeadline")
			// Not submitted, check extension
			case _ => unsubmittedProgress(assignment)(cm2)
		}

		private def unsubmittedProgress(assignment: Assignment)(cm2: WorkflowItems) = cm2.enhancedExtension match {
			case Some(extension) if extension.within =>
				StageProgress(Submission, started = false, messageCode = "workflow.Submission.unsubmitted.withinExtension")
			case _ if assignment.isClosed && !assignment.allowLateSubmissions =>
				StageProgress(Submission, started = true, messageCode = "workflow.Submission.unsubmitted.failedToSubmit", health = Danger, completed = false)

			case _ => StageProgress(Submission, started = true, messageCode = "workflow.Submission.unsubmitted.late", health = Danger, completed = false)
		}
	}

	case object DownloadSubmission extends CM2WorkflowStage {
		def actionCode = "workflow.DownloadSubmission.action"
		def progress(assignment: Assignment)(cm2: WorkflowItems): StageProgress = cm2.enhancedSubmission match {
			case Some(submission) if submission.downloaded =>
				StageProgress(DownloadSubmission, started = true, messageCode = "workflow.DownloadSubmission.downloaded", health = Good, completed = true)
			case Some(_) =>
				StageProgress(DownloadSubmission, started = false, messageCode = "workflow.DownloadSubmission.notDownloaded")
			case _ =>
				StageProgress(DownloadSubmission, started = false, messageCode = "workflow.DownloadSubmission.notDownloaded")
		}
		override def preconditions = Seq(Seq(Submission))
	}

	case object CheckForPlagiarism extends CM2WorkflowStage {
		def actionCode = "workflow.CheckForPlagiarism.action"
		def progress(assignment: Assignment)(cm2: WorkflowItems): StageProgress = cm2.enhancedSubmission match {
			case Some(item) if item.submission.suspectPlagiarised =>
				StageProgress(CheckForPlagiarism, started = true, messageCode = "workflow.CheckForPlagiarism.suspectPlagiarised", health = Danger, completed = true)
			case Some(item) if item.submission.allAttachments.exists(_.originalityReportReceived) =>
				StageProgress(CheckForPlagiarism, started = true, messageCode = "workflow.CheckForPlagiarism.checked", health = Good, completed = true)
			case Some(_) => StageProgress(CheckForPlagiarism, started = false, messageCode = "workflow.CheckForPlagiarism.notChecked")
			case _ => StageProgress(CheckForPlagiarism, started = false, messageCode = "workflow.CheckForPlagiarism.notChecked")
		}
		override def preconditions = Seq(Seq(Submission))
	}

	case object ReleaseForMarking extends CM2WorkflowStage {
		def actionCode = "workflow.ReleaseForMarking.action"
		def progress(assignment: Assignment)(cm2: WorkflowItems): StageProgress = {
			if (assignment.isReleasedForMarking(cm2.student.getUserId)) {
				StageProgress(ReleaseForMarking, started = true, messageCode = "workflow.ReleaseForMarking.released", health = Good, completed = true)
			} else {
				StageProgress(ReleaseForMarking, started = false, messageCode = "workflow.ReleaseForMarking.notReleased")
			}
		}
		override def preconditions = Seq()
	}

	case object FirstMarking extends CM2WorkflowStage {
		def actionCode = "workflow.FirstMarking.action"
		def progress(assignment: Assignment)(cm2: WorkflowItems): StageProgress = cm2.enhancedFeedback match {
			case Some(item) =>
				if (item.feedback.getFirstMarkerFeedback.exists(_.state == MarkingCompleted))
					StageProgress(FirstMarking, started = true, messageCode = "workflow.FirstMarking.marked", health = Good, completed = true)
				else
					StageProgress(FirstMarking, started = true, messageCode = "workflow.FirstMarking.notMarked", health = Warning, completed = false)
			case _ => StageProgress(FirstMarking, started = false, messageCode = "workflow.FirstMarking.notMarked")
		}
		override def preconditions = Seq(Seq(ReleaseForMarking))
	}

	case object SecondMarking extends CM2WorkflowStage {
		def actionCode = "workflow.SecondMarking.action"
		def progress(assignment: Assignment)(cm2: WorkflowItems): StageProgress = {
			val released = assignment.isReleasedToSecondMarker(cm2.student.getUserId)
			cm2.enhancedFeedback match {
				case Some(item) if released && item.feedback.getSecondMarkerFeedback.exists(_.state != Rejected) =>
					if (item.feedback.getSecondMarkerFeedback.exists(_.state == MarkingCompleted))
						StageProgress(
							SecondMarking,
							started = true,
							messageCode = "workflow.SecondMarking.marked",
							health = Good,
							completed = true
						)
					else
						StageProgress(
							SecondMarking,
							started = item.feedback.getFirstMarkerFeedback.exists(_.state == MarkingCompleted),
							messageCode = "workflow.SecondMarking.notMarked",
							health = Warning,
							completed = false
						)
				case _ => StageProgress(SecondMarking, started = false, messageCode = "workflow.SecondMarking.notMarked")
			}
		}
		override def preconditions = Seq(Seq(ReleaseForMarking, FirstMarking))
	}

	case object Moderation extends CM2WorkflowStage {
		def actionCode = "workflow.ModeratedMarking.action"
		def progress(assignment: Assignment)(cm2: WorkflowItems): StageProgress = {
			val released = assignment.isReleasedToSecondMarker(cm2.student.getWarwickId)
			cm2.enhancedFeedback match {
				case Some(item) if released && item.feedback.getSecondMarkerFeedback.exists(_.state != Rejected) =>
					if (item.feedback.getSecondMarkerFeedback.exists(_.state == MarkingCompleted))
						StageProgress(
							Moderation,
							started = true,
							messageCode = "workflow.ModeratedMarking.marked",
							health = Good,
							completed = true
						)
					else
						StageProgress(
							Moderation,
							started = item.feedback.getFirstMarkerFeedback.exists(_.state == MarkingCompleted),
							messageCode = "workflow.ModeratedMarking.notMarked",
							health = Warning,
							completed = false
						)
				case _ => StageProgress(Moderation, started = false, messageCode = "workflow.ModeratedMarking.notMarked")
			}
		}
		override def preconditions = Seq(Seq(ReleaseForMarking, FirstMarking))
	}

	case object FinaliseSeenSecondMarking extends CM2WorkflowStage {
		def actionCode = "workflow.FinaliseSeenSecondMarking.action"
		def progress(assignment: Assignment)(cm2: WorkflowItems): StageProgress = {
			val released = assignment.isReleasedToThirdMarker(cm2.student.getUserId)
			cm2.enhancedFeedback match {
				case Some(item) if released && item.feedback.getThirdMarkerFeedback.exists(_.state != Rejected) =>
					if (item.feedback.getThirdMarkerFeedback.exists(_.state == MarkingCompleted))
						StageProgress(
							FinaliseSeenSecondMarking,
							started = true,
							messageCode = "workflow.FinaliseSeenSecondMarking.finalised",
							health = Good,
							completed = true
						)
					else
						StageProgress(
							FinaliseSeenSecondMarking,
							started = item.feedback.getSecondMarkerFeedback.exists(_.state == MarkingCompleted),
							messageCode = "workflow.FinaliseSeenSecondMarking.notFinalised",
							health = Warning,
							completed = false
						)
				case _ => StageProgress(FinaliseSeenSecondMarking, started = false, messageCode = "workflow.FinaliseSeenSecondMarking.notFinalised")
			}
		}
		override def preconditions = Seq(Seq(ReleaseForMarking, FirstMarking, SecondMarking))
	}




	case object AddMarks extends CM2WorkflowStage {
		def actionCode = "workflow.AddMarks.action"
		def progress(assignment: Assignment)(cm2: WorkflowItems): StageProgress =
			cm2.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
				case Some(item) if item.feedback.hasMarkOrGrade =>
					StageProgress(AddMarks, started = true, messageCode = "workflow.AddMarks.marked", health = Good, completed = true)
				case Some(_) => StageProgress(AddMarks, started = true, messageCode = "workflow.AddMarks.notMarked", health = Warning, completed = false)
				case _ => StageProgress(AddMarks, started = false, messageCode = "workflow.AddMarks.notMarked")
			}
	}

	case object AddFeedback extends CM2WorkflowStage {
		def actionCode = "workflow.AddFeedback.action"
		def progress(assignment: Assignment)(cm2: WorkflowItems): StageProgress = cm2.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
			case Some(item) if item.feedback.hasAttachments || item.feedback.hasOnlineFeedback =>
				StageProgress(AddFeedback, started = true, messageCode = "workflow.AddFeedback.uploaded", health = Good, completed = true)
			case Some(_) =>
				StageProgress(AddFeedback, started = true, messageCode = "workflow.AddFeedback.notUploaded", health = Warning, completed = false)
			case _ =>
				StageProgress(AddFeedback, started = false, messageCode = "workflow.AddFeedback.notUploaded")
		}
	}

	case object ReleaseFeedback extends CM2WorkflowStage {
		def actionCode = "workflow.ReleaseFeedback.action"
		def progress(assignment: Assignment)(cm2: WorkflowItems): StageProgress =
			cm2.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
				case Some(item) if item.feedback.released =>
					StageProgress(ReleaseFeedback, started = true, messageCode = "workflow.ReleaseFeedback.released", health = Good, completed = true)
				case Some(item) if item.feedback.hasAttachments || item.feedback.hasOnlineFeedback || item.feedback.hasMarkOrGrade =>
					StageProgress(ReleaseFeedback, started = true, messageCode = "workflow.ReleaseFeedback.notReleased", health = Warning, completed = false)
				case _ => StageProgress(ReleaseFeedback, started = false, messageCode = "workflow.ReleaseFeedback.notReleased")
			}
		override def preconditions = Seq(Seq(AddMarks), Seq(AddFeedback))
	}

	case object ViewOnlineFeedback extends CM2WorkflowStage {
		def actionCode = "workflow.ViewOnlineFeedback.action"
		def progress(assignment: Assignment)(cm2: WorkflowItems): StageProgress =
			cm2.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
				case Some(item) if item.feedback.released && item.onlineViewed =>
					StageProgress(ViewOnlineFeedback, started = true, messageCode = "workflow.ViewOnlineFeedback.viewed", health = Good, completed = true)
				case Some(item) if item.feedback.released =>
					StageProgress(ViewOnlineFeedback, started = true, messageCode = "workflow.ViewOnlineFeedback.notViewed", health = Warning, completed = false)
				case _ => StageProgress(ViewOnlineFeedback, started = false, messageCode = "workflow.ViewOnlineFeedback.notViewed")
		}
		override def preconditions = Seq(Seq(ReleaseFeedback))
	}

	case object DownloadFeedback extends CM2WorkflowStage {
		def actionCode = "workflow.DownloadFeedback.action"
		def progress(assignment: Assignment)(cm2: WorkflowItems): StageProgress =
			cm2.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
				case Some(item) if !(item.onlineViewed && (item.feedback.hasGenericFeedback || item.feedback.hasOnlineFeedback)) && !item.downloaded  =>
					StageProgress(DownloadFeedback, started = false, messageCode = "workflow.DownloadFeedback.notDownloaded")
				case Some(item) if item.downloaded || !item.feedback.hasAttachments =>
					StageProgress(DownloadFeedback, started = true, messageCode = "workflow.DownloadFeedback.downloaded", health = Good, completed = true)
				case Some(item) if item.feedback.released =>
					StageProgress(DownloadFeedback, started = true, messageCode = "workflow.DownloadFeedback.notDownloaded", health = Warning, completed = false)
				case _ => StageProgress(DownloadFeedback, started = false, messageCode = "workflow.DownloadFeedback.notDownloaded")
			}
		override def preconditions = Seq(Seq(ReleaseFeedback, ViewOnlineFeedback), Seq(ReleaseFeedback))
	}
}

trait CM2WorkflowServiceProgressComponent {
	def cm2WorkflowProgressService: CM2WorkflowProgressService
}

trait AutowiringCM2WorkflowServiceProgressComponent extends CM2WorkflowServiceProgressComponent {
	var cm2WorkflowProgressService: CM2WorkflowProgressService = Wire[CM2WorkflowProgressService]
}