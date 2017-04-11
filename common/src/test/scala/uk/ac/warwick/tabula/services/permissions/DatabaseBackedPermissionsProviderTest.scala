package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.data.model.permissions.DepartmentGrantedRole
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.roles.ModuleManagerRoleDefinition
import uk.ac.warwick.tabula.roles.DepartmentalAdministrator
import uk.ac.warwick.tabula.roles.DepartmentModuleManager
import uk.ac.warwick.tabula.roles.DepartmentalAdministrator
import uk.ac.warwick.tabula.data.model.permissions.DepartmentGrantedPermission
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import uk.ac.warwick.tabula.permissions.PermissionsTarget

class DatabaseBackedPermissionsProviderTest extends TestBase with Mockito {

	val provider = new DatabaseBackedPermissionsProvider

	val service: PermissionsService = mock[PermissionsService]
	provider.service = service

	val dept: Department = Fixtures.department("in")

	@Test def getPermissions = withUser("cuscav") {
		val gp1 = new DepartmentGrantedPermission(dept, Permissions.Department.ManageDisplaySettings, GrantedPermission.Allow)
		val gp2 = new DepartmentGrantedPermission(dept, Permissions.Module.Create, GrantedPermission.Deny)

		service.getGrantedPermissionsFor[PermissionsTarget](currentUser) returns (Stream(gp1, gp2).asInstanceOf[Stream[GrantedPermission[PermissionsTarget]]])

		val permissions = provider.getPermissionsFor(currentUser, dept)
		permissions.size should be (2)
		permissions(0).permission should be (Permissions.Department.ManageDisplaySettings)
		permissions(0).scope should be (Some(dept))
		permissions(0).permissionType should be (GrantedPermission.Allow)
		permissions(1).permission should be (Permissions.Module.Create)
		permissions(1).scope should be (Some(dept))
		permissions(1).permissionType should be (GrantedPermission.Deny)
	}

	@Test def noPermissions = withUser("cuscav") {
		service.getGrantedPermissionsFor[PermissionsTarget](currentUser) returns (Stream.empty)
		provider.getPermissionsFor(currentUser) should be (Stream.empty)
	}

}