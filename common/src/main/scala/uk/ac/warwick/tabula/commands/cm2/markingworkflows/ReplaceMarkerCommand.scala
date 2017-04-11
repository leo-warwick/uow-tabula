package uk.ac.warwick.tabula.commands.cm2.markingworkflows

import org.springframework.validation.Errors

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.commands.{Describable, Description, _}
import uk.ac.warwick.tabula.data.model.{Assignment, Department}
import uk.ac.warwick.tabula.data.model.markingworkflow._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User


object ReplaceMarkerCommand {

	type Command = Appliable[CM2MarkingWorkflow] with ReplaceMarkerState

	def apply(department:Department, markingWorkflow: CM2MarkingWorkflow) =
		new ReplaceMarkerCommandInternal(department, markingWorkflow)
			with ComposableCommand[CM2MarkingWorkflow]
			with MarkingWorkflowPermissions
			with ReplaceMarkerDescription
			with ReplaceMarkerState
			with ReplaceMarkerValidation
			with AutoWiringCM2MarkingWorkflowServiceComponent
			with AutowiringUserLookupComponent
}

class ReplaceMarkerCommandInternal(val department: Department, val markingWorkflow: CM2MarkingWorkflow)
	extends CommandInternal[CM2MarkingWorkflow] {

	self: ReplaceMarkerState with CM2MarkingWorkflowServiceComponent with UserLookupComponent =>

	def applyInternal(): CM2MarkingWorkflow = {
		val stages = markingWorkflow.workflowType.allStages

		for(stage <- stages) {
			for(assignment <- assignmentsToUpdate) {
				val existingAllocations = cm2MarkingWorkflowService.getMarkerAllocations(assignment, stage)
				val students = existingAllocations.getOrElse(oldMarkerUser, Set())
				val newAllocations = (existingAllocations - oldMarkerUser) + (newMarkerUser -> students)
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
		if (newMarker.hasText && !userLookup.getUserByUserId(newMarker).isFoundUser){
			errors.rejectValue("newMarker", "markingWorkflow.marker.unknownUser")
		}
		if (!confirm) {
			errors.rejectValue("confirm", "markingWorkflow.marker.confirm")
		}
	}

}

trait ReplaceMarkerDescription extends Describable[CM2MarkingWorkflow] {
	self: ReplaceMarkerState =>
	def describe(d: Description) {
		d.department(department)
		d.markingWorkflow(markingWorkflow)
		d.properties(("assignments", assignmentsToUpdate.map(_.id)), ("oldMarker", oldMarker), ("newMarker", newMarker))
	}
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

	lazy val allMarkers: Seq[User] = markingWorkflow.markers.values.flatten.toSeq.distinct.sortBy(u => (u.getLastName, u.getFirstName))

	lazy val affectedAssignments: Set[Assignment] = markingWorkflow.assignments.asScala.toSet

	// at the point in time when this command runs a 'finished' assignment is one where all the feedback associated with a marker has been published
	// feedback associated with a marker is feedback that has that marker in at least one of it's workflow stages
	lazy val finishedAssignments: Set[Assignment] = affectedAssignments.filter(assignment => {
		val feedbackFromOldMarker = assignment.allFeedback
			.filter(_.markerFeedback.asScala.exists(_.marker == oldMarkerUser))
		feedbackFromOldMarker.nonEmpty && feedbackFromOldMarker.forall(_.released)
	})

	lazy val unfinishedAssignments: Set[Assignment] = affectedAssignments -- finishedAssignments

	def assignmentsToUpdate: Set[Assignment] = if(includeCompleted) affectedAssignments else unfinishedAssignments
}