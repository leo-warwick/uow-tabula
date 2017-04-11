package uk.ac.warwick.tabula.commands.permissions

import uk.ac.warwick.tabula.{MockUserLookup, TestBase, Mockito, Fixtures}
import uk.ac.warwick.tabula.services.permissions.{PermissionsServiceComponent, PermissionsService}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{UserLookupComponent, SecurityServiceComponent, SecurityService}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.commands.{Describable, SelfValidating, Appliable, DescriptionImpl}
import scala.reflect._

class RevokePermissionsCommandTest extends TestBase with Mockito {

	trait CommandTestSupport[A <: PermissionsTarget] extends RevokePermissionsCommandState[A] with PermissionsServiceComponent with SecurityServiceComponent with UserLookupComponent {
		val permissionsService: PermissionsService = mock[PermissionsService]
		val securityService: SecurityService = mock[SecurityService]
		val userLookup = new MockUserLookup()
	}

	trait Fixture {
		val department: Department = Fixtures.department("in", "IT Services")

		val command = new RevokePermissionsCommandInternal(department) with CommandTestSupport[Department] with RevokePermissionsCommandValidation
	}

	@Test def nonExistingPermission { new Fixture {
		command.permission = Permissions.Department.ManageExtensionSettings
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")
		command.overrideType = GrantedPermission.Allow

		command.permissionsService.getGrantedPermission(department, Permissions.Department.ManageExtensionSettings, true) returns (None)

		// Doesn't blow up, just a no-op
		command.applyInternal() should be (null)

		verify(command.permissionsService, times(0)).saveOrUpdate(any[GrantedPermission[_]])
	}}

	@Test def itWorksWithExisting { new Fixture {
		command.permission = Permissions.Department.ManageExtensionSettings
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")
		command.overrideType = GrantedPermission.Allow

		command.userLookup.registerUsers("cuscav", "cusebr")

		val existing = GrantedPermission(department, Permissions.Department.ManageExtensionSettings, true)
		existing.users.knownType.addUserId("cuscav")
		existing.users.knownType.addUserId("cusebr")
		existing.users.knownType.addUserId("cuscao")

		command.permissionsService.getGrantedPermission(department, Permissions.Department.ManageExtensionSettings, true) returns (Some(existing))

		val grantedPerm: GrantedPermission[Department] = command.applyInternal()
		(grantedPerm.eq(existing)) should be (true)

		grantedPerm.permission should be (Permissions.Department.ManageExtensionSettings)
		grantedPerm.users.size should be (1)
		grantedPerm.users.knownType.includesUserId("cuscav") should be (false)
		grantedPerm.users.knownType.includesUserId("cusebr") should be (false)
		grantedPerm.users.knownType.includesUserId("cuscao") should be (true)
		grantedPerm.overrideType should be (GrantedPermission.Allow)
		grantedPerm.scope should be (department)

		verify(command.permissionsService, times(1)).saveOrUpdate(existing)
		verify(command.permissionsService, atLeast(1)).clearCachesForUser(("cuscav", classTag[Department]))
		verify(command.permissionsService, atLeast(1)).clearCachesForUser(("cusebr", classTag[Department]))
	}}

	@Test def validatePasses { withUser("cuscav", "0672089") { new Fixture {
		command.permission = Permissions.Department.ManageExtensionSettings
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")
		command.overrideType = GrantedPermission.Allow

		val existing = GrantedPermission(department, Permissions.Department.ManageExtensionSettings, true)
		existing.users.knownType.addUserId("cuscav")
		existing.users.knownType.addUserId("cusebr")
		existing.users.knownType.addUserId("cuscao")

		command.permissionsService.getGrantedPermission(department, Permissions.Department.ManageExtensionSettings, true) returns (Some(existing))
		command.securityService.canDelegate(currentUser, Permissions.Department.ManageExtensionSettings, department) returns (true)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}}

	@Test def noUsercodes { withUser("cuscav", "0672089") { new Fixture {
		command.permission = Permissions.Department.ManageExtensionSettings
		command.overrideType = GrantedPermission.Allow

		val existing = GrantedPermission(department, Permissions.Department.ManageExtensionSettings, true)
		existing.users.knownType.addUserId("cuscav")
		existing.users.knownType.addUserId("cusebr")
		existing.users.knownType.addUserId("cuscao")

		command.permissionsService.getGrantedPermission(department, Permissions.Department.ManageExtensionSettings, true) returns (Some(existing))
		command.securityService.canDelegate(currentUser, Permissions.Department.ManageExtensionSettings, department) returns (true)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("usercodes")
		errors.getFieldError.getCode should be ("NotEmpty")
	}}}

	@Test def usercodeNotInGroup { withUser("cuscav", "0672089") { new Fixture {
		command.permission = Permissions.Department.ManageExtensionSettings
		command.usercodes.add("curef")
		command.usercodes.add("cusebr")
		command.usercodes.add("cuscao")
		command.overrideType = GrantedPermission.Allow

		val existing = GrantedPermission(department, Permissions.Department.ManageExtensionSettings, true)
		existing.users.knownType.addUserId("cuscav")
		existing.users.knownType.addUserId("cusebr")
		existing.users.knownType.addUserId("cuscao")

		command.permissionsService.getGrantedPermission(department, Permissions.Department.ManageExtensionSettings, true) returns (Some(existing))
		command.securityService.canDelegate(currentUser, Permissions.Department.ManageExtensionSettings, department) returns (true)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("usercodes")
		errors.getFieldError.getCode should be ("userId.notingroup")
	}}}

	@Test def noPermission { withUser("cuscav", "0672089") { new Fixture {
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")
		command.overrideType = GrantedPermission.Allow

		command.permissionsService.getGrantedPermission(department, null, true) returns (None)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("permission")
		errors.getFieldError.getCode should be ("NotEmpty")
	}}}

	@Test def cantRevokeWhatYouDontHave { withUser("cuscav", "0672089") { new Fixture {
		command.permission = Permissions.Department.ManageExtensionSettings
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")
		command.overrideType = GrantedPermission.Allow

		val existing = GrantedPermission(department, Permissions.Department.ManageExtensionSettings, true)
		existing.users.knownType.addUserId("cuscav")
		existing.users.knownType.addUserId("cusebr")
		existing.users.knownType.addUserId("cuscao")

		command.permissionsService.getGrantedPermission(department, Permissions.Department.ManageExtensionSettings, true) returns (Some(existing))
		command.securityService.canDelegate(currentUser, Permissions.Department.ManageExtensionSettings, department) returns (false)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("permission")
		errors.getFieldError.getCode should be ("permissions.cantRevokeWhatYouDontHave")
	}}}

	@Test
	def describe {
		val department = Fixtures.department("in")
		department.id = "department-id"

		val command = new RevokePermissionsCommandDescription[Department] with CommandTestSupport[Department] {
			val eventName: String = "test"

			val scope: Department = department
			val grantedPermission = None
		}

		command.permission = Permissions.Department.ManageExtensionSettings
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"scope" -> "Department[department-id]",
			"usercodes" -> "cuscav,cusebr",
			"permission" -> "Department.ManageExtensionSettings",
			"overrideType" -> false
		))
	}

	@Test def gluesEverythingTogether {
		val department = Fixtures.department("in")
		val command = RevokePermissionsCommand(department)

		command should be (anInstanceOf[Appliable[GrantedPermission[Department]]])
		command should be (anInstanceOf[RevokePermissionsCommandState[Department]])
		command should be (anInstanceOf[RevokePermissionsCommandPermissions])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[RevokePermissionsCommandValidation])
		command should be (anInstanceOf[Describable[GrantedPermission[Department]]])
	}

}