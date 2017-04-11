package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent}
import uk.ac.warwick.tabula.commands.TaskBenchmarking

@Component
class SmallGroupTutorRoleProvider extends RoleProvider with TaskBenchmarking {

	override def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role] = benchmarkTask("Get roles for SmallGroupTutorRoleProvider") {
		scope match {
			case event: SmallGroupEvent => getRoles(user, Seq(event))
			case group: SmallGroup => getRoles(user, group.events)
			case _ => Stream.empty
		}
	}

	private def getRoles(user: CurrentUser, events: Seq[SmallGroupEvent]) = {
		val validEvents =
			events.toStream
				.filter { _.tutors.includesUser(user.apparentUser) }
				.filter { _.group.groupSet.releasedToTutors }

		val eventRoles = validEvents.map { event =>
			customRoleFor(event.group.groupSet.module.adminDepartment)(SmallGroupEventTutorRoleDefinition, event).getOrElse(SmallGroupEventTutor(event))
		}

		val groupRoles = validEvents.map { _.group }.distinct.map { group =>
			customRoleFor(group.groupSet.module.adminDepartment)(SmallGroupTutorRoleDefinition, group).getOrElse(SmallGroupTutor(group))
		}

		eventRoles #::: groupRoles
	}

	def rolesProvided = Set(classOf[SmallGroupTutor], classOf[SmallGroupEventTutor])

}