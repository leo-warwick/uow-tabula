package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.helpers.Promises._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.helpers.MutablePromise

/**
 * A role provider that will look for any staff members who the current user
 * is marked as an assistant of, and return any permissions that the staff
 * member has and allow the assistant to perform them. Particularly useful if
 * someone is an assistant of a personal tutor, where the permissions are implicitly
 * granted rather than explicitly granted.
 */
@Component
class StaffMemberAssistantRoleProvider extends RoleProvider with TaskBenchmarking {

	val roleService: MutablePromise[RoleService] = promise { Wire[RoleService] }
	val roleProviders: MutablePromise[Seq[RoleProvider]] = promise { Wire.all[RoleProvider] }
	val profileService: MutablePromise[ProfileService] = promise { Wire[ProfileService] }

	def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role] = benchmarkTask("Get roles for StaffMemberAssistantRoleProvider") {
		profileService.get.findStaffMembersWithAssistant(user.apparentUser).toStream.flatMap { staff =>
			// Treat it as a masquerade, so if we have any role providers that explicitly ignore masquerade this is taken into account
			val staffCurrentUser =
				new CurrentUser(
					realUser = user.apparentUser,
					apparentUser = staff.asSsoUser,
					profile = Some(staff),
					sysadmin = false,
					masquerader = false,
					god = false
				)

			roleService.get.getRolesFor(staffCurrentUser, scope)
		}
	}

	def rolesProvided: Set[Class[_ <: Role]] =
		roleProviders.get.toSet[RoleProvider].filterNot { _.isInstanceOf[StaffMemberAssistantRoleProvider] }.flatMap { _.rolesProvided }

}