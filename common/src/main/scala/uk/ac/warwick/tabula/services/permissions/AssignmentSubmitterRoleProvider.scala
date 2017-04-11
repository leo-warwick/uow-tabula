package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.AssignmentSubmitter
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.roles.AssignmentSubmitterRoleDefinition
import uk.ac.warwick.tabula.commands.TaskBenchmarking

@Component
class AssignmentSubmitterRoleProvider extends RoleProvider with TaskBenchmarking {

	def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role] = benchmarkTask("Get roles for AssignmentSubmitterRoleProvider") {
		scope match {
			case assignment: Assignment =>
				if (assignment.canSubmit(user.apparentUser))
					Stream(customRoleFor(assignment.module.adminDepartment)(AssignmentSubmitterRoleDefinition, assignment).getOrElse(AssignmentSubmitter(assignment)))
				else Stream.empty

			// AssignmentSubmitter is only checked at the assignment level
			case _ => Stream.empty
		}
	}

	def rolesProvided = Set(classOf[AssignmentSubmitter])

}