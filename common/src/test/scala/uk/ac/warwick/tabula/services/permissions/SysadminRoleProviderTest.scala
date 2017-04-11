package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.{Mockito, MockGroupService, TestBase, MockUserLookup, Fixtures}
import uk.ac.warwick.tabula.roles.Sysadmin
import uk.ac.warwick.userlookup.{UserLookup, GroupService}
import uk.ac.warwick.tabula.services.{LenientGroupService, UserLookupService}
import uk.ac.warwick.userlookup.webgroups.GroupServiceException

class SysadminRoleProviderTest extends TestBase with Mockito {

	val provider = new SysadminRoleProvider

	val userLookup = new MockUserLookup
	provider.userLookup = userLookup
	provider.webgroup = "tabula-sysadmins"

	userLookup.groupService.usersInGroup ++= Map(
		("cuscav", "othergroup") -> true,
		("cuscav", "tabula-sysadmins") -> true
	)

	@Test def itWorks {
		withUser("cuscav") {
			provider.getRolesFor(currentUser) should be (Seq(Sysadmin()))
		}

		withUser("cusebr") {
			provider.getRolesFor(currentUser) should be (Seq())
		}
	}

	@Test def groupServiceException {
		val userLookup = mock[UserLookupService]
		val groupService = mock[GroupService]
		userLookup.getGroupService() returns (new LenientGroupService(groupService))
		groupService.isUserInGroup(any[String], any[String]) throws (new GroupServiceException("Unhandled webgroups downtime"))
		provider.userLookup = userLookup

		withUser("cuscav") {
			provider.getRolesFor(currentUser) should be (Seq())
		}
	}

}