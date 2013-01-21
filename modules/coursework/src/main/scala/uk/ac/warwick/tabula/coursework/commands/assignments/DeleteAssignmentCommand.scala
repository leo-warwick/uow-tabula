package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.Assignment
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils
import scala.reflect.BeanProperty
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.actions.Participate


class DeleteAssignmentCommand(val module: Module = null, val assignment: Assignment = null) extends Command[Unit] {
	
	mustBeLinked(assignment, module)
	PermissionsCheck(Participate(module))

	@BeanProperty var confirm: JBoolean = false

	def validate(errors: Errors) {
		if (!confirm) {
			errors.rejectValue("confirm", "assignment.delete.confirm")
		} else {
			commonChecks(errors)
		}
	}

	private def commonChecks(errors: Errors) {
		if (assignment.deleted) {
			errors.reject("assignment.delete.deleted")
		} else if (!assignment.submissions.isEmpty) {
			errors.reject("assignment.delete.hasSubmissions")
		} else if (assignment.anyReleasedFeedback) {
			errors.reject("assignment.delete.hasFeedback")
		}
	}

	/**
	 * Test whether we could delete this assignment.
	 */
	def prechecks(errors: Errors) {
		commonChecks(errors)
	}

	override def applyInternal() = transactional() {
		assignment.markDeleted
	}

	override def describe(d: Description) = d.assignment(assignment)

}