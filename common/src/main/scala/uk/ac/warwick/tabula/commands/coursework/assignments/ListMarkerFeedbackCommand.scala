package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

case class MarkerFeedbackItem (
	student: User,
	submission: Option[Submission],
	feedbacks: Seq[MarkerFeedback],
	nextMarker: Option[User]
) {
	def currentFeedback: Option[MarkerFeedback] = feedbacks.lastOption
	def previousFeedback: Option[MarkerFeedback] = feedbacks.reverse.tail.headOption
}

case class MarkerFeedbackStage (
	roleName: String,
	nextRoleName: String,
	previousRoleName: Option[String],
	position: FeedbackPosition,
	feedbackItems: Seq[MarkerFeedbackItem]
)

object ListMarkerFeedbackCommand  {
	def apply(assignment:Assignment, module: Module, marker:User, submitter: CurrentUser) =
		new ListMarkerFeedbackCommand(assignment, module, marker, submitter)
		with ComposableCommand[Seq[MarkerFeedbackStage]]
		with ListMarkerFeedbackPermissions
		with ListMarkerFeedbackCommandState
		with AutowiringUserLookupComponent
		with Unaudited with ReadOnly
}

class ListMarkerFeedbackCommand(val assignment: Assignment, val module: Module, val marker: User, val submitter: CurrentUser)
	extends CommandInternal[Seq[MarkerFeedbackStage]] with CanProxy {

	self: UserLookupComponent =>

	def applyInternal(): Seq[MarkerFeedbackStage] = {
		val students = assignment.markingWorkflow.getMarkersStudents(assignment, marker).distinct
		val workflow = assignment.markingWorkflow

		val feedbackItems = students.map(student => {
			// all non transiant marker feedback items
			val feedbacks = assignment.getAllMarkerFeedbacks(student.getUserId, marker).filterNot(_.state == null).reverse
			val submission = assignment.findSubmission(student.getUserId)
			val position = feedbacks.lastOption.map(_.getFeedbackPosition)
			val nextMarker = workflow.getNextMarker(position, assignment, student.getUserId)
			MarkerFeedbackItem(student, submission, feedbacks, nextMarker)
		})

		feedbackItems
			.groupBy(_.feedbacks.lastOption.map(_.getFeedbackPosition))
			.flatMap { case (position, items) =>
				position.map { p =>
					val roleName = workflow.getRoleNameForPosition(p)
					val nextRoleName = workflow.getRoleNameForNextPosition(p).toLowerCase
					val previousRoleName = workflow.getRoleNameForPreviousPosition(p).map(_.toLowerCase)
					MarkerFeedbackStage(roleName, nextRoleName, previousRoleName, p, items)
				}
			}
			.toSeq
			.sortBy(_.position)
	}

}

trait ListMarkerFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ListMarkerFeedbackCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}

}

trait ListMarkerFeedbackCommandState {
	def assignment: Assignment
	def module: Module
	def marker:User
	def submitter: CurrentUser
}

trait CanProxy {

	val marker: User
	val submitter: CurrentUser

	def isProxying: Boolean = marker != submitter.apparentUser
}
