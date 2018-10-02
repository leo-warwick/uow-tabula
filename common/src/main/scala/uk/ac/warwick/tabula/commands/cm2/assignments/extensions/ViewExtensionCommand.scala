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
	def studentIdentifier: String = {
		student.map(s => Option(s.getWarwickId).getOrElse(s.getUserId)).getOrElse("")
	}
	def numAcceptedExtensions: Int = previousExtensions.count(_.approved)
	def numRejectedExtensions: Int = previousExtensions.count(_.rejected)
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

	def applyInternal(): ExtensionDetail = {
		val user = Option(userLookup.getUserByUserId(extension.usercode))
		val previousExtensions = user.toSeq.flatMap(extensionService.getAllExtensionRequests)
		val previousSubmissions = user.toSeq.flatMap(submissionService.getAllSubmissions)
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