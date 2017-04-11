package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.MockUserLookup
import uk.ac.warwick.tabula.roles.Masquerader
import uk.ac.warwick.tabula.Fixtures

class MasqueraderRoleProviderTest extends TestBase {

	val provider = new MasqueraderRoleProvider

	val userLookup = new MockUserLookup
	provider.userLookup = userLookup
	provider.webgroup = "tabula-masques"

	userLookup.groupService.usersInGroup ++= Map(
		("cuscav", "othergroup") -> true,
		("cuscav", "tabula-masques") -> true
	)

	@Test def itWorks {
		withUser("cuscav") {
			provider.getRolesFor(currentUser) should be (Seq(Masquerader()))
		}

		withUser("cusebr") {
			provider.getRolesFor(currentUser) should be (Seq())
		}
	}

}