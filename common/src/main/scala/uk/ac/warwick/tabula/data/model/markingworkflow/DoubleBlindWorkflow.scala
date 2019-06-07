package uk.ac.warwick.tabula.data.model.markingworkflow

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.{DblBlndFinalMarker, DblBlndInitialMarkerA, DblBlndInitialMarkerB}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.DoubleBlindMarking
import uk.ac.warwick.userlookup.User

@Entity
@Proxy(`lazy` = false)
@DiscriminatorValue("DoubleBlind")
class DoubleBlindWorkflow extends CM2MarkingWorkflow {
  def workflowType = DoubleBlindMarking

  override def replaceMarkers(markers: Seq[Marker]*): Unit = {
    val (initialMarkers, finalMarkers) = markers.toList match {
      case fm :: sm :: _ => (fm, sm)
      case _ => throw new IllegalArgumentException("Must add a list of initialMarkers and finalMarkers")
    }

    replaceStageMarkers(DblBlndInitialMarkerA, initialMarkers)
    replaceStageMarkers(DblBlndInitialMarkerB, initialMarkers)
    replaceStageMarkers(DblBlndFinalMarker, finalMarkers)
  }
}

object DoubleBlindWorkflow {
  def apply(name: String, department: Department, initialMarkers: Seq[User], finalMarkers: Seq[User]): DoubleBlindWorkflow = {
    val workflow = new DoubleBlindWorkflow
    workflow.name = name
    workflow.department = department

    val initialAMarkers = new StageMarkers
    initialAMarkers.stage = DblBlndInitialMarkerA
    initialAMarkers.workflow = workflow

    val initialBMarkers = new StageMarkers
    initialBMarkers.stage = DblBlndInitialMarkerB
    initialBMarkers.workflow = workflow

    initialMarkers.foreach(user => {
      initialAMarkers.markers.add(user)
      initialBMarkers.markers.add(user)
    })

    val lastMarkers = new StageMarkers
    lastMarkers.stage = DblBlndFinalMarker
    lastMarkers.workflow = workflow
    finalMarkers.foreach(lastMarkers.markers.add)

    workflow.stageMarkers = JList(initialAMarkers, initialBMarkers, lastMarkers)

    workflow
  }
}
