package uk.ac.warwick.tabula.commands.coursework.markingworkflows

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.MarkingWorkflowServiceComponent
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.services.AutowiringMarkingWorkflowServiceComponent
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking

object OldEditMarkingWorkflowCommand {
	def apply(department: Department, markingWorkflow: MarkingWorkflow) =
		new EditMarkingWorkflowCommandInternal(department, markingWorkflow)
			with ComposableCommand[MarkingWorkflow]
			with EditMarkingWorkflowCommandPermissions
			with EditMarkingWorkflowCommandValidation
			with EditMarkingWorkflowCommandDescription
			with AutowiringMarkingWorkflowServiceComponent
}

/** Edit an existing markingWorkflow. */
class EditMarkingWorkflowCommandInternal(department: Department, val markingWorkflow: MarkingWorkflow)
	extends ModifyMarkingWorkflowCommand(department) with EditMarkingWorkflowCommandState {
	self: MarkingWorkflowServiceComponent =>

	// fill in the properties on construction
	copyFrom(markingWorkflow)

	def applyInternal(): MarkingWorkflow = {
		transactional() {
			this.copyTo(markingWorkflow)
			markingWorkflowService.save(markingWorkflow)
			markingWorkflow
		}
	}
}

trait EditMarkingWorkflowCommandState extends MarkingWorkflowCommandState {
	def markingWorkflow: MarkingWorkflow
	// methods for putting missing markers back into the model
	def addFirstMarkers(markers: Seq[String])
	def addSecondMarkers(markers: Seq[String])
	def newFirstMarkers: Set[String] = firstMarkers.asScala.toSet
	def newSecondMarkers: Set[String] = secondMarkers.asScala.toSet
}

trait EditMarkingWorkflowCommandValidation extends MarkingWorkflowCommandValidation with MarkerRemovalAware {
	self: EditMarkingWorkflowCommandState with MarkingWorkflowServiceComponent =>

	def currentMarkingWorkflow = Some(markingWorkflow)

	def contextSpecificValidation(errors:Errors) {
		if (markingWorkflow.markingMethod != markingMethod)
			errors.rejectValue("markingMethod", "markingWorkflow.markingMethod.cannotUpdate")

		val assignments = markingWorkflowService.getAssignmentsUsingMarkingWorkflow(markingWorkflow)
		def oneHasSubmissions = assignments.exists(_.submissions.asScala.nonEmpty)
		def oneIsReleased = assignments.exists(_.allFeedback.nonEmpty)

		// if students choose marker then existence of submissions means we shouldn't change markers
		// otherwise we can change markers until one submission is released
		if ((markingWorkflow.studentsChooseMarker && oneHasSubmissions) || oneIsReleased) {
			val errorCode =
				if (markingWorkflow.studentsChooseMarker) "markingWorkflow.studentsChoose.cannotRemoveMarkers"
				else "markingWorkflow.markers.cannotRemoveMarkers"

			if(removedFirstMarkers.nonEmpty) {
				addFirstMarkers(removedFirstMarkers.toSeq)
				errors.rejectValue("firstMarkers", errorCode)

			}
			if(removedSecondMarkers.nonEmpty) {
				addSecondMarkers(removedSecondMarkers.toSeq)
				errors.rejectValue("secondMarkers", errorCode)
			}
		}
	}
}

trait MarkerRemovalAware {

	val markingWorkflow: MarkingWorkflow
	def newFirstMarkers: Set[String]
	def newSecondMarkers: Set[String]

	val existingFirstMarkers: Set[String] = markingWorkflow.firstMarkers.knownType.includedUserIds.toSet
	val existingSecondMarkers: Set[String] = markingWorkflow.secondMarkers.knownType.includedUserIds.toSet

	lazy val removedFirstMarkers: Set[String] = existingFirstMarkers -- newFirstMarkers
	lazy val removedSecondMarkers: Set[String] = existingSecondMarkers -- newSecondMarkers
}

trait EditMarkingWorkflowCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditMarkingWorkflowCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(mandatory(markingWorkflow), mandatory(department))
		p.PermissionCheck(Permissions.MarkingWorkflow.Manage, markingWorkflow)
	}
}

trait EditMarkingWorkflowCommandDescription extends Describable[MarkingWorkflow] {
	self: EditMarkingWorkflowCommandState =>

	def describe(d: Description) {
		d.department(department).markingWorkflow(markingWorkflow)
	}
}