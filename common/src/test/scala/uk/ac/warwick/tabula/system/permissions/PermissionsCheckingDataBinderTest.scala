package uk.ac.warwick.tabula.system.permissions

import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.system.BindListener
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.system.CustomDataBinder
import uk.ac.warwick.tabula.system.BindListenerBinding

class PermissionsCheckingDataBinderTest extends TestBase with Mockito {

	val securityService: SecurityService = mock[SecurityService]

	abstract class TestCommand extends Command[Boolean] {
		def describe(d:Description) {}
		def applyInternal = true
	}

	case class BasicCommand(scope: PermissionsTarget) extends TestCommand {
		PermissionCheck(Permissions.Module.Create, scope)
	}

	case class MultipleCommand(scope: PermissionsTarget) extends TestCommand {
		PermissionCheck(Permissions.Module.Create, scope)
		PermissionCheck(Permissions.Module.Delete, scope)
		PermissionCheck(Permissions.UserPicker)
	}

	case class PublicCommand() extends TestCommand with Public

	case class AccidentallyPublicCommand() extends TestCommand

	val dept: Department = Fixtures.department("in", "IT Services")

	class Binder(obj:Any, name:String, val securityService:SecurityService)
		extends CustomDataBinder(obj, name)
		with PermissionsBinding

	@Test def basicCheck = withUser("cuscav", "0672089") {
		new Binder(BasicCommand(dept), "command", securityService)
	}

	@Test(expected=classOf[PermissionDeniedException]) def basicCheckWithFailure = withUser("cuscav", "0672089") {
		securityService.check(currentUser, Permissions.Module.Create, dept) throws (new PermissionDeniedException(currentUser, Permissions.Module.Create, dept))

		new Binder(BasicCommand(dept), "command", securityService)
	}

	@Test def multipleChecks = withUser("cuscav", "0672089") {
		new Binder(MultipleCommand(dept), "command", securityService)
	}

	@Test(expected=classOf[PermissionDeniedException]) def multipleChecksWithFailure = withUser("cuscav", "0672089") {
		securityService.check(currentUser, Permissions.Module.Delete, dept) throws (new PermissionDeniedException(currentUser, Permissions.Module.Delete, dept))

		new Binder(MultipleCommand(dept), "command", securityService)
	}

	@Test(expected=classOf[IllegalArgumentException]) def noChecksThrowsException = withUser("cuscav", "0672089") {
		new Binder(AccidentallyPublicCommand(), "command", securityService) with PermissionsBinding
	}

	@Test def noChecksButPublic = withUser("cuscav", "0672089") {
		new Binder(PublicCommand(), "command", securityService)
	}

	class BindTestCommand() extends TestCommand with Public with BindListener {
		var bound = false
		override def onBind(result: BindingResult) {
			bound = true
		}
	}

	// FIXME this is in the wrong test class
	@Test def bindListener = withUser("cuscav", "0672089") {
		val cmd = new BindTestCommand

		cmd.bound should be (false)

		val binder = new CustomDataBinder(cmd, "command") with BindListenerBinding
		binder.bind(new MockHttpServletRequest)

		cmd.bound should be (true)
	}

}