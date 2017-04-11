package uk.ac.warwick.tabula.services

import org.junit.Before
import org.springframework.beans.factory.InitializingBean
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.{Permission, Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.services.permissions._
import uk.ac.warwick.util.cache.Caches.CacheStrategy
import uk.ac.warwick.util.queue.{Queue, QueueListener}

class CourseAndRouteServiceTest extends PersistenceTestBase with Mockito {

	val userLookupService = new MockUserLookup

	val service: CourseAndRouteService = new AbstractCourseAndRouteService with RouteDaoComponent with CourseDaoComponent with SecurityServiceComponent with PermissionsServiceComponent with ModuleAndDepartmentServiceComponent {
		val routeDao = new RouteDaoImpl
		val courseDao = new CourseDaoImpl
		val permissionsService = new AbstractPermissionsService with PermissionsDaoComponent with PermissionsServiceCaches with GrantedRolesForUserCache with GrantedRolesForGroupCache with GrantedPermissionsForUserCache with GrantedPermissionsForGroupCache with CacheStrategyComponent with QueueListener with InitializingBean with Logging with UserLookupComponent {
			val permissionsDao = new PermissionsDaoImpl
			val rolesByIdCache = new GrantedRoleByIdCache(permissionsDao)
			val permissionsByIdCache = new GrantedPermissionsByIdCache(permissionsDao)
			val cacheStrategy = CacheStrategy.InMemoryOnly
			val userLookup: MockUserLookup = userLookupService
		}

		val securityService: SecurityService = mock[SecurityService]
		val moduleAndDepartmentService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]
	}

	@Before def wire() {
		service.routeDao.asInstanceOf[RouteDaoImpl].sessionFactory = sessionFactory
		service.courseDao.asInstanceOf[CourseDaoImpl].sessionFactory = sessionFactory
		service.permissionsService.asInstanceOf[PermissionsDaoComponent].permissionsDao.asInstanceOf[PermissionsDaoImpl].sessionFactory = sessionFactory

		service.permissionsService.asInstanceOf[AbstractPermissionsService].queue = mock[Queue]
		service.permissionsService.asInstanceOf[AbstractPermissionsService].groupService = userLookupService.getGroupService()

		service.securityService.can(isA[CurrentUser],isA[Permission],isA[PermissionsTarget] ) returns true
	}

	@Test def crud() = transactional { tx =>
		// uses data created in data.sql
		val g500 = service.getRouteByCode("g500").get
		val g503 = service.getRouteByCode("g503").get
		val g900 = service.getRouteByCode("g900").get
		val g901 = service.getRouteByCode("g901").get

		service.allRoutes should be (Seq(g500, g503, g900, g901))

		// behaviour of child/parent departments
		val deptDao = new DepartmentDaoImpl
		deptDao.sessionFactory = sessionFactory

		val ch = deptDao.getByCode("ch").get
		val cs = deptDao.getByCode("cs").get

		service.getRouteByCode("g500") should be (Some(g500))
		service.getRouteById(g500.id) should be (Some(g500))
		service.getRouteByCode("wibble") should be (None)
		service.getRouteById("wibble") should be (None)

		withUser("cuscav") {
			userLookupService.registerUserObjects(currentUser.apparentUser)

			service.routesWithPermission(currentUser, Permissions.MonitoringPoints.Manage) should be (Set())
			service.routesWithPermission(currentUser, Permissions.MonitoringPoints.Manage, cs) should be (Set())
			service.routesWithPermission(currentUser, Permissions.MonitoringPoints.Manage, ch) should be (Set())

			service.addRouteManager(g503, "cuscav")
			service.routesWithPermission(currentUser, Permissions.MonitoringPoints.Manage) should be (Set(g503))
			service.routesWithPermission(currentUser, Permissions.MonitoringPoints.Manage, cs) should be (Set(g503))
			service.routesWithPermission(currentUser, Permissions.MonitoringPoints.Manage, ch) should be (Set())
		}
	}

}
