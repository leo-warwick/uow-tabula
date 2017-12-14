package uk.ac.warwick.tabula.data.model.markingworkflow

import uk.ac.warwick.tabula.data.model.AbstractStringUserType
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.{DblBlndFinalMarker, DblBlndInitialMarkerA, DblBlndInitialMarkerB, DblFinalMarker, DblFirstMarker, DblSecondMarker, ModerationMarker, ModerationModerator, SelectedModerationAdmin, SelectedModerationMarker, SelectedModerationModerator, SingleMarker}
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.helpers.StringUtils._


/**
	* Available marking workflows
	*/
abstract class MarkingWorkflowType(
	val name: String,
	val description: String,
	val allStages: Seq[MarkingWorkflowStage],
	val initialStages: Seq[MarkingWorkflowStage],
	val order: Int,
	// in most workflows stages that share a role name also share student -> marker allocations
	// in other workflows like double blind stages have their own separate allocations
	val rolesShareAllocations: Boolean = true
){
	val roleNames: Seq[String] = allStages.map(_.roleName).distinct
	def allPreviousStages(stage: MarkingWorkflowStage): Seq[MarkingWorkflowStage] = allStages.filter(_.order < stage.order)
	override def toString: String = name
}

object MarkingWorkflowType {

	implicit val ordering: Ordering[MarkingWorkflowType] = Ordering.by { t: MarkingWorkflowType => t.order }

	case object SingleMarking extends MarkingWorkflowType(
		name = "Single",
		description = "Single marking",
		allStages =  Seq(SingleMarker),
		initialStages = Seq(SingleMarker),
		order = 0
	)

	case object DoubleMarking extends MarkingWorkflowType(
		name = "Double",
		description = "Double seen marking",
		allStages =  Seq(DblFirstMarker, DblSecondMarker, DblFinalMarker),
		initialStages = Seq(DblFirstMarker),
		order = 1
	)

	case object ModeratedMarking extends MarkingWorkflowType(
		name = "Moderated",
		description = "Moderated marking",
		allStages =  Seq(ModerationMarker, ModerationModerator),
		initialStages = Seq(ModerationMarker),
		order = 2
	)

	case object DoubleBlindMarking extends MarkingWorkflowType(
		name = "DoubleBlind",
		description = "Double blind marking",
		allStages =  Seq(DblBlndInitialMarkerA, DblBlndInitialMarkerB, DblBlndFinalMarker),
		initialStages = Seq(DblBlndInitialMarkerA, DblBlndInitialMarkerB),
		order = 3,
		rolesShareAllocations = false
	)

	case object SelectedModeratedMarking extends MarkingWorkflowType(
		name = "SelectedModerated",
		description = "Moderated marking",
		allStages =  Seq(SelectedModerationMarker, SelectedModerationAdmin, SelectedModerationModerator),
		initialStages = Seq(SelectedModerationMarker),
		order = 4
	)

	// Don't change this to a val https://warwick.slack.com/archives/C029QTGBN/p1493995125972397
	def values: Seq[MarkingWorkflowType] = Seq(
		SingleMarking,
		DoubleMarking,
		ModeratedMarking,
		DoubleBlindMarking
		// Don't include - SelectedModeratedMarking in this list (It's not to be selected directly)
	)

	def allPossibleStages: Map[MarkingWorkflowType, Seq[MarkingWorkflowStage]] = values.map(t => t -> t.allStages).toMap

	def fromCode(code: String): MarkingWorkflowType =
		if (code == null) null
		else values.find{_.name == code} match {
			case Some(method) => method
			case None => throw new IllegalArgumentException()
		}
}

class MarkingWorkflowTypeUserType extends AbstractStringUserType[MarkingWorkflowType]{
	override def convertToObject(string: String): MarkingWorkflowType = MarkingWorkflowType.fromCode(string)
	override def convertToValue(state: MarkingWorkflowType): String = state.name
}

class StringToMarkingWorkflowType extends TwoWayConverter[String, MarkingWorkflowType] {
	override def convertRight(source: String): MarkingWorkflowType = source.maybeText.map(MarkingWorkflowType.fromCode).getOrElse(throw new IllegalArgumentException)
	override def convertLeft(source: MarkingWorkflowType): String = Option(source).map { _.name }.orNull
}