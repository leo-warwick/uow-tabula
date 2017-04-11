package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, Mockito, TestBase}
import uk.ac.warwick.tabula.services.permissions.{PermissionsService, PermissionsServiceComponent}
import uk.ac.warwick.tabula.roles.{DepartmentalAdministratorRoleDefinition, ModuleManagerRoleDefinition}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.DescriptionImpl
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, DepartmentGrantedRole}

class DeleteCustomRoleDefinitionCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends DeleteCustomRoleDefinitionCommandState with PermissionsServiceComponent {
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
	}

	private trait CommandFixture extends Fixture {
		val command = new DeleteCustomRoleDefinitionCommandInternal(department, customRole) with CommandTestSupport
	}

	@Test def init { new CommandFixture {
		command.department should be (department)
		command.customRoleDefinition should be (customRole)
	}}

	@Test def apply { new CommandFixture {
		command.applyInternal() should be (customRole)

		verify(command.permissionsService, times(1)).delete(customRole)
	}}

	@Test def permissions { new Fixture {
		val command = new DeleteCustomRoleDefinitionCommandPermissions with DeleteCustomRoleDefinitionCommandState {
			override val department: Department = Fixtures.department("in")
			override val customRoleDefinition: CustomRoleDefinition = customRole
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.RolesAndPermissions.Delete, customRole)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def noDepartment { new Fixture {
		val command = new DeleteCustomRoleDefinitionCommandPermissions with DeleteCustomRoleDefinitionCommandState {
			override val department = null
			override val customRoleDefinition: CustomRoleDefinition = customRole
		}

		customRole.department = department

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}}

	private trait ValidationFixture extends Fixture {
		val d: Department = department

		val command = new DeleteCustomRoleDefinitionCommandValidation with CommandTestSupport {
			val department: Department = d
			val customRoleDefinition: CustomRoleDefinition = customRole
		}
	}

	@Test def validateNoErrors { new ValidationFixture {
		command.permissionsService.getAllGrantedRolesForDefinition(customRole) returns (Nil)
		command.permissionsService.getCustomRoleDefinitionsBasedOn(customRole) returns (Nil)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateHasGrantedRole { new ValidationFixture {
		command.permissionsService.getAllGrantedRolesForDefinition(customRole) returns (Seq(new DepartmentGrantedRole(department, customRole)))
		command.permissionsService.getCustomRoleDefinitionsBasedOn(customRole) returns (Nil)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getGlobalError.getCodes should contain ("customRoleDefinition.delete.hasGrantedRoles")
	}}

	@Test def validateHasDerivedRole { new ValidationFixture {
		command.permissionsService.getAllGrantedRolesForDefinition(customRole) returns (Nil)
		command.permissionsService.getCustomRoleDefinitionsBasedOn(customRole) returns (Seq(new CustomRoleDefinition))

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getGlobalError.getCodes should contain ("customRoleDefinition.delete.hasDerivedRoles")
	}}

	@Test def description {
		val command = new DeleteCustomRoleDefinitionCommandDescription with DeleteCustomRoleDefinitionCommandState {
			override val eventName: String = "test"
			val department: Department = Fixtures.department("in")
			val customRoleDefinition = new CustomRoleDefinition
			customRoleDefinition.id = "custom-role"
			customRoleDefinition.department = department
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"department" -> "in",
			"customRoleDefinition" -> "custom-role"
		))
	}

}
