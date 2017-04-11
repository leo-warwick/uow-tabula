package uk.ac.warwick.tabula.data.model.markingworkflow

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage._
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.DoubleMarking
import uk.ac.warwick.userlookup.User

@Entity @DiscriminatorValue("Double")
class DoubleWorkflow extends CM2MarkingWorkflow {
	def workflowType = DoubleMarking

	override def replaceMarkers(markers: Seq[Marker]*): Unit = {
		val (firstMarkers, secondMarkers) = markers.toList match {
			case fm :: sm :: _ => (fm, sm)
			case _ => throw new IllegalArgumentException("Must add a list of firstMarkers and secondMarkers")
		}

		replaceStageMarkers(DblFirstMarker, firstMarkers)
		replaceStageMarkers(DblSecondMarker, secondMarkers)
		replaceStageMarkers(DblFinalMarker, firstMarkers)
	}
}

object DoubleWorkflow {
	def apply(name: String, department: Department, firstMarkers: Seq[User], secondMarkers: Seq[User]) : DoubleWorkflow = {
		val workflow = new DoubleWorkflow
		workflow.name = name
		workflow.department = department

		val firstMarkersStage = new StageMarkers
		firstMarkersStage.stage = DblFirstMarker
		firstMarkersStage.workflow = workflow

		val finalMarkerStage = new StageMarkers
		finalMarkerStage.stage = DblFinalMarker
		finalMarkerStage.workflow = workflow

		firstMarkers.foreach(user => {
			firstMarkersStage.markers.knownType.add(user)
			finalMarkerStage.markers.knownType.add(user)
		})

		val secondMarkerStage = new StageMarkers
		secondMarkerStage.stage = DblSecondMarker
		secondMarkerStage.workflow = workflow
		secondMarkers.foreach(secondMarkerStage.markers.knownType.add)

		workflow.stageMarkers = JList(firstMarkersStage, secondMarkerStage, finalMarkerStage)

		workflow
	}
}
