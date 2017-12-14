package uk.ac.warwick.tabula.commands.cm2.assignments

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.markers.{PopulateMarkerFeedbackComponent, PopulateMarkerFeedbackComponentImpl}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.{SelectedModerationAdmin, SelectedModerationMarker}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringCM2MarkingWorkflowServiceComponent, CM2MarkingWorkflowServiceComponent}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object AllocateModeratorsCommand {

	def apply(assignment: Assignment, user: User) = new AllocateModeratorsCommandInternal(assignment, user)
		with ComposableCommand[Seq[Feedback]]
		with AllocateModeratorsActionPermissions
		with AllocateModeratorsActionDescription
		with AllocateModeratorsActionValidation
		with PopulateMarkerFeedbackComponentImpl
		with AutowiringCM2MarkingWorkflowServiceComponent {
			override lazy val eventName: String = "AllocateModerators"
		}
}

class AllocateModeratorsCommandInternal(val assignment: Assignment, val user: User) extends CommandInternal[Seq[Feedback]]
	with AllocateModeratorsActionState {

	self: CM2MarkingWorkflowServiceComponent with PopulateMarkerFeedbackComponent =>

	def applyInternal(): Seq[Feedback] = {
		val feedbackToSendToModerator = feedbacks.filter(_.outstandingStages.contains(SelectedModerationAdmin))
		val releasedMarkerFeedback = cm2MarkingWorkflowService.progress(SelectedModerationAdmin, feedbackToSendToModerator)
		populateMarkerFeedback(assignment, releasedMarkerFeedback)
		releasedMarkerFeedback.map(_.feedback)
	}
}

object AdminFinaliseCommand {

	def apply(assignment: Assignment, user: User) = new AdminFinaliseCommandInternal(assignment, user)
		with ComposableCommand[Seq[Feedback]]
		with AllocateModeratorsActionPermissions
		with AllocateModeratorsActionDescription
		with AllocateModeratorsActionValidation
		with AutowiringCM2MarkingWorkflowServiceComponent {
		override lazy val eventName: String = "AdminFinalise"
	}
}

class AdminFinaliseCommandInternal(val assignment: Assignment, val user: User) extends CommandInternal[Seq[Feedback]]
	with AllocateModeratorsActionState {

	self: CM2MarkingWorkflowServiceComponent =>

	def applyInternal(): Seq[Feedback] = {
		val feedbackToFinalise = feedbacks.filter(_.outstandingStages.contains(SelectedModerationAdmin))
		cm2MarkingWorkflowService.finish(SelectedModerationMarker, feedbackToFinalise)
	}
}

trait AllocateModeratorsActionPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AllocateModeratorsActionState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Update, assignment)
	}
}

trait AllocateModeratorsActionValidation extends SelfValidating {
	self: AllocateModeratorsActionState =>

	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "return.marking.confirm")
	}
}

trait AllocateModeratorsActionDescription extends Describable[Seq[Feedback]] {
	self: AllocateModeratorsActionState =>

	def describe(d: Description) {
		d.assignment(assignment)
		 .studentUsercodes(students.asScala)
	}

	override def describeResult(d: Description, result: Seq[Feedback]) {
		d.assignment(assignment)
		 .studentUsercodes(result.map(_.usercode))
	}
}

trait AllocateModeratorsActionState extends SelectedStudentsState with SelectedStudentsRequest with UserAware {
	val assignment: Assignment
	var confirm: Boolean = _
}