package uk.ac.warwick.tabula.dev.web.commands

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.services.{AssessmentService, AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class CreateExtensionFixtureCommand extends CommandInternal[Extension] {
	this: TransactionalComponent with UserLookupComponent =>

	var assignmentService: AssessmentService = Wire[AssessmentService]

	var userId: String = _
	var assignmentId: String = _
	var approved: Boolean = _

	protected def applyInternal(): Extension = {
		transactional() {
			val now = new DateTime()
			val user = userLookup.getUserByUserId(userId)
			val e = new Extension()
			e._universityId = user.getWarwickId
			e.usercode = user.getUserId
			e.requestedOn = now
			e.requestedExpiryDate = now.plusMonths(3)
			e.reason = "For use in fixture."
			e.assignment = assignmentService.getAssignmentById(assignmentId).get

			if (approved) {
				e.approve()
				e.reviewedOn = now.plusMillis(5) // superquick
			}

			// make sure to manually create the inverse relationship
			e.assignment.extensions.add(e)

			e
		}
	}
}

object CreateExtensionFixtureCommand {
	def apply(): CreateExtensionFixtureCommand with ComposableCommand[Extension] with AutowiringTransactionalComponent with AutowiringUserLookupComponent with PubliclyVisiblePermissions with Unaudited = {
		new CreateExtensionFixtureCommand
			with ComposableCommand[Extension]
			with AutowiringTransactionalComponent
			with AutowiringUserLookupComponent
			with PubliclyVisiblePermissions
			with Unaudited
	}
}
