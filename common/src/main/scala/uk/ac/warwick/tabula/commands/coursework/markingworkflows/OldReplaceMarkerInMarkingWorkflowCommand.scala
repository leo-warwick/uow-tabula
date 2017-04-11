package uk.ac.warwick.tabula.commands.coursework.markingworkflows

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object OldReplaceMarkerInMarkingWorkflowCommand {
	def apply(department: Department, markingWorkflow: MarkingWorkflow) =
		new ReplaceMarkerInMarkingWorkflowCommandInternal(department, markingWorkflow)
			with AutowiringUserLookupComponent
			with AutowiringMarkingWorkflowServiceComponent
			with AutowiringAssessmentServiceComponent
			with ComposableCommand[MarkingWorkflow]
			with ReplaceMarkerInMarkingWorkflowValidation
			with ReplaceMarkerInMarkingWorkflowDescription
			with ReplaceMarkerInMarkingWorkflowPermissions
			with ReplaceMarkerInMarkingWorkflowCommandState
			with ReplaceMarkerInMarkingWorkflowCommandRequest
}


class ReplaceMarkerInMarkingWorkflowCommandInternal(val department: Department, val markingWorkflow: MarkingWorkflow)
	extends CommandInternal[MarkingWorkflow] {

	self: MarkingWorkflowServiceComponent with AssessmentServiceComponent with UserLookupComponent
		with ReplaceMarkerInMarkingWorkflowCommandRequest with ReplaceMarkerInMarkingWorkflowCommandState =>

	override def applyInternal(): MarkingWorkflow = {
		val oldUser = userLookup.getUserByUserId(oldMarker)
		val newUser = userLookup.getUserByUserId(newMarker)

		def replaceMarkerMap[A <: MarkerMap](markers: JList[A]): Unit = {
			val oldMarkerMap = markers.asScala.find(_.marker_id == oldMarker)
			if (markers.asScala.exists(_.marker_id == newMarker)) {
				// The new marker is an existing marker, so move the students
				oldMarkerMap.foreach(marker => {
					marker.students.knownType.members.foreach(s => markers.asScala.find(_.marker_id == newMarker).get.students.knownType.addUserId(s))
					markers.remove(marker)
				})
			} else {
				oldMarkerMap.foreach(_.marker_id = newMarker)
			}
		}

		def replaceMarkerStudentsChoose(assignment: Assignment): Unit = {
			assignment.markerSelectField.foreach { field =>
				assignment.submissions.asScala
					.filter(_.getValue(field).exists(_.value == oldUser.getUserId))
				  .foreach(submission =>
						submission.getValue(field).foreach(_.value = newUser.getUserId)
					)
			}
		}

		markingWorkflow match {
			case _: OldStudentsChooseMarkerWorkflow =>
				affectedAssignments.foreach { assignment =>
					replaceMarkerStudentsChoose(assignment)
					assessmentService.save(assignment)
				}
			case _: AssessmentMarkerMap =>
				affectedAssignments.foreach(assignment => {
					replaceMarkerMap(assignment.firstMarkers)
					replaceMarkerMap(assignment.secondMarkers)
					assessmentService.save(assignment)
				})
			case _ =>
			 throw new IllegalArgumentException("Unknown workflow type")
		}

		if (markingWorkflow.firstMarkers.includesUser(oldUser)) {
			markingWorkflow.firstMarkers.remove(oldUser)
			markingWorkflow.firstMarkers.add(newUser)
		}
		if (markingWorkflow.hasSecondMarker && markingWorkflow.secondMarkers.includesUser(oldUser)) {
			markingWorkflow.secondMarkers.remove(oldUser)
			markingWorkflow.secondMarkers.add(newUser)
		}
		if (markingWorkflow.hasThirdMarker && markingWorkflow.thirdMarkers.includesUser(oldUser)) {
			markingWorkflow.thirdMarkers.remove(oldUser)
			markingWorkflow.thirdMarkers.add(newUser)
		}
		markingWorkflowService.save(markingWorkflow)
		markingWorkflow
	}

}

trait ReplaceMarkerInMarkingWorkflowValidation extends SelfValidating {

	self: ReplaceMarkerInMarkingWorkflowCommandState with ReplaceMarkerInMarkingWorkflowCommandRequest with UserLookupComponent =>

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

trait ReplaceMarkerInMarkingWorkflowPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ReplaceMarkerInMarkingWorkflowCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(mandatory(markingWorkflow), mandatory(department))
		p.PermissionCheck(Permissions.MarkingWorkflow.Manage, markingWorkflow)
	}

}

trait ReplaceMarkerInMarkingWorkflowDescription extends Describable[MarkingWorkflow] {

	self: ReplaceMarkerInMarkingWorkflowCommandState with ReplaceMarkerInMarkingWorkflowCommandRequest =>

	override lazy val eventName = "ReplaceMarkerInMarkingWorkflow"

	override def describe(d: Description) {
		d.markingWorkflow(markingWorkflow)
		d.properties(("assignments", affectedAssignments.map(_.id)), ("oldMarker", oldMarker), ("newMarker", newMarker))
	}
}

trait ReplaceMarkerInMarkingWorkflowCommandState {

	self: MarkingWorkflowServiceComponent =>

	def department: Department
	def markingWorkflow: MarkingWorkflow

	lazy val allMarkers: Seq[User] = (markingWorkflow.firstMarkers.users ++ (
		if (markingWorkflow.hasSecondMarker) {
			markingWorkflow.secondMarkers.users
		} else {
			Seq()
		}
	) ++ (
		if (markingWorkflow.hasThirdMarker) {
			markingWorkflow.thirdMarkers.users
		} else {
			Seq()
		}
	)).distinct.sortBy(u => (u.getLastName, u.getFirstName))

	lazy val affectedAssignments: Seq[Assignment] = markingWorkflowService.getAssignmentsUsingMarkingWorkflow(markingWorkflow)
}

trait ReplaceMarkerInMarkingWorkflowCommandRequest {
	var oldMarker: String = _
	var newMarker: String = _
	var confirm: Boolean = false
}
