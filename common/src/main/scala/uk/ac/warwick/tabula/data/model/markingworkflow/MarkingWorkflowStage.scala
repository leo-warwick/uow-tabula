package uk.ac.warwick.tabula.data.model.markingworkflow

import org.hibernate.`type`.{StandardBasicTypes, StringType}
import uk.ac.warwick.tabula.CaseObjectEqualityFixes
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.markingworkflow.ModerationSampler.Marker
import uk.ac.warwick.tabula.data.model.{AbstractBasicUserType, Assignment, Feedback}
import uk.ac.warwick.tabula.helpers.StringUtils
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.TwoWayConverter

import java.sql.Types

sealed abstract class MarkingWorkflowStage(val name: String, val order: Int) extends CaseObjectEqualityFixes[MarkingWorkflowStage] {
  override def getName: String = name

  def roleName: String = MarkingWorkflowStage.DefaultRole

  def verb: String = MarkingWorkflowStage.DefaultVerb

  def pastVerb: String = MarkingWorkflowStage.DefaultPastVerb

  def presentVerb: String = MarkingWorkflowStage.DefaultPresentVerb

  // used when stages have their own allocations rather than allocations being at the roleName level
  def allocationName: String = roleName

  // used to describe a stage - when two stages share a role name (the same person is responsible for both stages) these will need to be distinct
  def description: String = roleName

  def previousStages: Seq[MarkingWorkflowStage] = Nil

  def nextStages: Seq[MarkingWorkflowStage] = Nil

  def otherStagesInSequence: Set[MarkingWorkflowStage] = Set.empty

  // get a description of the next stage - default works best when there is only one next stage - may need overriding in other cases
  def nextStagesDescription: Option[String] = nextStages.headOption.map(_.description)

  // allows you to specify a custom URL to show on the department homepage when viewing this assignments workflow progress
  def url(assignment: Assignment): Option[String] = None

  // allows the message key used to label this stages completion to vary
  def actionCompletedKey(feedback: Option[Feedback]): String = MarkingWorkflowStage.DefaultCompletionKey

  override def toString: String = name

  // should users at this stage be able to complete feedback skipping any subsequent stages
  def canFinish(markingWorkflow: CM2MarkingWorkflow): Boolean = false

  // should users at this stage be able to complete feedback skipping the current stage any subsequent stages
  def canSkipAndFinish: Boolean = false

  // should the online marker and upload marks commands be pre-populated with the previous stages feedback
  def populateWithPreviousFeedback: Boolean = false

  // should the stage show the mark in ListMarkerFeedback
  def summariseCurrentFeedback: Boolean = false

  // should the stage show the mark and marker name for the previous feedback in ListMarkerFeedback
  def summarisePreviousFeedback: Boolean = false

  // by default allocation is based by role rather than by stage - stage allocation is needed when there are concurrent stages for the same role (double blind has two streams of markers for example).
  def stageAllocation: Boolean = false

  // if this is false this stage won't show up on the allocate markers screen - some stages are completed by admins only so they don't need to be allocated
  def hasMarkers: Boolean = true

  // allows the marker to make bulk adjustments - at time of writing this is intended for moderators only but that could be changed
  def allowsBulkAdjustments: Boolean = false
}

abstract class FinalStage(n: String) extends MarkingWorkflowStage(name = n, order = Int.MaxValue) {
  override def roleName: String = "Admin"
}

object ModerationStage {
  val NotModeratedKey: String = "notModerated"
  val ApprovedKey: String = "approved"
}

abstract class ModerationStage(name: String, order: Int) extends MarkingWorkflowStage(name, order) {
  override def roleName: String = "Moderator"

  override def verb: String = "moderate"

  override def pastVerb: String = "moderated"

  override def presentVerb: String = "moderating"

  override def populateWithPreviousFeedback: Boolean = true

  override def summarisePreviousFeedback: Boolean = true

  override def summariseCurrentFeedback: Boolean = true

  override def allowsBulkAdjustments: Boolean = true

  override def actionCompletedKey(feedback: Option[Feedback]): String = feedback match {
    case Some(f) if f.wasModerated && !f.moderatorMadeChanges => ModerationStage.ApprovedKey
    case Some(f) if !f.wasModerated => ModerationStage.NotModeratedKey
    case _ => super.actionCompletedKey(feedback)
  }
}

/**
 * Stages model the steps in any given workflow.
 * Stage names are exposed in URLs so don't use upper case characters
 */
object MarkingWorkflowStage {

  val DefaultRole: String = "Marker"
  val DefaultVerb: String = "mark"
  val DefaultPastVerb: String = "marked"
  val DefaultPresentVerb: String = "marking"
  val DefaultCompletionKey: String = "complete"

  // single marker workflow
  case object SingleMarker extends MarkingWorkflowStage("single-marker", 1) {
    override def nextStages: Seq[MarkingWorkflowStage] = Seq(SingleMarkingCompleted)
  }

  case object SingleMarkingCompleted extends FinalStage("single-marking-completed") {
    override def previousStages: Seq[MarkingWorkflowStage] = Seq(SingleMarker)
  }

  // double marker workflow
  case object DblFirstMarker extends MarkingWorkflowStage("dbl-first-marker", 1) {
    override def description: String = "First marker"

    override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblSecondMarker)

    override def otherStagesInSequence: Set[MarkingWorkflowStage] = Set(DblSecondMarker)
  }

  case object DblSecondMarker extends MarkingWorkflowStage("dbl-second-marker", 2) {
    override def roleName: String = "Second marker"

    override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblFinalMarker)

    override def previousStages: Seq[MarkingWorkflowStage] = Seq(DblFirstMarker)

    override def otherStagesInSequence: Set[MarkingWorkflowStage] = Set(DblFirstMarker)
  }

  case object DblFinalMarker extends MarkingWorkflowStage("dbl-final-marker", 3) {
    override def description: String = "Final marker"

    override def verb: String = "finalise"

    override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblCompleted)

    override def previousStages: Seq[MarkingWorkflowStage] = Seq(DblSecondMarker)
  }

  case object DblCompleted extends FinalStage("dbl-Completed") {
    override def previousStages: Seq[MarkingWorkflowStage] = Seq(DblFinalMarker)
  }

  // double blind workflow
  case object DblBlndInitialMarkerA extends MarkingWorkflowStage("dbl-blnd-marker-a", 1) {
    override def roleName: String = "Independent marker"

    override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblBlndFinalMarker)

    override def allocationName = "First independent marker"

    override def stageAllocation = true

    override def description = "First independent marker"

    override def otherStagesInSequence: Set[MarkingWorkflowStage] = Set(DblBlndFinalMarker)
  }

  case object DblBlndInitialMarkerB extends MarkingWorkflowStage("dbl-blnd-marker-b", 1) {
    override def roleName: String = "Independent marker"

    override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblBlndFinalMarker)

    override def allocationName = "Second independent marker"

    override def stageAllocation = true

    override def description = "Second independent marker"

    override def otherStagesInSequence: Set[MarkingWorkflowStage] = Set(DblBlndFinalMarker)
  }

  case object DblBlndFinalMarker extends MarkingWorkflowStage("dbl-blnd-final-marker", 2) {
    override def roleName = "Final marker"

    override def verb = "finalise"

    override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblBlndCompleted)

    override def previousStages: Seq[MarkingWorkflowStage] = Seq(DblBlndInitialMarkerA, DblBlndInitialMarkerB)

    override def description = "Final marker"

    override def otherStagesInSequence: Set[MarkingWorkflowStage] = Set(DblBlndInitialMarkerA, DblBlndInitialMarkerB)
  }

  case object DblBlndCompleted extends FinalStage("dbl-blnd-completed") {
    override def previousStages: Seq[MarkingWorkflowStage] = Seq(DblBlndFinalMarker)
  }

  // moderated workflow
  case object ModerationMarker extends MarkingWorkflowStage("moderation-marker", 1) {
    override def nextStages: Seq[MarkingWorkflowStage] = Seq(ModerationModerator)

    override def canFinish(workflow: CM2MarkingWorkflow): Boolean = Option(HibernateHelpers.initialiseAndUnproxy(workflow))
      .collect { case w: ModeratedWorkflow => w }
      .exists(_.moderationSampler == Marker)

    override def summariseCurrentFeedback: Boolean = true

    override def otherStagesInSequence: Set[MarkingWorkflowStage] = Set(ModerationModerator)
  }

  case object ModerationModerator extends ModerationStage("moderation-moderator", 2) {

    override def nextStages: Seq[MarkingWorkflowStage] = Seq(ModerationCompleted)

    override def previousStages: Seq[MarkingWorkflowStage] = Seq(ModerationMarker)

    override def canSkipAndFinish: Boolean = true

    override def otherStagesInSequence: Set[MarkingWorkflowStage] = Set(ModerationMarker)
  }

  case object ModerationCompleted extends FinalStage("moderation-completed") {
    override def previousStages: Seq[MarkingWorkflowStage] = Seq(ModerationModerator)
  }

  // moderated workflow with admin selection
  case object SelectedModerationMarker extends MarkingWorkflowStage("admin-moderation-marker", 1) {
    override def nextStages: Seq[MarkingWorkflowStage] = Seq(SelectedModerationAdmin)

    override def otherStagesInSequence: Set[MarkingWorkflowStage] = Set(SelectedModerationModerator)
  }

  // moderated workflow with admin selection
  case object SelectedModerationAdmin extends MarkingWorkflowStage("admin-moderation-admin", 2) {
    override def roleName = "Admin"

    override def verb: String = "select"

    override def pastVerb: String = "selected"

    override def presentVerb: String = "selecting"

    override def nextStages: Seq[MarkingWorkflowStage] = Seq(SelectedModerationModerator)

    override def previousStages: Seq[MarkingWorkflowStage] = Seq(SelectedModerationMarker)

    override def canFinish(workflow: CM2MarkingWorkflow): Boolean = true

    override def populateWithPreviousFeedback: Boolean = true

    override def summariseCurrentFeedback: Boolean = true

    override def url(assignment: Assignment): Option[String] = Some(Routes.admin.assignment.submissionsandfeedback(assignment))

    override def hasMarkers = false

    val NotSelectedForModerationKey: String = "notSelected"

    override def actionCompletedKey(feedback: Option[Feedback]): String = {
      val markerFeedback = feedback.flatMap(_.allMarkerFeedback.find(_.stage == SelectedModerationModerator))
      markerFeedback match {
        case Some(mf) if !mf.hasContent => NotSelectedForModerationKey
        case _ => super.actionCompletedKey(feedback)
      }
    }
  }

  case object SelectedModerationModerator extends ModerationStage("admin-moderation-moderator", 3) {

    override def nextStages: Seq[MarkingWorkflowStage] = Seq(SelectedModerationCompleted)

    override def previousStages: Seq[MarkingWorkflowStage] = Seq(SelectedModerationMarker)

    override def otherStagesInSequence: Set[MarkingWorkflowStage] = Set(SelectedModerationMarker)
  }

  case object SelectedModerationCompleted extends FinalStage("admin-moderation-completed") {
    override def previousStages: Seq[MarkingWorkflowStage] = Seq(SelectedModerationModerator)
  }

  // lame manual collection. Keep in sync with the case objects above
  // Don't change this to a val https://warwick.slack.com/archives/C029QTGBN/p1493995125972397
  def values: Set[MarkingWorkflowStage] = Set(
    SingleMarker, SingleMarkingCompleted,
    DblFirstMarker, DblSecondMarker, DblFinalMarker, DblCompleted,
    DblBlndInitialMarkerA, DblBlndInitialMarkerB, DblBlndFinalMarker, DblBlndCompleted,
    ModerationMarker, ModerationModerator, ModerationCompleted,
    SelectedModerationMarker, SelectedModerationAdmin, SelectedModerationModerator, SelectedModerationCompleted,
  )

  def unapply(code: String): Option[MarkingWorkflowStage] =
    code.maybeText.flatMap { code =>
      values.find(_.name == code)
    }

  def fromAllocationName(allocationName: String, workflowType: MarkingWorkflowType): Option[MarkingWorkflowStage] =
    values.filter(workflowType.allStages.contains).find(_.allocationName == allocationName)

  //WorkflowStage generated based on either MarkingWorkflowStage's allocation/role name (when MarkingWorkflowType non null)  or MarkingWorkflowStage's name (when marking workflow  as null)
  def fromCode(code: String, workflowType: MarkingWorkflowType = null): MarkingWorkflowStage = code match {
    case null => null
    case allocationName if workflowType != null => MarkingWorkflowStage.fromAllocationName(allocationName, workflowType).getOrElse(throw new IllegalArgumentException(s"Invalid allocation name for ${workflowType.name}: $code"))
    case MarkingWorkflowStage(s) => s
    case _ => throw new IllegalArgumentException(s"Invalid marking stage: $code")
  }

  implicit val defaultOrdering: Ordering[MarkingWorkflowStage] {
    def compare(a: MarkingWorkflowStage, b: MarkingWorkflowStage): Int
  } = new Ordering[MarkingWorkflowStage] {
    def compare(a: MarkingWorkflowStage, b: MarkingWorkflowStage): Int = {
      val orderCompare = a.order compare b.order
      if (orderCompare != 0) orderCompare else StringUtils.AlphaNumericStringOrdering.compare(a.name, b.name)
    }
  }

}

class MarkingWorkflowStageUserType extends AbstractBasicUserType[MarkingWorkflowStage, String] {

  val basicType: StringType = StandardBasicTypes.STRING

  override def sqlTypes = Array(Types.VARCHAR)

  val nullValue: Null = null
  val nullObject: Null = null

  override def convertToObject(string: String): MarkingWorkflowStage = MarkingWorkflowStage.fromCode(string)

  override def convertToValue(stage: MarkingWorkflowStage): String = stage.name
}

class StringToMarkingWorkflowStage extends TwoWayConverter[String, MarkingWorkflowStage] {
  override def convertRight(source: String): MarkingWorkflowStage = source.maybeText.map(MarkingWorkflowStage.fromCode(_)).getOrElse(throw new IllegalArgumentException)

  override def convertLeft(source: MarkingWorkflowStage): String = Option(source).map(_.name).orNull
}
