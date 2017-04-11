package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, Mockito, TestBase}
import uk.ac.warwick.tabula.services.permissions.{PermissionsService, PermissionsServiceComponent}
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.DescriptionImpl
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, RoleOverride}

import scala.collection.JavaConverters._

class DeleteCustomRoleOverrideCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends DeleteCustomRoleOverrideCommandState with PermissionsServiceComponent {
		val permissionsService: PermissionsService = mock[PermissionsService]
	}

	private trait Fixture {
		val department: Department = Fixtures.department("in")

		val customRole = new CustomRoleDefinition
		customRole.id = "custom"
		customRole.name = "Custom role"
		customRole.baseRoleDefinition = DepartmentalAdministratorRoleDefinition
		customRole.department = department
		department.customRoleDefinitions.add(customRole)

		val roleOverride = new RoleOverride
		roleOverride.permission = Permissions.Module.ManageAssignments
		roleOverride.overrideType = RoleOverride.Allow
		roleOverride.customRoleDefinition = customRole
		customRole.overrides.add(roleOverride)
	}

	private trait CommandFixture extends Fixture {
		val command = new DeleteCustomRoleOverrideCommandInternal(department, customRole, roleOverride) with CommandTestSupport
	}

	@Test def init { new CommandFixture {
		command.department should be (department)
		command.customRoleDefinition should be (customRole)
		command.roleOverride should be (roleOverride)
	}}

	@Test def apply { new CommandFixture {
		command.applyInternal() should be (roleOverride)
		customRole.overrides.asScala should be ('empty)

		verify(command.permissionsService, times(1)).saveOrUpdate(customRole)
	}}

	@Test def permissions { new Fixture {
		val d: Department = department
		val o: RoleOverride = roleOverride

		val command = new DeleteCustomRoleOverrideCommandPermissions with DeleteCustomRoleOverrideCommandState {
			override val department: Department = d
			override val customRoleDefinition: CustomRoleDefinition = customRole
			override val roleOverride: RoleOverride = o
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.RolesAndPermissions.Delete, roleOverride)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def noDepartment { new Fixture {
		val o: RoleOverride = roleOverride

		val command = new DeleteCustomRoleOverrideCommandPermissions with DeleteCustomRoleOverrideCommandState {
			override val department = null
			override val customRoleDefinition: CustomRoleDefinition = customRole
			override val roleOverride: RoleOverride = o
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}}

	private trait ValidationFixture extends Fixture {
		val d: Department = department
		val o: RoleOverride = roleOverride

		val command = new DeleteCustomRoleOverrideCommandValidation with CommandTestSupport {
			val department: Department = d
			val customRoleDefinition: CustomRoleDefinition = customRole
			val roleOverride: RoleOverride = o
		}
	}

	@Test def validateNoErrors { new ValidationFixture {
		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def description { new Fixture {
		val dept: Department = department
		val o: RoleOverride = roleOverride

		val command = new DeleteCustomRoleOverrideCommandDescription with DeleteCustomRoleOverrideCommandState {
			override val eventName: String = "test"
			val department: Department = dept
			val customRoleDefinition: CustomRoleDefinition = customRole
			val roleOverride: RoleOverride = o
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"department" -> "in",
			"customRoleDefinition" -> "custom"
		))
	}}

}
