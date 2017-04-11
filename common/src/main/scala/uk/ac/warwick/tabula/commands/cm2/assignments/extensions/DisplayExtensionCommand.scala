package uk.ac.warwick.tabula.commands.cm2.assignments.extensions

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assignment, Submission}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{ExtensionServiceComponent, _}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

case class DisplayExtensionDetail(
	extension: Option[Extension],
	student: User,
	previousExtensions: Seq[Extension],
	previousSubmissions: Seq[Submission]
){
	def numAcceptedExtensions: Int = previousExtensions.count(_.approved)
	def numRejectedExtensions: Int = previousExtensions.count(_.rejected)
}

object DisplayExtensionCommand {
	def apply(student: User, assignment: Assignment) = new DisplayExtensionCommandInternal(student, assignment)
			with ComposableCommand[DisplayExtensionDetail]
			with DisplayExtensionPermissions
			with AutowiringExtensionServiceComponent
			with AutowiringSubmissionServiceComponent
			with ReadOnly with Unaudited
}

class DisplayExtensionCommandInternal(val student: User, val assignment: Assignment) extends CommandInternal[DisplayExtensionDetail]
	with DisplayExtensionState with TaskBenchmarking {

	this:  ExtensionServiceComponent with SubmissionServiceComponent  =>

	def applyInternal(): DisplayExtensionDetail = {

		val extension: Option[Extension] = assignment.findExtension(student.getUserId)

		val previousExtensions = extensionService.getPreviousExtensions(student)

		val previousSubmissions = submissionService.getPreviousSubmissions(student)

		DisplayExtensionDetail(extension, student, previousExtensions, previousSubmissions)

		}
}

trait DisplayExtensionPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DisplayExtensionState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Read, assignment)
	}
}

trait DisplayExtensionState {
	val assignment: Assignment
}