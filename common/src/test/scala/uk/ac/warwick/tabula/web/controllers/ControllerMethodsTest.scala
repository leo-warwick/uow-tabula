package uk.ac.warwick.tabula.web.controllers

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.model.Department

class ControllerMethodsTest extends TestBase with ControllerMethods with Mockito {

	var securityService: SecurityService = mock[SecurityService]
	val user: CurrentUser = {
		val u = new User("cuscav")
		u.setIsLoggedIn(true)
		u.setFoundUser(true)

		new CurrentUser(u, u)
	}

	val dept: Department = Fixtures.department("in", "IT Services")
	dept.id = "dept"

	abstract class TestCommand extends Command[Boolean] {
		def describe(d:Description) {}
		def applyInternal = true
	}

	case class BasicCommand() extends TestCommand {
		PermissionCheck(Permissions.Module.Create, dept)
	}

	@Test def restricted {
		securityService.check(user, Permissions.Module.Create, dept) throws(new PermissionDeniedException(user, Permissions.Module.Create, dept))

		restricted(BasicCommand()) should be (None)

		reset(securityService)

		restricted(BasicCommand()) should be ('defined)
	}

	@Test def restrictedBy {
		securityService.check(user, Permissions.Module.Create, dept) throws (new PermissionDeniedException(user, Permissions.Module.Create, dept))

		restrictedBy(true)(BasicCommand()) should be (None)

		// Restriction doesn't apply because predicate returns false
		restrictedBy(false)(BasicCommand()) should be ('defined)

		reset(securityService)

		restrictedBy(true)(BasicCommand()) should be ('defined)
		restrictedBy(false)(BasicCommand()) should be ('defined)
	}

}