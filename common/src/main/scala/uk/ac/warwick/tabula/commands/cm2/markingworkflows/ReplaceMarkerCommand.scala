package uk.ac.warwick.tabula.commands.cm2.markingworkflows

import org.springframework.validation.Errors

import scala.jdk.CollectionConverters._
import uk.ac.warwick.tabula.commands.{Describable, Description, _}
import uk.ac.warwick.tabula.data.model.{Assignment, Department}
import uk.ac.warwick.tabula.data.model.markingworkflow._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.UserOrdering._


object ReplaceMarkerCommand {

  type Command = Appliable[CM2MarkingWorkflow] with ReplaceMarkerState

  def apply(department: Department, markingWorkflow: CM2MarkingWorkflow) =
    new ReplaceMarkerCommandInternal(department, markingWorkflow)
      with ComposableCommand[CM2MarkingWorkflow]
      with MarkingWorkflowPermissions
      with ReplaceMarkerDescription
      with ReplaceMarkerState
      with ReplaceMarkerValidation
      with AutowiringCM2MarkingWorkflowServiceComponent
      with AutowiringUserLookupComponent
}

class ReplaceMarkerCommandInternal(val department: Department, val markingWorkflow: CM2MarkingWorkflow)
  extends CommandInternal[CM2MarkingWorkflow] {

  self: ReplaceMarkerState with CM2MarkingWorkflowServiceComponent with UserLookupComponent =>

  def applyInternal(): CM2MarkingWorkflow = {
    // only do replacements on stages containing the old marker
    val stagesToReplace = markingWorkflow.markers.filter { case (_, v) => v.contains(oldMarkerUser) }.keys
    for (stage <- stagesToReplace) {
      for (assignment <- assignmentsToUpdate) {
        val existingAllocations = cm2MarkingWorkflowService.getMarkerAllocations(assignment, stage)
        val newStudents = existingAllocations.getOrElse(oldMarkerUser, Set())
        val existingStudents = existingAllocations.getOrElse(newMarkerUser, Set())
        val newAllocations = (existingAllocations - oldMarkerUser - newMarkerUser) + (newMarkerUser -> existingStudents.union(newStudents))
        cm2MarkingWorkflowService.allocateMarkersForStage(assignment, stage, newAllocations)
      }

      // if there are no finished assignments or we are swapping the marker for finished assignments - remove them from the stage
      if (finishedAssignments.isEmpty || includeCompleted) {
        cm2MarkingWorkflowService.removeMarkersForStage(markingWorkflow, stage, Seq(oldMarkerUser))
      }
      cm2MarkingWorkflowService.addMarkersForStage(markingWorkflow, stage, Seq(newMarkerUser))
    }
    markingWorkflow
  }
}

trait ReplaceMarkerValidation extends SelfValidating {

  self: ReplaceMarkerState with UserLookupComponent =>

  override def validate(errors: Errors) {
    if (!oldMarker.hasText) {
      errors.rejectValue("oldMarker", "markingWorkflow.marker.none")
    }
    if (!newMarker.hasText) {
      errors.rejectValue("newMarker", "markingWorkflow.marker.none")
    }
    if (oldMarker.hasText && !allMarkers.exists(u => u.getUserId == oldMarker)) {
      errors.rejectValue("oldMarker", "markingWorkflow.marker.notOldMarker")
    }
    if (newMarker.hasText && !userLookup.getUserByUserId(newMarker).isFoundUser) {
      errors.rejectValue("newMarker", "markingWorkflow.marker.unknownUser")
    }
    if (newMarkerUser.isFoundUser && oldMarkerUser == newMarkerUser) {
      errors.rejectValue("newMarker", "markingWorkflow.marker.sameMarker")
    }
    if (!confirm) {
      errors.rejectValue("confirm", "markingWorkflow.marker.confirm")
    }
  }

}

trait ReplaceMarkerDescription extends Describable[CM2MarkingWorkflow] {
  self: ReplaceMarkerState =>

  override lazy val eventName: String = "ReplaceMarker"

  def describe(d: Description): Unit =
    d.department(department)
     .markingWorkflow(markingWorkflow)
     .properties(
      "assignments" -> assignmentsToUpdate.map(_.id),
      "oldMarker" -> oldMarker,
      "newMarker" -> newMarker
    )
}

trait ReplaceMarkerState {

  this: UserLookupComponent =>

  def department: Department

  def markingWorkflow: CM2MarkingWorkflow

  var oldMarker: String = _
  var newMarker: String = _
  var includeCompleted: Boolean = false
  var confirm: Boolean = false

  lazy val oldMarkerUser: User = userLookup.getUserByUserId(oldMarker)
  lazy val newMarkerUser: User = userLookup.getUserByUserId(newMarker)

  lazy val allMarkers: Seq[User] = markingWorkflow.markers.values.flatten.toSeq.distinct.sorted

  lazy val affectedAssignments: Set[Assignment] = markingWorkflow.assignments.asScala.toSet

  // at the point in time when this command runs a 'finished' assignment is one where all the feedback associated with a marker has been published
  // feedback associated with a marker is feedback that has that marker in at least one of it's workflow stages
  lazy val finishedAssignments: Set[Assignment] = affectedAssignments.filter(assignment => {
    val feedbackFromOldMarker = assignment.allFeedback
      .filter(_.allMarkerFeedback.exists(_.marker == oldMarkerUser))
    feedbackFromOldMarker.nonEmpty && feedbackFromOldMarker.forall(_.released)
  })

  lazy val unfinishedAssignments: Set[Assignment] = affectedAssignments -- finishedAssignments

  def assignmentsToUpdate: Set[Assignment] = if (includeCompleted) affectedAssignments else unfinishedAssignments
}
