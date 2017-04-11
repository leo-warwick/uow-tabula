package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, Mockito, TestBase}
import uk.ac.warwick.tabula.services.permissions.{PermissionsService, PermissionsServiceComponent}
import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, DepartmentGrantedRole}
import ListCustomRolesCommand._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions

class ListCustomRolesCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends ListCustomRolesCommandState with PermissionsServiceComponent {
		val permissionsService: PermissionsService = mock[PermissionsService]
	}

	private trait Fixture {
		val department: Department = Fixtures.department("in")

		val command = new ListCustomRolesCommandInternal(department) with CommandTestSupport
	}

	@Test def itWorks { new Fixture {
		val customRole1 = new CustomRoleDefinition
		val customRole2 = new CustomRoleDefinition

		command.permissionsService.getCustomRoleDefinitionsFor(department) returns (Seq(customRole1, customRole2))

		command.permissionsService.getAllGrantedRolesForDefinition(customRole1) returns (Nil)
		command.permissionsService.getCustomRoleDefinitionsBasedOn(customRole1) returns (Seq(customRole2))

		command.permissionsService.getAllGrantedRolesForDefinition(customRole2) returns (Seq(new DepartmentGrantedRole(department, customRole2)))
		command.permissionsService.getCustomRoleDefinitionsBasedOn(customRole2) returns (Nil)

		command.applyInternal() should be (
			Seq(
				CustomRoleInfo(customRole1, 0, 1),
				CustomRoleInfo(customRole2, 1, 0)
			)
		)
	}}

	@Test def permissions {
		val command = new ListCustomRolesCommandPermissions with ListCustomRolesCommandState {
			override val department: Department = Fixtures.department("in")
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.RolesAndPermissions.Create, command.department)
	}

	@Test(expected = classOf[ItemNotFoundException]) def noDepartment {
		val command = new ListCustomRolesCommandPermissions with ListCustomRolesCommandState {
			override val department = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

}
