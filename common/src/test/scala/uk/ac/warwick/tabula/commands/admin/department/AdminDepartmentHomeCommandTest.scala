package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.{Department, Route, Module}
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.events.EventListener
import uk.ac.warwick.util.queue.Queue

class AdminDepartmentHomeCommandTest extends TestBase with Mockito with FunctionalContextTesting {

	trait CommandTestSupport extends AdminDepartmentHomeCommandState with ModuleAndDepartmentServiceComponent with CourseAndRouteServiceComponent with SecurityServiceComponent {
		val moduleAndDepartmentService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]
		val courseAndRouteService: CourseAndRouteService = mock[CourseAndRouteService]
		val securityService: SecurityService = mock[SecurityService]
	}

	trait Fixture {
		val department: Department = Fixtures.department("in", "IT Services")

		val mod1: Module = Fixtures.module("in101")
		val mod2: Module = Fixtures.module("in102")
		val mod3: Module = Fixtures.module("in103")

		Seq(mod1, mod2, mod3).foreach { mod =>
			department.modules.add(mod)
			mod.adminDepartment = department
		}

		val route1: Route = Fixtures.route("i100")
		val route2: Route = Fixtures.route("i200")
		val route3: Route = Fixtures.route("i300")

		Seq(route1, route2, route3).foreach { route =>
			department.routes.add(route)
			route.adminDepartment = department
		}

		val user: CurrentUser = mock[CurrentUser]

		val command = new AdminDepartmentHomeCommandInternal(department, user) with CommandTestSupport with AdminDepartmentHomeCommandPermissions
	}

	@Test def init() { new Fixture {
		// This is mostly to test that we don't eagerly do anything on command creation
		command.department should be (department)
		command.user should be (user)
	}}

	trait DeptAdminFixture extends Fixture {
		command.securityService.can(user, Permissions.Module.Administer, department) returns true
		command.securityService.can(user, Permissions.Route.Administer, department) returns true
	}

	trait ModuleManagerFixture extends Fixture {
		command.moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.Administer, department) returns Set(mod3, mod1)
		command.courseAndRouteService.routesWithPermission(user, Permissions.Route.Administer, department) returns Set.empty
	}

	trait RouteManagerFixture extends Fixture {
		command.courseAndRouteService.routesWithPermission(user, Permissions.Route.Administer, department) returns Set(route3, route1)
		command.moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.Administer, department) returns Set.empty
	}

	trait ModuleAndRouteManagerFixture extends Fixture {
		command.moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.Administer, department) returns Set(mod3, mod1)
		command.courseAndRouteService.routesWithPermission(user, Permissions.Route.Administer, department) returns Set(route3, route1)
	}

	trait NoPermissionsFixture extends Fixture {
		command.moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.Administer, department) returns Set.empty
		command.courseAndRouteService.routesWithPermission(user, Permissions.Route.Administer, department) returns Set.empty
	}

	@Test def applyNoPerms() { new NoPermissionsFixture {
		val (modules, routes) = command.applyInternal()
		modules should be ('empty)
		routes should be ('empty)
	}}

	@Test def applyForDeptAdmin() { new DeptAdminFixture {
		val (modules, routes) = command.applyInternal()
		modules should be (Seq(mod1, mod2, mod3))
		routes should be (Seq(route1, route2, route3))
	}}

	@Test def applyForModuleManager() { new ModuleManagerFixture {
		val (modules, routes) = command.applyInternal()
		modules should be (Seq(mod1, mod3))
		routes should be ('empty)
	}}

	@Test def applyForRouteManager() { new RouteManagerFixture {
		val (modules, routes) = command.applyInternal()
		modules should be ('empty)
		routes should be (Seq(route1, route3))
	}}

	@Test def applyForModuleAndRouteManager() { new ModuleAndRouteManagerFixture {
		val (modules, routes) = command.applyInternal()
		modules should be (Seq(mod1, mod3))
		routes should be (Seq(route1, route3))
	}}

	@Test def permissionsNoPerms() { new NoPermissionsFixture {
		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.Module.Administer, department)
	}}

	@Test def permissionsForDeptAdmin() { new DeptAdminFixture {
		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.Module.Administer, department)
	}}

	@Test def permissionsForModuleManager() { new ModuleManagerFixture {
		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheckAll(Permissions.Module.Administer, Set(mod1, mod3))
	}}

	@Test def permissionsForRouteManager() { new RouteManagerFixture {
		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheckAll(Permissions.Route.Administer, Set(route1, route3))
	}}

	@Test def permissionsForModuleAndRouteManager() { new ModuleAndRouteManagerFixture {
		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheckAll(Permissions.Module.Administer, Set(mod1, mod3))
	}}

	import AdminDepartmentHomeCommandTest.MinimalCommandContext

	@Test
	def glueEverythingTogether() { inContext[MinimalCommandContext] {
		val department = Fixtures.department("in", "IT Services")
		val user = mock[CurrentUser]

		val command = AdminDepartmentHomeCommand(department, user)

		command should be (anInstanceOf[Appliable[(Seq[Module], Seq[Route])]])
		command should be (anInstanceOf[AdminDepartmentHomeCommandState])
		command should be (anInstanceOf[AdminDepartmentHomeCommandPermissions])
	}}

}

object AdminDepartmentHomeCommandTest {
	class MinimalCommandContext extends FunctionalContext with Mockito {
		bean(){mock[SecurityService]}
		bean(){mock[RelationshipService]}
		bean(){mock[PermissionsService]}
		bean(){mock[NotificationService]}
		bean(){mock[ScheduledNotificationService]}
		bean(){mock[EventListener]}
		bean(){mock[MaintenanceModeService]}
		bean(){mock[Features]}
		bean(){mock[TriggerService]}
		bean(){
			val service = mock[ModuleAndDepartmentService]
			service.modulesWithPermission(any[CurrentUser], any[Permission], any[Department]) returns Set.empty
			service
		}
		bean(){
			val service = mock[CourseAndRouteService]
			service.routesWithPermission(any[CurrentUser], any[Permission], any[Department]) returns Set.empty
			service
		}
	}
}
