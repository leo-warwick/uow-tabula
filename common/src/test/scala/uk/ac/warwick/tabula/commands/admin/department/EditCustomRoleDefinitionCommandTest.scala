package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, Mockito, TestBase}
import uk.ac.warwick.tabula.services.permissions.{PermissionsService, PermissionsServiceComponent}
import uk.ac.warwick.tabula.roles.{DepartmentalAdministratorRoleDefinition, ModuleManagerRoleDefinition}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.DescriptionImpl
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition

class EditCustomRoleDefinitionCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends EditCustomRoleDefinitionCommandState with PermissionsServiceComponent {
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
		val command = new EditCustomRoleDefinitionCommandInternal(department, customRole) with CommandTestSupport
	}

	@Test def init { new CommandFixture {
		command.department should be (department)
		command.customRoleDefinition should be (customRole)
		command.name should be ("Custom role")
		command.baseDefinition should be (DepartmentalAdministratorRoleDefinition)
	}}

	@Test def apply { new CommandFixture {
		command.name = "Edited name"
		command.baseDefinition = ModuleManagerRoleDefinition

		command.applyInternal() should be (customRole)
		customRole.name should be ("Edited name")
		customRole.baseRoleDefinition should be (ModuleManagerRoleDefinition)
		customRole.department should be (department)

		verify(command.permissionsService, times(1)).saveOrUpdate(customRole)
	}}

	@Test def permissions { new Fixture {
		val command = new EditCustomRoleDefinitionCommandPermissions with EditCustomRoleDefinitionCommandState {
			override val department: Department = Fixtures.department("in")
			override val customRoleDefinition: CustomRoleDefinition = customRole
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.RolesAndPermissions.Update, customRole)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def noDepartment { new Fixture {
		val command = new EditCustomRoleDefinitionCommandPermissions with EditCustomRoleDefinitionCommandState {
			override val department = null
			override val customRoleDefinition: CustomRoleDefinition = customRole
		}

		customRole.department = department

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}}

	private trait ValidationFixture extends Fixture {
		val d: Department = department

		val command = new EditCustomRoleDefinitionCommandValidation with CommandTestSupport {
			val department: Department = d
			val customRoleDefinition: CustomRoleDefinition = customRole
		}
	}

	@Test def validateNoErrors { new ValidationFixture {
		command.name = "Custom role"
		command.baseDefinition = DepartmentalAdministratorRoleDefinition

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateNoName { new ValidationFixture {
		command.name = "         "
		command.baseDefinition = DepartmentalAdministratorRoleDefinition

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("customRoleDefinition.name.empty")
	}}

	@Test def validateNameTooLong { new ValidationFixture {
		command.name = (1 to 300).map { _ => "a" }.mkString("")
		command.baseDefinition = DepartmentalAdministratorRoleDefinition

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("customRoleDefinition.name.tooLong")
	}}

	@Test def validateNoBaseDefinition { new ValidationFixture {
		command.name = "Custom role"

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("baseDefinition")
		errors.getFieldError.getCodes should contain ("NotEmpty")
	}}

	@Test def validateBaseIsItself { new ValidationFixture {
		command.name = "Custom role"
		command.baseDefinition = customRole

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("baseDefinition")
		errors.getFieldError.getCodes should contain ("customRoleDefinition.baseIsSelf")
	}}

	@Test def description {
		val command = new EditCustomRoleDefinitionCommandDescription with EditCustomRoleDefinitionCommandState {
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
