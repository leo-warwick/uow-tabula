package uk.ac.warwick.tabula.data.model.markingworkflow

import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

import uk.ac.warwick.tabula.CaseObjectEqualityFixes
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.AbstractBasicUserType
import uk.ac.warwick.tabula.data.model.markingworkflow.ModerationSampler.Marker
import uk.ac.warwick.tabula.helpers.StringUtils
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.helpers.StringUtils._

sealed abstract class MarkingWorkflowStage(val name: String, val order: Int) extends CaseObjectEqualityFixes[MarkingWorkflowStage] {
	override def getName: String = name

	def roleName: String = MarkingWorkflowStage.DefaultRole
	def verb: String = MarkingWorkflowStage.DefaultVerb
	// used when stages have their own allocations rather than allocations being at the roleName level
	def allocationName: String = roleName
	// used to describe a stage - when two stages share a role name (the same person is responsible for both stages) these will need to be distinct
	def description: String = roleName

	def previousStages: Seq[MarkingWorkflowStage] = Nil
	def nextStages: Seq[MarkingWorkflowStage] = Nil

	// should the online marker and upload marks commands be pre-populated with the previous stages feedback
	def populateWithPreviousFeedback: Boolean = false

	// should users at this stage be able to complete feedback skipping any intermediate stage
	def canFinish(markingWorkflow: CM2MarkingWorkflow): Boolean = false

	// should the stage show the mark in ListMarkerFeedback
	def summariseCurrentFeedback: Boolean = false

	// should the stage show the mark and marker name for the previous feedback in ListMarkerFeedback
	def summarisePreviousFeedback: Boolean = false

	// get a description of the next stage - default works best when there is only one next stage - may need overriding in other cases
	def nextStagesDescription: Option[String] = nextStages.headOption.map(_.description)

	// by default allocation is based by role rather than by stage
	def stageAllocation: Boolean = false
	override def toString: String = name
}

abstract class FinalStage(n: String) extends MarkingWorkflowStage(name = n, order = Int.MaxValue) {
	override def roleName: String = "Admin"
}

/**
	* Stages model the steps in any given workflow.
	* Stage names are exposed in URLs so don't use upper case characters
	*/
object MarkingWorkflowStage {

	val DefaultRole: String = "Marker"
	val DefaultVerb: String = "mark"

	// single marker workflow
	case object SingleMarker extends MarkingWorkflowStage("single-marker", 1) {
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(SingleMarkingCompleted)
	}
	case object SingleMarkingCompleted extends FinalStage("single-marking-completed") {
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(SingleMarker)
	}

	// double marker workflow
	case object DblFirstMarker extends MarkingWorkflowStage("dbl-first-marker", 1) {
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblSecondMarker)
	}
	case object DblSecondMarker extends MarkingWorkflowStage("dbl-second-marker", 2) {
		override def roleName: String = "Second marker"
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblFinalMarker)
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(DblFirstMarker)
	}
	case object DblFinalMarker extends MarkingWorkflowStage("dbl-final-marker", 3) {
		override def verb: String = "finalise"
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblCompleted)
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(DblSecondMarker)
		override def description: String = "Final marker"
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
	}
	case object DblBlndInitialMarkerB extends MarkingWorkflowStage("dbl-blnd-marker-b", 1) {
		override def roleName: String = "Independent marker"
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblBlndFinalMarker)
		override def allocationName = "Second independent marker"
		override def stageAllocation = true
	}
	case object DblBlndFinalMarker extends MarkingWorkflowStage("dbl-blnd-final-marker", 2) {
		override def roleName = "Final marker"
		override def verb = "finalise"
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblBlndCompleted)
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(DblBlndInitialMarkerA, DblBlndInitialMarkerA)
		override def description = "Final marker"
	}
	case object DblBlndCompleted extends FinalStage("dbl-blnd-completed") {
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(DblBlndFinalMarker)
	}

	// moderated workflow
	case object ModerationMarker extends MarkingWorkflowStage("moderation-marker", 1) {
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(ModerationModerator)
		override def canFinish(workflow: CM2MarkingWorkflow): Boolean = Option(HibernateHelpers.initialiseAndUnproxy(workflow))
			.collect{case w: ModeratedWorkflow => w}
			.exists(_.moderationSampler == Marker)
		override def summariseCurrentFeedback: Boolean = true
	}
	case object ModerationModerator extends MarkingWorkflowStage("moderation-moderator", 2) {
		override def roleName = "Moderator"
		override def verb: String = "moderate"
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(ModerationCompleted)
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(ModerationMarker)
		override def populateWithPreviousFeedback: Boolean = true
		override def summarisePreviousFeedback: Boolean = true
	}
	case object ModerationCompleted extends FinalStage("moderation-completed") {
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(ModerationModerator)
	}

	// lame manual collection. Keep in sync with the case objects above
	// Don't change this to a val https://warwick.slack.com/archives/C029QTGBN/p1493995125972397
	def values: Set[MarkingWorkflowStage] = Set(
		SingleMarker, SingleMarkingCompleted,
		DblFirstMarker, DblSecondMarker, DblFinalMarker, DblCompleted,
		DblBlndInitialMarkerA, DblBlndInitialMarkerB, DblBlndFinalMarker, DblBlndCompleted,
		ModerationMarker, ModerationModerator, ModerationCompleted
	)

	def unapply(code: String): Option[MarkingWorkflowStage] =
		code.maybeText.flatMap { code =>
			values.find(_.name == code).orElse(values.find(_.allocationName == code))
		}

	def fromCode(code: String): MarkingWorkflowStage = code match {
		case null => null
		case MarkingWorkflowStage(s) => s
		case _ => throw new IllegalArgumentException(s"Invalid marking stage: $code")
	}

	implicit val defaultOrdering = new Ordering[MarkingWorkflowStage] {
		def compare(a: MarkingWorkflowStage, b: MarkingWorkflowStage): Int = {
			val orderCompare = a.order compare b.order
			if (orderCompare != 0) orderCompare else StringUtils.AlphaNumericStringOrdering.compare(a.name, b.name)
		}
	}
}

class MarkingWorkflowStageUserType extends AbstractBasicUserType[MarkingWorkflowStage, String]{

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String): MarkingWorkflowStage = MarkingWorkflowStage.fromCode(string)
	override def convertToValue(stage: MarkingWorkflowStage): String = stage.name
}

class StringToMarkingWorkflowStage extends TwoWayConverter[String, MarkingWorkflowStage] {
	override def convertRight(source: String): MarkingWorkflowStage = source.maybeText.map(MarkingWorkflowStage.fromCode).getOrElse(throw new IllegalArgumentException)
	override def convertLeft(source: MarkingWorkflowStage): String = Option(source).map { _.name }.orNull
}