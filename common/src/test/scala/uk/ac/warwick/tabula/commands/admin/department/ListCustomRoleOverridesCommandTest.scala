package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, RoleOverride}
import ListCustomRoleOverridesCommand._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.roles.{DepartmentalAdministratorRoleDefinition, RoleBuilder}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.roles.RoleBuilder.GeneratedRole

class ListCustomRoleOverridesCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends ListCustomRoleOverridesCommandState

	private trait Fixture {
		val department: Department = Fixtures.department("in")
		val customRole = new CustomRoleDefinition
		customRole.name = "Custom role"
		customRole.baseRoleDefinition = DepartmentalAdministratorRoleDefinition

		val command = new ListCustomRoleOverridesCommandInternal(department, customRole) with CommandTestSupport
	}

	@Test def itWorks { new Fixture {
		val override1 = new RoleOverride
		val override2 = new RoleOverride

		customRole.overrides.add(override1)
		customRole.overrides.add(override2)

		val generatedRole: GeneratedRole = RoleBuilder.build(customRole, Some(null), customRole.name)

		command.applyInternal() should be (CustomRoleOverridesInfo(generatedRole, Seq(override1, override2)))
	}}

	@Test def permissions {
		val command = new ListCustomRoleOverridesCommandPermissions with ListCustomRoleOverridesCommandState {
			override val department: Department = Fixtures.department("in")
			override val customRoleDefinition = new CustomRoleDefinition
		}

		command.customRoleDefinition.department = command.department

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.RolesAndPermissions.Read, command.customRoleDefinition)
	}

	@Test(expected = classOf[ItemNotFoundException]) def noDepartment {
		val command = new ListCustomRoleOverridesCommandPermissions with ListCustomRoleOverridesCommandState {
			override val department = null
			override val customRoleDefinition = new CustomRoleDefinition
		}

		command.customRoleDefinition.department = command.department

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

 }
