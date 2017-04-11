package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula._
import org.junit.Before
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget, Permissions}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.services.permissions._
import uk.ac.warwick.util.queue.Queue
import uk.ac.warwick.util.queue.QueueListener
import org.springframework.beans.factory.InitializingBean
import uk.ac.warwick.tabula.helpers.Logging
import scala.collection.JavaConverters._
import uk.ac.warwick.util.cache.Caches.CacheStrategy
import org.mockito.Mockito._

class ModuleAndDepartmentServiceTest extends PersistenceTestBase with Mockito {

	val service: ModuleAndDepartmentService = new ModuleAndDepartmentService

	val userLookupService = new MockUserLookup

	@Before def wire {
		val departmentDao = new DepartmentDaoImpl
		departmentDao.sessionFactory = sessionFactory
		service.departmentDao = departmentDao

		service.userLookup = userLookupService

		val moduleDao = new ModuleDaoImpl
		moduleDao.sessionFactory = sessionFactory
		service.moduleDao = moduleDao

		val permsDao = new PermissionsDaoImpl
		permsDao.sessionFactory = sessionFactory

		val permissionsService = new AbstractPermissionsService with PermissionsDaoComponent with PermissionsServiceCaches with GrantedRolesForUserCache with GrantedRolesForGroupCache with GrantedPermissionsForUserCache with GrantedPermissionsForGroupCache with CacheStrategyComponent with QueueListener with InitializingBean with Logging with UserLookupComponent {
			var permissionsDao:PermissionsDao = permsDao
			val rolesByIdCache:GrantedRoleByIdCache = new GrantedRoleByIdCache(permsDao)
			val permissionsByIdCache = new GrantedPermissionsByIdCache(permsDao)
			val cacheStrategy = CacheStrategy.InMemoryOnly
			val userLookup: MockUserLookup = userLookupService
		}
		permissionsService.queue = mock[Queue]
		permissionsService.groupService = userLookupService.getGroupService()
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

		service.allDepartments should be (Seq(ch, cs, cssub1, cssub2))
		service.allModules should be (Seq(cs108, cs240, cs241, cs242))

		// behaviour of child/parent departments
		cs.children.asScala.toSet should be (Set(cssub1, cssub2))
		cssub1.parent should be (cs)
		cssub2.parent should be (cs)
		ch.children.isEmpty should be (true)
		cs241.adminDepartment should be (cssub1)

		service.getDepartmentByCode("ch") should be (Some(ch))
		service.getDepartmentById(ch.id) should be (Some(ch))
		service.getDepartmentByCode("wibble") should be (None)
		service.getDepartmentById("wibble") should be (None)

		service.getDepartmentByCode("CH") should be (Some(ch))
		service.getDepartmentByCode(null) should be (None)


		service.getModuleByCode("cs108") should be (Some(cs108))
		service.getModuleById(cs108.id) should be (Some(cs108))
		service.getModuleByCode("wibble") should be (None)
		service.getModuleById("wibble") should be (None)

		withUser("cusebr") {
			userLookupService.registerUserObjects(currentUser.apparentUser)

			service.departmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set(cs))
		}

		withUser("cuscav") {
			userLookupService.registerUserObjects(currentUser.apparentUser)

			service.departmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set())
			service.modulesInDepartmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set())
			service.modulesInDepartmentWithPermission(currentUser, Permissions.Module.ManageAssignments, cs) should be (Set())
			service.modulesInDepartmentWithPermission(currentUser, Permissions.Module.ManageAssignments, ch) should be (Set())

			service.addOwner(cs, "cuscav")
			service.departmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set(cs))
			service.modulesInDepartmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set(cs108, cs240))
			service.modulesInDepartmentWithPermission(currentUser, Permissions.Module.ManageAssignments, cs) should be (Set(cs108, cs240))
			service.modulesInDepartmentWithPermission(currentUser, Permissions.Module.ManageAssignments, ch) should be (Set())

			service.removeOwner(cs, "cuscav")
			service.departmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set())

			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set())
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments, cs) should be (Set())
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments, ch) should be (Set())

			service.addModuleManager(cs108, "cuscav")
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set(cs108))
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments, cs) should be (Set(cs108))
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments, ch) should be (Set())
		}
	}

	@Test
	def testStampMissingModules {
		// spurn persistence and everything that has gone before
		val module1 = Fixtures.module("one", "my name is one")
		val module2 = Fixtures.module("two", "my name is two")
		val module3 = Fixtures.module("three", "my name is three")
		val module4 = Fixtures.module("four", "my name is four")
		val module5 = Fixtures.module("five", "my name is five")

		val service = new ModuleAndDepartmentService
		service.userLookup = userLookupService

		val moduleDao = smartMock[ModuleDao]
		moduleDao.allModules returns Seq(module1, module2, module3, module4, module5)
		service.moduleDao = moduleDao

		service.stampMissingModules(Seq("two", "four"))

		verify(moduleDao).stampMissingFromImport(Seq("one", "three", "five"))
	}

}
