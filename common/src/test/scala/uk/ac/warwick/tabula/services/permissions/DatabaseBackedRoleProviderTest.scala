package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.{DepartmentalAdministrator, DepartmentalAdministratorRoleDefinition, ModuleManager, ModuleManagerRoleDefinition}

class DatabaseBackedRoleProviderTest extends TestBase with Mockito {

  val provider = new DatabaseBackedRoleProvider

  val service: PermissionsService = mock[PermissionsService]
  provider.service = service

  val dept: Department = Fixtures.department("in")
  val module: Module = Fixtures.module("in101")
  module.adminDepartment = dept

  @Test def getRoles = withUser("cuscav") {
    val gr1 = GrantedRole(dept, DepartmentalAdministratorRoleDefinition)
    val gr2 = GrantedRole(module, ModuleManagerRoleDefinition)

    service.getGrantedRolesFor[PermissionsTarget](currentUser) returns (LazyList(gr1, gr2).asInstanceOf[LazyList[GrantedRole[PermissionsTarget]]])

    // Can't do exact equality because a granted role with no overrides is still a generated role, not a strict built in role
    val roles = provider.getRolesFor(currentUser, dept)
    roles.size should be(2)
    roles(0).explicitPermissions should be(DepartmentalAdministrator(dept).explicitPermissions)
    roles(1).explicitPermissions should be(ModuleManager(module).explicitPermissions)
  }

  @Test def noRoles = withUser("cuscav") {
    service.getGrantedRolesFor[PermissionsTarget](currentUser) returns (LazyList.empty)
    provider.getRolesFor(currentUser) should be(LazyList.empty)
  }

}
