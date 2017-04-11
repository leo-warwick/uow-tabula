package uk.ac.warwick.tabula.commands.coursework.turnitin

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.turnitinlti.{AutowiringTurnitinLtiQueueServiceComponent, TurnitinLtiQueueServiceComponent}
import uk.ac.warwick.tabula.services.{AssessmentServiceComponent, AutowiringAssessmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, CurrentUser, FeaturesComponent}
import uk.ac.warwick.userlookup.User

/**
	* Marks an assignment as ready for plagarism checking, adding the submitting user to the notification list
	*
	* If the assignment has previously been created on Turnitin it creates the blank originality reports.
	*/
object SubmitToTurnitinCommand {
	type CommandType = Appliable[Assignment] with SubmitToTurnitinRequest with SelfValidating

	def apply(module: Module, assignment: Assignment): CommandType =
		new SubmitToTurnitinCommandInternal(module, assignment)
			with ComposableCommand[Assignment]
			with SubmitToTurnitinPermissions
			with SubmitToTurnitinDescription
			with SubmitToTurnitinValidation
			with AutowiringAssessmentServiceComponent
			with AutowiringFeaturesComponent
			with AutowiringTurnitinLtiQueueServiceComponent

	def apply(module: Module, assignment: Assignment, user: CurrentUser): CommandType =
		new SubmitToTurnitinCommandInternal(module, assignment, user)
			with ComposableCommand[Assignment]
			with SubmitToTurnitinPermissions
			with SubmitToTurnitinDescription
			with SubmitToTurnitinValidation
			with AutowiringAssessmentServiceComponent
			with AutowiringFeaturesComponent
			with AutowiringTurnitinLtiQueueServiceComponent
}

trait SubmitToTurnitinState {
	def module: Module
	def assignment: Assignment
}

trait SubmitToTurnitinRequest extends SubmitToTurnitinState {
	var submitter: User = _
}

abstract class SubmitToTurnitinCommandInternal(val module: Module, val assignment: Assignment)
	extends CommandInternal[Assignment] with SubmitToTurnitinRequest {

	self: AssessmentServiceComponent with FeaturesComponent with TurnitinLtiQueueServiceComponent =>

	def this(module: Module, assignment: Assignment, user: CurrentUser) {
		this(module, assignment)

		submitter = user.apparentUser
	}

	override def applyInternal(): Assignment = {
		if (!assignment.submitToTurnitin) {
			// Not already started the submission process
			assignment.lastSubmittedToTurnitin = new DateTime(0)
			assignment.turnitinLtiNotifyUsers = Seq()
			assignment.submitToTurnitin = true
		}
		if (assignment.turnitinId != null) {
			// Assignment won't be re-submitted, so create empty reports now
			turnitinLtiQueueService.createEmptyOriginalityReports(assignment)
		} else {
			// For all new assignments, create academic year-scoped classes
			assignment.turnitinLtiClassWithAcademicYear = true
		}
		// Add the requesting user on to the list
		if (Option(submitter).nonEmpty) {
			assignment.turnitinLtiNotifyUsers = (assignment.turnitinLtiNotifyUsers ++ Seq(submitter)).distinct
		}

		assessmentService.save(assignment)
		assignment
	}

}

trait SubmitToTurnitinPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: SubmitToTurnitinState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(mandatory(assignment), mandatory(module))
		p.PermissionCheck(Permissions.Submission.CheckForPlagiarism, assignment)
	}
}

trait SubmitToTurnitinDescription extends Describable[Assignment] {
	self: SubmitToTurnitinState =>

	override def describe(d: Description): Unit = d.assignment(assignment)
}

trait SubmitToTurnitinValidation extends SelfValidating {
	self: SubmitToTurnitinRequest with FeaturesComponent =>

	override def validate(errors: Errors) {
		if (!features.turnitinSubmissions) errors.reject("turnitin.submissions.disabled")

		if (!Option(submitter).exists(_.isFoundUser))
			errors.rejectValue("submitter", "userId.notfound")
	}
}