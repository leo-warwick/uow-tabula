package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.commands.{Description, _}
import uk.ac.warwick.tabula.data.model.MarkingState._
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.userlookup.User


/**
 * Download one or more submissions from an assignment, as a Zip, for you as a marker.
 */
object DownloadMarkersSubmissionsCommand {
	def apply(module: Module, assignment: Assignment, marker: User, submitter: CurrentUser) =
		new DownloadMarkersSubmissionsCommand(module, assignment, marker, submitter)
		with ComposableCommand[RenderableFile]
		with AutowiringZipServiceComponent
		with AutowiringAssessmentServiceComponent
		with AutowiringStateServiceComponent
		with DownloadMarkersSubmissionsDescription
		with DownloadMarkersSubmissionsCommandState
		with DownloadMarkersSubmissionsPermissions
		with ReadOnly
}

class DownloadMarkersSubmissionsCommand(val module: Module, val assignment: Assignment, val marker: User, val submitter: CurrentUser)
	extends CommandInternal[RenderableFile] with CanProxy {

	self: ZipServiceComponent with AssessmentServiceComponent with StateServiceComponent =>

	override def applyInternal(): RenderableFile = {
		val submissions = assignment.getMarkersSubmissions(marker)

		// TODO - Maybe we should do some validation here instead or disable the link if there are no submissions
		if (submissions.isEmpty) throw new ItemNotFoundException

		// do not download submissions where the marker has completed marking
		val filteredSubmissions = submissions.filter{ submission =>
			assignment.getAllMarkerFeedbacks(submission.usercode, marker).headOption.exists(mf => mf.state != MarkingCompleted)
		}

		zipService.getSomeSubmissionsZip(filteredSubmissions)
	}

}

trait DownloadMarkersSubmissionsDescription extends Describable[RenderableFile] {

	self: DownloadMarkersSubmissionsCommandState =>

	override lazy val eventName = "DownloadMarkersSubmissions"

	override def describe(d: Description) {
		val downloads = assignment.getMarkersSubmissions(marker)

		d.assignment(assignment)
			.submissions(downloads)
			.studentIds(downloads.flatMap(_.universityId))
			.studentUsercodes(downloads.map(_.usercode))
			.properties("submissionCount" -> downloads.size)
	}

}

trait DownloadMarkersSubmissionsPermissions extends PermissionsCheckingMethods with RequiresPermissionsChecking {

	self: DownloadMarkersSubmissionsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.Submission.Read, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}

}

trait DownloadMarkersSubmissionsCommandState {
	def module: Module
	def assignment: Assignment
	def marker: User
	def submitter: CurrentUser
}