package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.{Assignment, Department, Module, RuntimeMember, StaffMember, StudentCourseDetails, StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.roles._
import uk.ac.warwick.tabula.services.permissions.{PermissionDefinition, RoleService}
import uk.ac.warwick.userlookup.User

class SecurityServiceTest extends TestBase with Mockito {

	val user = new User("cusebr")
	user.setIsLoggedIn{true}
	user.setFoundUser{true}

	@Test def godMode() {
		val securityService = new SecurityService

		val currentUser = new CurrentUser(user, user, god=true)

		securityService.can(currentUser, Permissions.GodMode) should be {true}
		securityService.can(currentUser, Permissions.Department.ManageDisplaySettings, new Department) should be {true}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Manage(new StudentRelationshipType), null) should be {true}
		securityService.can(currentUser, null) should be {true}
		securityService.can(currentUser, null, null) should be {true}
	}

	@Test def explicitPermission() {
		val securityService = new SecurityService

		val department = new Department
		val currentUser = new CurrentUser(user, user)

		val roleService = mock[RoleService]
		roleService.getRolesFor(currentUser,null) returns Stream.empty
		roleService.getRolesFor(currentUser,department) returns Stream.empty
		roleService.getExplicitPermissionsFor(currentUser, null) returns Stream(
			PermissionDefinition(Permissions.UserPicker, None, true),
			PermissionDefinition(Permissions.ImportSystemData, None, true)
		)
		roleService.getExplicitPermissionsFor(currentUser, department) returns Stream(
			PermissionDefinition(Permissions.Department.ManageDisplaySettings, Some(department), true)
		)

		securityService.roleService = roleService

		securityService.can(currentUser, Permissions.GodMode) should be {false}
		securityService.can(currentUser, Permissions.Module.Create, department) should be {false}
		securityService.can(currentUser, Permissions.UserPicker) should be {true}
		securityService.can(currentUser, Permissions.Department.ManageDisplaySettings, department) should be {true}
	}

	@Test def role() {
		val securityService = new SecurityService

		val department = new Department
		val currentUser = new CurrentUser(user, user)

		val roleService = mock[RoleService]
		roleService.getRolesFor(currentUser, null) returns Stream(
			Sysadmin()
		)
		roleService.getRolesFor(currentUser, department) returns Stream(
			Sysadmin(),
			DepartmentalAdministrator(department)
		)

		securityService.roleService = roleService

		securityService.can(currentUser, Permissions.UserPicker) should be {false}
		securityService.can(currentUser, Permissions.GodMode) should be {true}
		securityService.can(currentUser, Permissions.Department.ManageDisplaySettings, department) should be {true}
		// Global perm
		securityService.can(currentUser, Permissions.Module.Create, department) should be {true}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Manage(new StudentRelationshipType), null) should be {false}
	}

	@Test def globalPermission() {
		val department = new Department
		val currentUser = new CurrentUser(user, user)

		val securityService = new SecurityService
		val permissions: Seq[(Permission, Option[PermissionsTarget])] = Seq(
			Permissions.Department.DownloadFeedbackReport -> None
		)

		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, department
		) should be (securityService.Allow)

		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, new Module
		) should be (securityService.Allow)

		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.ManageDisplaySettings, department
		) should be (securityService.Continue)

		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.ManageDisplaySettings, null
		) should be (securityService.Continue)
	}

	@Test def exactScopeMatch() {
		val department = new Department
		val currentUser = new CurrentUser(user, user)

		val securityService = new SecurityService
		val permissions: Seq[(Permission, Option[PermissionsTarget])] = Seq(
				Permissions.Department.DownloadFeedbackReport -> Some(department)
		)

		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, department
		) should be (securityService.Allow)

		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, new Module
		) should be (securityService.Continue)

		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.ManageDisplaySettings, department
		) should be (securityService.Continue)
	}

	@Test def scopelessMatch() {
		val department = new Department
		val currentUser = new CurrentUser(user, user)

		val securityService = new SecurityService
		val permissions: Seq[(Permission, Option[PermissionsTarget])] = Seq(
				Permissions.Department.DownloadFeedbackReport -> None
		)

		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, department
		) should be (securityService.Allow)

		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.ManageDisplaySettings, department
		) should be (securityService.Continue)
	}

	@Test def higherScopeMatch() {
		val module = new Module
		val department = new Department
		module.adminDepartment = department

		val currentUser = new CurrentUser(user, user)

		val securityService = new SecurityService
		val permissions: Seq[(Permission, Option[PermissionsTarget])] = Seq(
				Permissions.Department.DownloadFeedbackReport -> Some(department)
		)

		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, module
		) should be (securityService.Allow)

		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, new Assignment
		) should be (securityService.Continue)

		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.ManageDisplaySettings, module
		) should be (securityService.Continue)
	}

	@Test def scopelessPermission() {
		val currentUser = new CurrentUser(user, user)

		val securityService = new SecurityService
		val permissions: Seq[(Permission, Option[PermissionsTarget])] = Seq(
				Permissions.GodMode -> None
		)

		securityService.checkPermissions(
				permissions, currentUser, Permissions.GodMode, null
		) should be (securityService.Allow)

		securityService.checkPermissions(
				permissions, currentUser, Permissions.ImportSystemData, null
		) should be (securityService.Continue)
	}

	@Test def samePermissionMultipleScopes() {
		val department1 = new Department
		val department2 = new Department
		val currentUser = new CurrentUser(user, user)

		val securityService = new SecurityService
		val permissions: Seq[(Permission, Option[PermissionsTarget])] = Seq(
			Permissions.Department.DownloadFeedbackReport -> Some(department1),
			Permissions.Department.DownloadFeedbackReport -> Some(department2)
		)

		securityService.checkPermissions(
			permissions, currentUser, Permissions.Department.DownloadFeedbackReport, department1
		) should be (securityService.Allow)

		securityService.checkPermissions(
			permissions, currentUser, Permissions.Department.DownloadFeedbackReport, department2
		) should be (securityService.Allow)

		securityService.checkPermissions(
			permissions, currentUser, Permissions.Department.DownloadFeedbackReport, new Module
		) should be (securityService.Continue)
	}

	@Test def lowerScope() {
		val module = new Module
		val department = new Department
		module.adminDepartment = department

		val currentUser = new CurrentUser(user, user)

		val securityService = new SecurityService
		val permissions: Seq[(Permission, Option[PermissionsTarget])] = Seq(
			Permissions.Department.DownloadFeedbackReport -> Some(module),
			Permissions.Department.ManageDisplaySettings -> Some(department)
		)

		securityService.checkPermissions(
			permissions, currentUser, Permissions.Department.DownloadFeedbackReport, module
		) should be (securityService.Allow)

		securityService.checkPermissions(
			permissions, currentUser, Permissions.Department.ManageDisplaySettings, module
		) should be (securityService.Allow)

		securityService.checkPermissions(
			permissions, currentUser, Permissions.Department.DownloadFeedbackReport, department
		) should be (securityService.Continue)
	}

	@Test def runtimeMemberDenied() {
		val currentUser = new CurrentUser(user, user)
		val securityService = new SecurityService
		val runtimeMember = new RuntimeMember(currentUser)
		val realMember = new StaffMember

		securityService.checkRuntimeMember(
				currentUser, Permissions.Profiles.Read.RelationshipStudents(PermissionsSelector.Any[StudentRelationshipType]), runtimeMember
		) should be (securityService.Deny)

		securityService.checkRuntimeMember(
				currentUser, Permissions.Department.DownloadFeedbackReport, realMember
		) should be (securityService.Continue)

		securityService.checkRuntimeMember(
				currentUser, Permissions.Department.ManageDisplaySettings, runtimeMember
		) should be (securityService.Deny)
	}

	@Test def selectors() {
		val type1 = StudentRelationshipType("tutor", "tutor", "tutor", "tutee")
		val type2 = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisee")

		val securityService = new SecurityService
		val currentUser = new CurrentUser(user, user)

		val department = new Department
		department.code = "in"

		val student1 = new StudentMember
		student1.universityId = "1111111"
		student1.homeDepartment = department

		val student2 = new StudentMember
		student2.universityId = "2222222"
		student2.homeDepartment = department

		val smallGroupService = mock[SmallGroupService]
		smallGroupService.findSmallGroupsByStudent(student1.asSsoUser) returns Nil
		smallGroupService.findSmallGroupsByStudent(student2.asSsoUser) returns Nil

		student1.smallGroupService = smallGroupService
		student2.smallGroupService = smallGroupService

		val studentCourseDetails = new StudentCourseDetails(student1, "1111111/1")

		/*
		 * I can:
		 *
		 * - read any relationship type in the department
		 * - manage type1, but only against that student
		 * - manage type2 over the whole department
		 */

		val deptPerms = Stream(
				PermissionDefinition(Permissions.Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]), Some(department), true),
				PermissionDefinition(Permissions.Profiles.StudentRelationship.Manage(type2), Some(department), true)
		)

		val student1Perms = Stream(
				PermissionDefinition(Permissions.Profiles.StudentRelationship.Manage(type1), Some(student1), true)
		) #::: deptPerms

		val student2Perms = deptPerms

		val scdPerms = student1Perms

		val roleService = mock[RoleService]
		roleService.getRolesFor(currentUser,null) returns Stream.empty
		roleService.getRolesFor(currentUser,department) returns Stream.empty
		roleService.getRolesFor(currentUser,studentCourseDetails) returns Stream.empty
		roleService.getRolesFor(currentUser,student1) returns Stream.empty
		roleService.getRolesFor(currentUser,student2) returns Stream.empty

		roleService.getExplicitPermissionsFor(currentUser, null) returns Stream.empty
		roleService.getExplicitPermissionsFor(currentUser, studentCourseDetails) returns scdPerms
		roleService.getExplicitPermissionsFor(currentUser, student1) returns student1Perms
		roleService.getExplicitPermissionsFor(currentUser, student2) returns student2Perms
		roleService.getExplicitPermissionsFor(currentUser, department) returns deptPerms

		securityService.roleService = roleService

		// Can manage type 2 for whole dept
		// Can also manage type 1, but only over student1
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Manage(type1), department) should be {false}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Manage(type2), department) should be {true}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Manage(PermissionsSelector.Any[StudentRelationshipType]), department) should be {false}

		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Manage(type1), student1) should be {true}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Manage(type2), student1) should be {true}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Manage(PermissionsSelector.Any[StudentRelationshipType]), student1) should be {false}

		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Manage(type1), studentCourseDetails) should be {true}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Manage(type2), studentCourseDetails) should be {true}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Manage(PermissionsSelector.Any[StudentRelationshipType]), studentCourseDetails) should be {false}

		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Manage(type1), student2) should be {false}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Manage(type2), student2) should be {true}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Manage(PermissionsSelector.Any[StudentRelationshipType]), student2) should be {false}

		securityService.canForAny(currentUser, Permissions.Profiles.StudentRelationship.Manage(type1), Seq(student1, student2)) should be {true}

		// Can read any type over the dept
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(type1), department) should be {true}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(type2), department) should be {true}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]), department) should be {true}

		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(type1), student1) should be {true}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(type2), student1) should be {true}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]), student1) should be {true}

		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(type1), studentCourseDetails) should be {true}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(type2), studentCourseDetails) should be {true}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]), studentCourseDetails) should be {true}

		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(type1), student2) should be {true}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(type2), student2) should be {true}
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]), student2) should be {true}

	}

	/*
	 * Just testing a compiler warning that scared me, about it
	 * mismatching case classes that use inheritence. But it's
	 * only when the superclass is a case class, which we don't do.
	 * I'll leave this in though!
	 */
	@Test def caseMatching() {
	  val action = Permissions.GodMode

	  (action:Any) match {
	    case Permissions.GodMode =>
			case _ => fail("Should have matched god mode")
	  }
	}

	/*
	 * Test that we don't accidentally make permissions of the same
	 * name equal to each other in future. Sigh
	 */
	@Test def equality() {
		(Permissions.Module.Create == Permissions.AssignmentFeedback.Manage) should be {false}
		(Permissions.Module.Create == Permissions.Module.Create) should be {true}
	}

	object TestRoleDef extends BuiltInRoleDefinition{
		override def description="test"
		GrantsScopedPermission(
			Permissions.Department.ArrangeRoutesAndModules)
		def canDelegateThisRolesPermissions:JBoolean = false
	}
	object TestDelegatableRoleDef extends BuiltInRoleDefinition{
		override def description="test"
		GrantsScopedPermission(
			Permissions.Department.ArrangeRoutesAndModules)
		def canDelegateThisRolesPermissions:JBoolean = true
	}

	@Test
	def canDelegateReturnsFalseIfUserHasNoDelegatableRoles(){
		val securityService = new SecurityService
		val currentUser = new CurrentUser(user, user)
		val module = new Module("xxx01")
		securityService.roleService = mock[RoleService]
		securityService.roleService.getRolesFor(currentUser,module) returns Seq(new BuiltInRole(TestRoleDef,Some(module)){}).toStream
		securityService.canDelegate(currentUser,Permissions.Department.ArrangeRoutesAndModules,module) should be {false}
	}

	@Test
	def canDelegateReturnsTrueIfUserHasDelegatableRoles(){
		val securityService = new SecurityService
		val currentUser = new CurrentUser(user, user)
		val module = new Module("xxx01")
		securityService.roleService = mock[RoleService]
		securityService.roleService.getRolesFor(currentUser,module) returns Seq(new BuiltInRole(TestDelegatableRoleDef,Some(module)){}).toStream
		securityService.canDelegate(currentUser,Permissions.Department.ArrangeRoutesAndModules,module) should be {true}
	}

	@Test
	def canDelegateReturnsTrueIfUserIsGod(){
		val securityService = new SecurityService
		val currentUser = new CurrentUser(user, user, god=true)
		val module = new Module("xxx01")
		securityService.roleService = mock[RoleService]
		// use a role without delegation, to prove godliness works.
		securityService.roleService.getRolesFor(currentUser,module) returns Seq(new BuiltInRole(TestRoleDef,Some(module)){}).toStream
		securityService.canDelegate(currentUser,Permissions.Department.ArrangeRoutesAndModules,module) should be {true}
	}

}