package uk.ac.warwick.tabula.services.permissions

import collection.JavaConversions._
import org.mockito.Matchers._
import org.mockito.Mockito._
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.DepartmentalAdministrator
import uk.ac.warwick.tabula.roles.ExtensionManager
import uk.ac.warwick.tabula.roles.ModuleManager
import uk.ac.warwick.tabula.roles.ModuleManager
import uk.ac.warwick.tabula.roles.Sysadmin
import uk.ac.warwick.tabula.roles.Sysadmin
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.roles.DepartmentalAdministrator
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.roles.UniversityMemberRole
import uk.ac.warwick.tabula.services.{StaffAssistantsHelpers, SmallGroupService, ProfileService}
import uk.ac.warwick.tabula.roles.DepartmentalAdministrator

class RoleServiceTest extends TestBase with Mockito {

	@Test def loggedOutRoleCheck() = withUser(null){
		val provider = new UserTypeAndDepartmentRoleProvider
		provider.getRolesFor(currentUser).size should be (0)
	}

	@Test def getRolesScopeless() = withUser("cuscav", "0672089") {
		val scopedProvider = smartMock[RoleProvider]
		val provider1 = smartMock[ScopelessRoleProvider]
		val provider2 = smartMock[ScopelessRoleProvider]

		when(scopedProvider.getRolesFor(isEq(currentUser), isA[PermissionsTarget])) thenThrow(classOf[RuntimeException])
		when(provider1.getRolesFor(currentUser, null)) thenReturn(Stream(Sysadmin()))
		when(provider2.getRolesFor(currentUser, null)) thenThrow(classOf[RuntimeException])

		val service = new RoleServiceImpl()
		service.roleProviders = Array(scopedProvider, provider1, provider2)

		val isSysadminRole = service.getRolesFor(currentUser, null) exists { _ == Sysadmin() }

		verify(provider1, times(1)).getRolesFor(currentUser, null)
		verify(provider2, times(0)).getRolesFor(currentUser, null)

		isSysadminRole should be (true)
	}

	@Test def getRolesScoped() = withUser("cuscav", "0672089") {
		val provider1 = smartMock[ScopelessRoleProvider]
		val provider2 = smartMock[RoleProvider]
		val provider3 = smartMock[RoleProvider]
		val provider4 = smartMock[ScopelessRoleProvider]

		val dept = Fixtures.department("in")
		val module = Fixtures.module("in101")
		module.adminDepartment = dept

		val service = new RoleServiceImpl()
		service.roleProviders = Array(provider1, provider2, provider3, provider4)

		when(provider1.getRolesFor(currentUser, null)) thenReturn(Stream(Sysadmin()))
		when(provider2.getRolesFor(currentUser, module)) thenReturn(Stream(ModuleManager(module)))
		when(provider3.getRolesFor(currentUser, dept)) thenReturn(Stream(DepartmentalAdministrator(dept)))
		when(provider3.getRolesFor(currentUser, module)) thenReturn(Stream.empty)
		when(provider4.getRolesFor(currentUser, null)) thenReturn(Stream.empty)

		(service.getRolesFor(currentUser, module) exists { _ == DepartmentalAdministrator(dept) }) should be (true)

		verify(provider1, times(1)).getRolesFor(currentUser, null)
		verify(provider4, times(1)).getRolesFor(currentUser, null)

		verify(provider2, times(0)).getRolesFor(currentUser, dept) // We don't bubble up on this one because it's not exhaustive
		verify(provider2, times(1)).getRolesFor(currentUser, module)
		verify(provider3, times(1)).getRolesFor(currentUser, dept)
		verify(provider3, times(1)).getRolesFor(currentUser, module)
	}

	@Test def exhaustiveGetRoles() = withUser("cuscav", "0672089") {
		val provider = mock[RoleProvider]

		val dept = Fixtures.department("in")
		val module = Fixtures.module("in101")
		module.adminDepartment = dept

		val service = new RoleServiceImpl()
		service.roleProviders = Array(provider)

		when(provider.getRolesFor(currentUser, module)) thenReturn(Stream(ModuleManager(module)))
		when(provider.getRolesFor(currentUser, dept)) thenReturn(Stream(DepartmentalAdministrator(dept)))

		(service.getRolesFor(currentUser, module) exists { _ == DepartmentalAdministrator(dept) }) should be (false)

		provider.isExhaustive returns (true)

		(service.getRolesFor(currentUser, module) exists { _ == DepartmentalAdministrator(dept) }) should be (true)
	}

	/** Test that permissions checking DOES go up from a department to its parent if it has one.
	 */
	@Test def parentDepartments = withUser("cuscav", "0672089") {

		val in = Fixtures.department("in")
		val insub1 = Fixtures.department("in-sub1")
		val insub2 = Fixtures.department("in-sub2")
		in.children.addAll(Seq(insub1, insub2))
		insub1.parent = in
		insub2.parent = in

		val provider = mock[RoleProvider]
		when(provider.getRolesFor(currentUser, insub1)) thenReturn(Stream.empty)
		when(provider.getRolesFor(currentUser, in)) thenReturn(Stream(DepartmentalAdministrator(in)))

		val service = new RoleServiceImpl()
		service.roleProviders = Array(provider)

		service.getRolesFor(currentUser, insub1).toList should equal (List(DepartmentalAdministrator(in)))

	}

	@Test def hasRole() = withUser("cuscav", "0672089") {
		val provider1 = mock[ScopelessRoleProvider]
		provider1.rolesProvided returns (Set(classOf[Sysadmin]))

		val provider2 = mock[RoleProvider]
		provider2.rolesProvided returns (Set(classOf[ModuleManager]))

		val provider3 = mock[RoleProvider]
		provider3.rolesProvided returns (Set(classOf[DepartmentalAdministrator]))

		val provider4 = mock[ScopelessRoleProvider]
		provider4.rolesProvided returns (Set())

		val dept = Fixtures.department("in")
		val module = Fixtures.module("in101")

		val service = new RoleServiceImpl()
		service.roleProviders = Array(provider1, provider2, provider3, provider4)

		when(provider2.getRolesFor(currentUser, module)) thenReturn(Stream(ModuleManager(module)))

		service.hasRole(currentUser, ModuleManager(module)) should be (true)
		service.hasRole(currentUser, ExtensionManager(dept)) should be (false)
	}

	@Test def getPermissions() = withUser("cuscav", "0672089") {
		val provider1 = mock[PermissionsProvider]
		val provider2 = mock[PermissionsProvider]

		val dept = Fixtures.department("in")
		val module = Fixtures.module("in101")
		module.adminDepartment = dept

		val service = new RoleServiceImpl()
		service.permissionsProviders = Array(provider1, provider2)

		when(provider1.getPermissionsFor(currentUser, module)) thenReturn(Stream(PermissionDefinition(Permissions.Module.ManageAssignments, Some(module), GrantedPermission.Allow)))
		when(provider2.getPermissionsFor(currentUser, dept)) thenReturn(Stream(PermissionDefinition(Permissions.Module.Create, Some(dept), GrantedPermission.Allow)))
		when(provider2.getPermissionsFor(currentUser, module)) thenReturn(Stream.empty)

		(service.getExplicitPermissionsFor(currentUser, module) exists {
			_ == PermissionDefinition(Permissions.Module.Create, Some(dept), GrantedPermission.Allow)
		}) should be (true)

		verify(provider1, times(0)).getPermissionsFor(currentUser, dept) // We don't bubble up on this one because it's not exhaustive
		verify(provider1, times(1)).getPermissionsFor(currentUser, module)
		verify(provider2, times(1)).getPermissionsFor(currentUser, dept)
		verify(provider2, times(1)).getPermissionsFor(currentUser, module)
	}

	@Test def dontCheckAllParents() = withUser("cuscav", "0672089") {
		// TAB-755 We should never have to go to the list of registered modules here, because we're looking for something in ITS
		val dept = Fixtures.department("in")
		val member = Fixtures.student(universityId = "0672089", department = dept)

		val profileService = mock[ProfileService with StaffAssistantsHelpers]
		member.profileService = profileService

		val smallGroupService = mock[SmallGroupService]
		smallGroupService.findSmallGroupsByStudent(member.asSsoUser) returns (Nil)
		member.smallGroupService = smallGroupService

		val provider1 = mock[RoleProvider]
		val provider2 = mock[RoleProvider]

		val service = new RoleServiceImpl()
		service.roleProviders = Array(provider1, provider2)

		when(provider1.getRolesFor(currentUser, member)) thenReturn(Stream.empty)
		when(provider2.getRolesFor(currentUser, member)) thenReturn(Stream.empty)
		when(provider1.getRolesFor(currentUser, dept)) thenReturn(Stream(DepartmentalAdministrator(dept)))
		when(provider2.getRolesFor(currentUser, dept)) thenReturn(Stream.empty)

		(service.getRolesFor(currentUser, member) exists { _ == DepartmentalAdministrator(dept) }) should be (true)
	}

}