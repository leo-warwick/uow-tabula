package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula._
import org.junit.Before
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget, Permissions}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.services.permissions._
import scala.Some
import uk.ac.warwick.util.queue.Queue
import uk.ac.warwick.util.queue.QueueListener
import org.springframework.beans.factory.InitializingBean
import uk.ac.warwick.tabula.helpers.Logging

class ModuleAndDepartmentServiceTest extends PersistenceTestBase with Mockito {
	
	val service: ModuleAndDepartmentService = new ModuleAndDepartmentService

	val userLookup = new MockUserLookup
	
	@Before def wire {
		val departmentDao = new DepartmentDaoImpl
		departmentDao.sessionFactory = sessionFactory
		service.departmentDao = departmentDao

		service.userLookup = userLookup

		val moduleDao = new ModuleDaoImpl
		moduleDao.sessionFactory = sessionFactory
		service.moduleDao = moduleDao

		val routeDao = new RouteDaoImpl
		routeDao.sessionFactory = sessionFactory
		service.routeDao = routeDao

		val permsDao = new PermissionsDaoImpl
		permsDao.sessionFactory = sessionFactory

		val permissionsService = new AbstractPermissionsService with PermissionsDaoComponent with PermissionsServiceCaches with GroupServiceComponent with GrantedRolesForUserCache with GrantedRolesForGroupCache with GrantedPermissionsForUserCache with GrantedPermissionsForGroupCache with StaffAssistantsHelpers with QueueListener with InitializingBean with Logging {
			var permissionsDao:PermissionsDao = permsDao
			val rolesByIdCache:GrantedRoleByIdCache = new GrantedRoleByIdCache(permsDao)
			val permissionsByIdCache = new GrantedPermissionsByIdCache(permsDao)
			val groupService = userLookup.getGroupService()
			val staffAssistantsHelper = null
		}
		permissionsService.queue = mock[Queue]
		service.permissionsService = permissionsService

		val securityService = mock[SecurityService]
		securityService.can(isA[CurrentUser],isA[Permission],isA[PermissionsTarget] ) returns true
		service.securityService = securityService


	}
	
	@Test def crud = transactional { tx =>
		// uses data created in data.sql
		
		val ch = service.getDepartmentByCode("ch").get
		val cs = service.getDepartmentByCode("cs").get
		val cssub1 = service.getDepartmentByCode("cs-subsidiary").get
		val cssub2 = service.getDepartmentByCode("cs-subsidiary-2").get
		
		val cs108 = service.getModuleByCode("cs108").get
		val cs240 = service.getModuleByCode("cs240").get
		val cs241 = service.getModuleByCode("cs241").get
		val cs242 = service.getModuleByCode("cs242").get
		
		val g500 = service.getRouteByCode("g500").get
		val g503 = service.getRouteByCode("g503").get
		val g900 = service.getRouteByCode("g900").get
		val g901 = service.getRouteByCode("g901").get
		
		service.allDepartments should be (Seq(ch, cs, cssub1, cssub2))
		service.allModules should be (Seq(cs108, cs240, cs241, cs242))
		service.allRoutes should be (Seq(g500, g503, g900, g901))
		
		// behaviour of child/parent departments
		cs.children.toArray should be (Array(cssub1, cssub2))
		cssub1.parent should be (cs)
		cssub2.parent should be (cs)
		ch.children.isEmpty should be (true)
		cs241.department should be (cssub1)
		
		service.getDepartmentByCode("ch") should be (Some(ch))
		service.getDepartmentById(ch.id) should be (Some(ch))
		service.getDepartmentByCode("wibble") should be (None)
		service.getDepartmentById("wibble") should be (None)
		
		service.getModuleByCode("cs108") should be (Some(cs108))
		service.getModuleById(cs108.id) should be (Some(cs108))
		service.getModuleByCode("wibble") should be (None)
		service.getModuleById("wibble") should be (None)
		
		service.getRouteByCode("g500") should be (Some(g500))
		service.getRouteById(g500.id) should be (Some(g500))
		service.getRouteByCode("wibble") should be (None)
		service.getRouteById("wibble") should be (None)
		
		withUser("cusebr") { service.departmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set(cs)) }
		withUser("cuscav") { 
			service.departmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set())
			service.modulesInDepartmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set())
			service.modulesInDepartmentWithPermission(currentUser, Permissions.Module.ManageAssignments, cs) should be (Set())
			service.modulesInDepartmentWithPermission(currentUser, Permissions.Module.ManageAssignments, ch) should be (Set())
			service.routesInDepartmentsWithPermission(currentUser, Permissions.MonitoringPoints.Manage) should be (Set())
			service.routesInDepartmentWithPermission(currentUser, Permissions.MonitoringPoints.Manage, cs) should be (Set())
			service.routesInDepartmentWithPermission(currentUser, Permissions.MonitoringPoints.Manage, ch) should be (Set())
			
			service.addOwner(cs, "cuscav")
			service.departmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set(cs))
			service.modulesInDepartmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set(cs108, cs240))
			service.modulesInDepartmentWithPermission(currentUser, Permissions.Module.ManageAssignments, cs) should be (Set(cs108, cs240))
			service.modulesInDepartmentWithPermission(currentUser, Permissions.Module.ManageAssignments, ch) should be (Set())
			service.routesInDepartmentsWithPermission(currentUser, Permissions.MonitoringPoints.Manage) should be (Set(g500, g503))
			service.routesInDepartmentWithPermission(currentUser, Permissions.MonitoringPoints.Manage, cs) should be (Set(g500, g503))
			service.routesInDepartmentWithPermission(currentUser, Permissions.MonitoringPoints.Manage, ch) should be (Set())
			
			service.removeOwner(cs, "cuscav")
			service.departmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set())
			
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set())
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments, cs) should be (Set())
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments, ch) should be (Set())
			
			service.routesWithPermission(currentUser, Permissions.MonitoringPoints.Manage) should be (Set())
			service.routesWithPermission(currentUser, Permissions.MonitoringPoints.Manage, cs) should be (Set())
			service.routesWithPermission(currentUser, Permissions.MonitoringPoints.Manage, ch) should be (Set())
			
			service.addModuleManager(cs108, "cuscav")
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set(cs108))
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments, cs) should be (Set(cs108))
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments, ch) should be (Set())
			
			service.routesWithPermission(currentUser, Permissions.MonitoringPoints.Manage) should be (Set())
			service.routesWithPermission(currentUser, Permissions.MonitoringPoints.Manage, cs) should be (Set())
			service.routesWithPermission(currentUser, Permissions.MonitoringPoints.Manage, ch) should be (Set())
			
			service.addRouteManager(g503, "cuscav")
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set(cs108))
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments, cs) should be (Set(cs108))
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments, ch) should be (Set())
			
			service.routesWithPermission(currentUser, Permissions.MonitoringPoints.Manage) should be (Set(g503))
			service.routesWithPermission(currentUser, Permissions.MonitoringPoints.Manage, cs) should be (Set(g503))
			service.routesWithPermission(currentUser, Permissions.MonitoringPoints.Manage, ch) should be (Set())
		}
	}

}
