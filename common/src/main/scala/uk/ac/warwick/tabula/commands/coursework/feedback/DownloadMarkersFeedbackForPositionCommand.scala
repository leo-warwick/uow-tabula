package uk.ac.warwick.tabula.commands.coursework.feedback

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.coursework.assignments.CanProxy
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.{AutowiringZipServiceComponent, ZipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConversions._

object DownloadMarkersFeedbackForPositionCommand {

	def apply(module: Module, assignment: Assignment, marker: User, submitter: CurrentUser, position: FeedbackPosition) =
		new DownloadMarkersFeedbackForPositionCommand(module, assignment, marker, submitter, position)
		with ComposableCommand[RenderableFile]
		with DownloadMarkersFeedbackForPositionDescription
		with DownloadMarkersFeedbackForPositionPermissions
		with DownloadMarkersFeedbackForPositionCommandState
		with ReadOnly
		with AutowiringZipServiceComponent
}

class DownloadMarkersFeedbackForPositionCommand(
	val module: Module,
	val assignment: Assignment,
	val marker:User,
	val submitter: CurrentUser,
	val position: FeedbackPosition
) extends CommandInternal[RenderableFile] with CanProxy {

	self: ZipServiceComponent =>

	override def applyInternal(): RenderableFile = {
		val markersSubs = assignment.getMarkersSubmissions(marker)
		val feedbacks = assignment.feedbacks.filter(f => markersSubs.exists(_.usercode == f.usercode))
		val releasedMarkerFeedbacks = feedbacks.flatMap(f => position match {
			case FirstFeedback => Option(f.firstMarkerFeedback)
			case SecondFeedback => Option(f.secondMarkerFeedback)
			case ThirdFeedback => Option(f.thirdMarkerFeedback)
		}).filter(_.state == MarkingState.MarkingCompleted)
		zipService.getSomeMarkerFeedbacksZip(releasedMarkerFeedbacks)
	}
}

trait DownloadMarkersFeedbackForPositionDescription extends Describable[RenderableFile] {
	override def describe(d: Description) {}
}

trait DownloadMarkersFeedbackForPositionPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: DownloadMarkersFeedbackForPositionCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}
}

trait DownloadMarkersFeedbackForPositionCommandState {

	def module: Module
	def assignment: Assignment
	def marker: User
	def submitter: CurrentUser
	def position: FeedbackPosition

}