package uk.ac.warwick.tabula.data.model.markingworkflow

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.{ModerationMarker, ModerationModerator, SingleMarker}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.ModeratedMarking
import uk.ac.warwick.userlookup.User

@Entity @DiscriminatorValue("Moderated")
class ModeratedWorkflow extends CM2MarkingWorkflow {
	def workflowType = ModeratedMarking

	override def replaceMarkers(markers: Seq[Marker]*): Unit = {
		val (firstMarkers, moderators) = markers.toList match {
			case fm :: sm :: _ => (fm, sm)
			case _ => throw new IllegalArgumentException("Must add a list of markers and moderators")
		}

		replaceStageMarkers(ModerationMarker, firstMarkers)
		replaceStageMarkers(ModerationModerator, moderators)
	}
}

object ModeratedWorkflow {
	def apply(name: String, department: Department, markers: Seq[User], moderators: Seq[User]): ModeratedWorkflow = {
		val workflow = new ModeratedWorkflow
		workflow.name = name
		workflow.department = department

		val markersStage = new StageMarkers
		markersStage.stage = ModerationMarker
		markersStage.workflow = workflow
		markers.foreach(markersStage.markers.knownType.add)

		// TODO - may not need this ?
		//val allocationStage = new StageMarkers
		//allocationStage.stage = ???
		//allocationStage.workflow = workflow

		val moderatorStage = new StageMarkers
		moderatorStage.stage = ModerationModerator
		moderatorStage.workflow = workflow
		moderators.foreach(moderatorStage.markers.knownType.add)

		workflow.stageMarkers = JList(markersStage, moderatorStage)
		workflow
	}
}