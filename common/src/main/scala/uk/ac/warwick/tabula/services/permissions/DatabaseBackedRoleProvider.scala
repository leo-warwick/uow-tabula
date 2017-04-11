package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.roles.RoleBuilder
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.commands.TaskBenchmarking

@Component
class DatabaseBackedRoleProvider extends ScopelessRoleProvider with TaskBenchmarking {

	var service: PermissionsService = Wire[PermissionsService]

	def getRolesFor(user: CurrentUser): Stream[Role] = benchmarkTask("Get roles for DatabaseBackedRoleProvider") {
		service.getGrantedRolesFor[PermissionsTarget](user).map { _.build() }
	}

	def rolesProvided = Set(classOf[RoleBuilder.GeneratedRole])

	// This isn't exhaustive because we use the cache now - it used to be though.

}