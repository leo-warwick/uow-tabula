package uk.ac.warwick.tabula.commands.cm2.assignments.extensions

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{ExtensionServiceComponent, _}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

case class ExtensionDetail(
	extension: Extension,
	student: Option[User],
	previousExtensions: Seq[Extension],
	previousSubmissions: Seq[Submission]
) {
	def numAcceptedExtensions = previousExtensions.count(_.approved)
	def numRejectedExtensions = previousExtensions.count(_.rejected)
}

object ViewExtensionCommand {
	def apply(extension: Extension) = new ViewExtensionCommandInternal(extension)
			with ComposableCommand[ExtensionDetail]
			with ViewExtensionPermissions
			with AutowiringUserLookupComponent
			with AutowiringExtensionServiceComponent
			with AutowiringSubmissionServiceComponent
			with ReadOnly with Unaudited
}

class ViewExtensionCommandInternal(val extension: Extension) extends CommandInternal[ExtensionDetail]
	with ViewExtensionState with TaskBenchmarking {

	this: UserLookupComponent with ExtensionServiceComponent with SubmissionServiceComponent  =>

	def applyInternal() = {
		val user = Option(userLookup.getUserByUserId(extension.userId))
		val previousExtensions = user.toSeq.flatMap(extensionService.getPreviousExtensions)
		val previousSubmissions = user.toSeq.flatMap(submissionService.getPreviousSubmissions)
		ExtensionDetail(extension, user, previousExtensions, previousSubmissions)
	}
}


trait ViewExtensionPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewExtensionState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Extension.Read, extension)
	}
}

trait ViewExtensionState {
	val extension: Extension
}