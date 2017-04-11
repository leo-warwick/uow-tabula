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

class AddCustomRoleOverrideCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends AddCustomRoleOverrideCommandState with PermissionsServiceComponent {
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
		val command = new AddCustomRoleOverrideCommandInternal(department, customRole) with CommandTestSupport
	}

	@Test def init { new CommandFixture {
		command.department should be (department)
		command.customRoleDefinition should be (customRole)
	}}

	@Test def apply { new CommandFixture {
		command.permission = Permissions.Module.ManageAssignments
		command.overrideType = RoleOverride.Allow

		val created: RoleOverride = command.applyInternal()
		created.permission should be (Permissions.Module.ManageAssignments)
		created.overrideType should be (RoleOverride.Allow)
		created.customRoleDefinition should be (customRole)

		verify(command.permissionsService, times(1)).saveOrUpdate(customRole)
	}}

	@Test def permissions { new Fixture {
		val d: Department = department

		val command = new AddCustomRoleOverrideCommandPermissions with AddCustomRoleOverrideCommandState {
			override val department: Department = d
			override val customRoleDefinition: CustomRoleDefinition = customRole
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.RolesAndPermissions.Create, customRole)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def noDepartment { new Fixture {
		val command = new AddCustomRoleOverrideCommandPermissions with AddCustomRoleOverrideCommandState {
			override val department = null
			override val customRoleDefinition: CustomRoleDefinition = customRole
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}}

	private trait ValidationFixture extends Fixture {
		val d: Department = department

		val command = new AddCustomRoleOverrideCommandValidation with CommandTestSupport {
			val department: Department = d
			val customRoleDefinition: CustomRoleDefinition = customRole
		}
	}

	@Test def validateNoErrors { new ValidationFixture {
		command.permission = Permissions.Module.ManageAssignments
		command.overrideType = RoleOverride.Deny

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateNoPermission { new ValidationFixture {
		command.overrideType = RoleOverride.Allow

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("permission")
		errors.getFieldError.getCodes should contain ("NotEmpty")
	}}

	@Test def validateExistingOverride { new ValidationFixture {
		command.permission = Permissions.Module.ManageAssignments
		command.overrideType = RoleOverride.Deny

		val existing = new RoleOverride
		existing.permission = Permissions.Module.ManageAssignments
		existing.overrideType = RoleOverride.Allow

		customRole.overrides.add(existing)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("permission")
		errors.getFieldError.getCodes should contain ("customRoleDefinition.override.existingOverride")
	}}

	@Test def validateAlreadyAllowed { new ValidationFixture {
		command.permission = Permissions.Module.ManageAssignments
		command.overrideType = RoleOverride.Allow

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("permission")
		errors.getFieldError.getCodes should contain ("customRoleDefinition.override.alreadyAllowed")
	}}

	@Test def validateAlreadyNotAllowed { new ValidationFixture {
		command.permission = Permissions.ImportSystemData
		command.overrideType = RoleOverride.Deny

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("permission")
		errors.getFieldError.getCodes should contain ("customRoleDefinition.override.notAllowed")
	}}

	@Test def description { new Fixture {
		val dept: Department = department

		val command = new AddCustomRoleOverrideCommandDescription with AddCustomRoleOverrideCommandState {
			override val eventName: String = "test"
			val department: Department = dept
			val customRoleDefinition: CustomRoleDefinition = customRole
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"department" -> "in",
			"customRoleDefinition" -> "custom"
		))
	}}

}
