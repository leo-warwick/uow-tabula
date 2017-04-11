package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{ScheduledNotification, Assignment, Module}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.{AutowiringAssessmentServiceComponent, AssessmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

/** Simply marks an assignment as archived. */
object ArchiveAssignmentCommand {
	def apply(module: Module, assignment: Assignment) =
		new ArchiveAssignmentCommandInternal(module, assignment)
		with ComposableCommand[Assignment]
		with ArchiveAssignmentPermissions
		with ArchiveAssignmentDescription
		with ArchiveAssignmentCommandState
		with ArchiveAssignmentNotifications
		with AutowiringAssessmentServiceComponent
}

class ArchiveAssignmentCommandInternal(val module: Module, val assignment: Assignment)
	extends CommandInternal[Assignment] {

	self: ArchiveAssignmentCommandState with AssessmentServiceComponent =>

	def applyInternal(): Assignment = {
		transactional() {
			if (unarchive) assignment.unarchive()
			else assignment.archive()

			assessmentService.save(assignment)
		}
		assignment
	}

}

trait ArchiveAssignmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ArchiveAssignmentCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.Assignment.Archive, assignment)
	}

}

trait ArchiveAssignmentDescription extends Describable[Assignment] {

	self: ArchiveAssignmentCommandState =>

	override lazy val eventName = "ArchiveAssignment"

	override def describe(d: Description) {
		d.assignment(assignment)
		d.property("unarchive" -> unarchive)
	}

}

trait ArchiveAssignmentCommandState {
	def module: Module
	def assignment: Assignment

	var unarchive: Boolean = false
}

trait ArchiveAssignmentNotifications extends SchedulesNotifications[Assignment, Assignment] {

	override def transformResult(assignment: Assignment): Seq[Assignment] = Seq(assignment)

	// Clear all notifications
	override def scheduledNotifications(assignment: Assignment): Seq[ScheduledNotification[_]] = Seq()

}