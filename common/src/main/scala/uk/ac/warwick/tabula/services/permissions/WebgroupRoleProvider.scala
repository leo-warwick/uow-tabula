package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.roles.{BuiltInRole, Role}
import uk.ac.warwick.tabula.services.{LenientGroupService, UserLookupService}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.userlookup.webgroups.GroupServiceException

import scala.reflect._
import uk.ac.warwick.tabula.commands.TaskBenchmarking

/**
 * Base class for the sysadmin and masquerader roles, which both grant
 * a single role if you are in a particular webgroup.
 */
@Component
abstract class WebgroupRoleProvider[A <: BuiltInRole : ClassTag](role: A) extends ScopelessRoleProvider with TaskBenchmarking {

	var userLookup: UserLookupService = Wire.auto[UserLookupService]
	var webgroup: String

	def groupService: LenientGroupService = userLookup.getGroupService

	def getRolesFor(user: CurrentUser): Stream[Role] = benchmarkTask("Get roles for WebgroupRoleProvider") {
		try {
			if (user.realId.hasText && groupService.isUserInGroup(user.realId, webgroup)) Stream(role)
			else Stream.empty
		} catch {
			case e: GroupServiceException => Stream.empty
		}
	}

	def rolesProvided = Set(classTag[A].runtimeClass.asInstanceOf[Class[Role]])

}