package uk.ac.warwick.tabula.helpers.cm2

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.{WorkflowStage, WorkflowStages}
import uk.ac.warwick.userlookup.User

import scala.collection.immutable.ListMap

case class SubmissionListItem(submission: Submission, downloaded: Boolean)
case class ExtensionListItem (extension: Extension,	within: Boolean)
case class FeedbackListItem(feedback: Feedback, downloaded: Boolean, onlineViewed: Boolean, feedbackForSits: FeedbackForSits)
case class Progress (percentage: Int, t: String, messageCode: String)
// Simple object holder
case class WorkFlowStudent (
	user: User,
	progress: Progress,
	nextStage: Option[WorkflowStage],
	stages: ListMap[String, WorkflowStages.StageProgress],
	coursework: WorkflowItems,
	assignment:Assignment,
	disability: Option[Disability]
)
case class WorkflowItems (
	student: User,
	enhancedSubmission: Option[SubmissionListItem],
	enhancedFeedback: Option[FeedbackListItem],
	enhancedExtension: Option[ExtensionListItem]
)