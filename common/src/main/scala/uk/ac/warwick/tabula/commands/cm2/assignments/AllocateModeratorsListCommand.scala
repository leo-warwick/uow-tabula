package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.data.model.{Assignment, MarkerFeedback}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.{SelectedModerationAdmin, SelectedModerationMarker, SelectedModerationModerator}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.userlookup.User


case class AllocateToModeratorInfo (
	markerFeedback: MarkerFeedback
) {
	def marker: User = markerFeedback.marker
	def moderator: Option[User] = markerFeedback.feedback.allMarkerFeedback.find(_.stage == SelectedModerationModerator).map(_.marker)
	def student: User = markerFeedback.student
	def mark: Option[Int] = markerFeedback.mark
	def grade: Option[String] = markerFeedback.grade
}

object AllocateModeratorsListCommand {
	def apply(assignment: Assignment, user: User) = new AllocateModeratorsListCommandInternal(assignment, user)
		with ComposableCommand[Seq[AllocateToModeratorInfo]]
		with AllocateModeratorsListPermissions
		with Unaudited
}

class AllocateModeratorsListCommandInternal(val assignment: Assignment, val user: User) extends CommandInternal[Seq[AllocateToModeratorInfo]]
	with AllocateModeratorsListState {

	def applyInternal(): Seq[AllocateToModeratorInfo] = {
		feedbacks
			.filter(_.outstandingStages.contains(SelectedModerationAdmin))
			.flatMap(_.allMarkerFeedback.find(_.stage == SelectedModerationMarker))
			.map(AllocateToModeratorInfo)
	}
}

trait AllocateModeratorsListPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AllocateModeratorsListState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Update, assignment)
	}
}

trait AllocateModeratorsListState extends SelectedStudentsState with SelectedStudentsRequest with UserAware {
	val assignment: Assignment
}