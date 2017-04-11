package uk.ac.warwick.tabula.commands.coursework.assignments


import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.notifications.coursework.{ExtensionRequestRejectedNotification, ExtensionRequestRespondedRejectNotification}
import uk.ac.warwick.tabula.data.model.{Notification, Assignment, Module}
import uk.ac.warwick.tabula.commands.Description
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.data.model.forms.Extension

class EditAssignmentCommand(module: Module = null, val assignment: Assignment = null, user: CurrentUser)
	extends ModifyAssignmentCommand(module) with NotificationHandling {

	private var unapprovedExtensions: Seq[Extension] = Seq()

	mustBeLinked(notDeleted(mandatory(assignment)), mandatory(module))
	PermissionCheck(Permissions.Assignment.Update, assignment)

	this.copyFrom(assignment)

	// submissions exist then the markingWorkflow cannot be updated
	def canUpdateMarkingWorkflow: Boolean = assignment.submissions.size() == 0

	override def validate(errors: Errors) {
		super.validate(errors)

		if (academicYear != assignment.academicYear) {
			errors.rejectValue("academicYear", "academicYear.immutable")
		}
	}

	override def contextSpecificValidation(errors:Errors){

		// compare ids directly as this.markingWorkflow always comes back with the type MarkingWorkflow which breaks .equals

		val workflowChanged = Option(assignment.markingWorkflow).map(_.id) != Option(markingWorkflow).map(_.id)
		if (!canUpdateMarkingWorkflow && workflowChanged){
			errors.rejectValue("markingWorkflow", "markingWorkflow.cannotChange")
		}
	}

	override def applyInternal(): Assignment = transactional() {
		copyTo(assignment)

		if (!allowExtensions && assignment.countUnapprovedExtensions > 0) {
			// reject unapproved extensions (in normal circumstances, this should be unlikely)
			unapprovedExtensions = assignment.getUnapprovedExtensions
			val admin = user.apparentUser


			unapprovedExtensions.foreach { extension =>
				extension.reject()

				// let's notify manually for completeness
				val studentNotification =
					Notification.init(new ExtensionRequestRejectedNotification, admin, Seq(extension), assignment)
				val adminNotification =
					Notification.init(new ExtensionRequestRespondedRejectNotification, admin, Seq(extension), assignment)

				notify[Option[Extension]](Seq(studentNotification, adminNotification))
			}
		}

		assignment
	}

	override def describe(d: Description) {
		val desc = d.assignment(assignment)
		desc.properties(
			"name" -> name,
			"openDate" -> openDate,
			"closeDate" -> closeDate,
			"removeWorkflow" -> removeWorkflow
		)
		if (unapprovedExtensions.nonEmpty) {
			desc.property(
				"studentExtensionRequestsAutoRejected" -> unapprovedExtensions.map(_.usercode)
			)
		}
	}
}
