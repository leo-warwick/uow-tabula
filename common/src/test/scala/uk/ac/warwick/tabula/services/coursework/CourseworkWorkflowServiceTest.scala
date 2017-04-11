package uk.ac.warwick.tabula.services.coursework

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.coursework.assignments.ListSubmissionsCommand._
import uk.ac.warwick.tabula.commands.coursework.assignments.SubmissionAndFeedbackCommand._
import uk.ac.warwick.tabula.commands.coursework.feedback.ListFeedbackCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.userlookup.User

import scala.collection.immutable.ListMap

// scalastyle:off magic.number
class CourseworkWorkflowServiceTest extends TestBase {

	val department: Department = Fixtures.department("in", "IT Services")
	val module: Module = Fixtures.module("in101", "Introduction to Web Development")
	val assignment: Assignment = Fixtures.assignment("Programming Test")
	assignment.module = module
	module.adminDepartment = department

	val service = new CourseworkWorkflowService
	service.features = Features.empty

	private def workflowItems(
			student:User = null,
			submission: Option[Submission]=None,
			submissionDownloaded: Boolean=false,
			feedback: Option[Feedback]=None,
			feedbackDownloaded: Boolean=false,
			onlineFeedbackViewed: Boolean=false,
			extension: Option[Extension]=None,
			withinExtension: Boolean=false) =
		WorkflowItems(
			student,
			enhancedSubmission=submission map { s => SubmissionListItem(s, submissionDownloaded) },
			enhancedFeedback=feedback map { f => FeedbackListItem(f, feedbackDownloaded, onlineFeedbackViewed, null) },
			enhancedExtension=extension map { e => ExtensionListItem(e, withinExtension) }
		)

	@Test def stagesForAssignment() {
		import CourseworkWorkflowStages._

		assignment.collectMarks = false
		assignment.collectSubmissions = false
		assignment.markingWorkflow = null
		assignment.genericFeedback = "Some feedback no doubt"

		service.features.turnitin = false
		service.features.markingWorkflows = false
		department.plagiarismDetectionEnabled = false

		// Default stages
		service.getStagesFor(assignment) should be (Seq(
			AddFeedback, ReleaseFeedback, ViewOnlineFeedback, DownloadFeedback
		))

		assignment.collectMarks = true

		service.getStagesFor(assignment) should be (Seq(
			AddMarks, AddFeedback, ReleaseFeedback, ViewOnlineFeedback, DownloadFeedback
		))

		assignment.collectSubmissions = true

		service.getStagesFor(assignment) should be (Seq(
			CourseworkWorkflowStages.Submission, DownloadSubmission,
			AddMarks, AddFeedback, ReleaseFeedback, ViewOnlineFeedback, DownloadFeedback
		))

		service.features.turnitin = true
		department.plagiarismDetectionEnabled = true

		service.getStagesFor(assignment) should be (Seq(
			CourseworkWorkflowStages.Submission, CheckForPlagiarism, DownloadSubmission,
			AddMarks, AddFeedback, ReleaseFeedback, ViewOnlineFeedback, DownloadFeedback
		))

		service.features.markingWorkflows = true
		assignment.markingWorkflow = Fixtures.studentsChooseMarkerWorkflow("my workflow")

		service.getStagesFor(assignment) should be (Seq(
			CourseworkWorkflowStages.Submission, CheckForPlagiarism, DownloadSubmission,
			ReleaseForMarking, FirstMarking,
			AddMarks, AddFeedback, ReleaseFeedback, ViewOnlineFeedback, DownloadFeedback
		))

		assignment.markingWorkflow = Fixtures.seenSecondMarkingLegacyWorkflow("my workflow")
		service.getStagesFor(assignment) should be (Seq(
			CourseworkWorkflowStages.Submission, CheckForPlagiarism, DownloadSubmission,
			ReleaseForMarking, FirstMarking, SecondMarking,
			AddMarks, AddFeedback, ReleaseFeedback, ViewOnlineFeedback, DownloadFeedback
		))
	}

	@Test def progress() {
		// Start with a very basic assignment, only the 3 core feedback workflows.
		assignment.collectMarks = false
		assignment.collectSubmissions = false
		assignment.markingWorkflow = null
		department.plagiarismDetectionEnabled = false

		// lines were getting a bit long...
		import CourseworkWorkflowStages._
		import WorkflowStageHealth._
		import WorkflowStages._

		{
			val p = service.progress(assignment)(workflowItems(feedback=None))
			p.stages should be (ListMap(
				"AddFeedback" -> StageProgress(AddFeedback, started = false, "workflow.AddFeedback.notUploaded", Good, completed = false, preconditionsMet = true),
				"ReleaseFeedback" -> StageProgress(ReleaseFeedback, started = false, "workflow.ReleaseFeedback.notReleased", Good, completed = false, preconditionsMet = false),
				"ViewOnlineFeedback" -> StageProgress(ViewOnlineFeedback, started = false, "workflow.ViewOnlineFeedback.notViewed", Good, completed = false, preconditionsMet = false),
				"DownloadFeedback" -> StageProgress(DownloadFeedback, started = false, "workflow.DownloadFeedback.notDownloaded", Good, completed = false, preconditionsMet = false)
			))
			p.percentage should be (0)
			p.nextStage should be (None) // no next stage because we haven't started
			p.messageCode should be ("workflow.AddFeedback.notUploaded")
			p.cssClass should be ("success")
		}

		val feedback = Fixtures.assignmentFeedback("0672089")
		feedback.assignment = assignment
		feedback.attachments.add(new FileAttachment)
		assignment.genericFeedback = "Thanks for not including herons"

		{
			val p = service.progress(assignment)(workflowItems(feedback=Some(feedback)))
			p.stages should be (ListMap(
				"AddFeedback" -> StageProgress(AddFeedback, started = true, "workflow.AddFeedback.uploaded", Good, completed = true, preconditionsMet = true),
				"ReleaseFeedback" -> StageProgress(ReleaseFeedback, started = true, "workflow.ReleaseFeedback.notReleased", Warning, completed = false, preconditionsMet = true),
				"ViewOnlineFeedback" -> StageProgress(ViewOnlineFeedback, started = false, "workflow.ViewOnlineFeedback.notViewed", Good, completed = false, preconditionsMet = false),
				"DownloadFeedback" -> StageProgress(DownloadFeedback, started = false, "workflow.DownloadFeedback.notDownloaded", Good, completed = false, preconditionsMet = false)
			))
			p.percentage should be (50)
			p.nextStage should be (Some(ReleaseFeedback))
			p.messageCode should be ("workflow.ReleaseFeedback.notReleased")
			p.cssClass should be ("warning")
		}

		feedback.released = true


		{
			val p = service.progress(assignment)(workflowItems(feedback=Some(feedback), feedbackDownloaded=false, onlineFeedbackViewed=false))
			p.stages should be (ListMap(
				"AddFeedback" -> StageProgress(AddFeedback, started = true, "workflow.AddFeedback.uploaded", Good, completed = true, preconditionsMet = true),
				"ReleaseFeedback" -> StageProgress(ReleaseFeedback, started = true, "workflow.ReleaseFeedback.released", Good, completed = true, preconditionsMet = true),
				"ViewOnlineFeedback" -> StageProgress(ViewOnlineFeedback, started = true, "workflow.ViewOnlineFeedback.notViewed", Warning, completed = false, preconditionsMet = true),
				"DownloadFeedback" -> StageProgress(DownloadFeedback, started = false, "workflow.DownloadFeedback.notDownloaded", Good, completed = false, preconditionsMet = true)
			))
			p.percentage should be (75)
			p.nextStage should be (Some(ViewOnlineFeedback)) // Complete
			p.messageCode should be ("workflow.ViewOnlineFeedback.notViewed")
			p.cssClass should be ("warning")
		}


		{
			val p = service.progress(assignment)(workflowItems(feedback=Some(feedback), feedbackDownloaded=true, onlineFeedbackViewed=true))
			p.stages should be (ListMap(
				"AddFeedback" -> StageProgress(AddFeedback, started = true, "workflow.AddFeedback.uploaded", Good, completed = true, preconditionsMet = true),
				"ReleaseFeedback" -> StageProgress(ReleaseFeedback, started = true, "workflow.ReleaseFeedback.released", Good, completed = true, preconditionsMet = true),
				"ViewOnlineFeedback" -> StageProgress(ViewOnlineFeedback, started = true, "workflow.ViewOnlineFeedback.viewed", Good, completed = true, preconditionsMet = true),
				"DownloadFeedback" -> StageProgress(DownloadFeedback, started = true, "workflow.DownloadFeedback.downloaded", Good, completed = true, preconditionsMet = true)
			))
			p.percentage should be (100)
			p.nextStage should be (None) // Complete
			p.messageCode should be ("workflow.DownloadFeedback.downloaded")
			p.cssClass should be ("success")
		}

		// TODO do a fuller example that tests the submission and marking bits too
	}

}