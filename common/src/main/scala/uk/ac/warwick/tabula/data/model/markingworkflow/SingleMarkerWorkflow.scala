package uk.ac.warwick.tabula.data.model.markingworkflow

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.SingleMarker
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.SingleMarking
import uk.ac.warwick.userlookup.User

@Entity @DiscriminatorValue("Single")
class SingleMarkerWorkflow extends CM2MarkingWorkflow {
	def workflowType = SingleMarking

	override def replaceMarkers(markers: Seq[Marker]*): Unit = {
		val firstMarkers = markers.toList match {
			case fm :: _ => fm
			case _ => throw new IllegalArgumentException("Must add a list of markers")
		}

		replaceStageMarkers(SingleMarker, firstMarkers)
	}

}

object SingleMarkerWorkflow {
	def apply(name: String, department: Department, firstMarkers: Seq[User]): SingleMarkerWorkflow = {

		val singleWorkflow = new SingleMarkerWorkflow
		singleWorkflow.name = name
		singleWorkflow.department = department

		val singleMarkers = new StageMarkers()
		singleMarkers.stage = SingleMarker
		singleMarkers.workflow = singleWorkflow
		firstMarkers.foreach(singleMarkers.markers.knownType.add)

		singleWorkflow.stageMarkers = JList(singleMarkers)

		singleWorkflow
	}
}