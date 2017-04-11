package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{Mockito, PersistenceTestBase, Fixtures}
import uk.ac.warwick.tabula.services.permissions._
import uk.ac.warwick.tabula.data.model.permissions.DepartmentGrantedRole
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.roles.ModuleManagerRoleDefinition
import uk.ac.warwick.tabula.data.model.permissions.RoleOverride
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.permissions.DepartmentGrantedPermission
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import uk.ac.warwick.tabula.data.{PermissionsDao, PermissionsDaoComponent, PermissionsDaoImpl}
import uk.ac.warwick.util.queue.Queue
import org.junit.Before
import uk.ac.warwick.util.queue.QueueListener
import org.springframework.beans.factory.InitializingBean
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.MockUserLookup
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.util.cache.Caches.CacheStrategy

class PermissionsServiceTest extends PersistenceTestBase with Mockito {

	val permsDao = new PermissionsDaoImpl

	val service = new AbstractPermissionsService with PermissionsDaoComponent with PermissionsServiceCaches with GrantedRolesForUserCache with GrantedRolesForGroupCache with GrantedPermissionsForUserCache with GrantedPermissionsForGroupCache with CacheStrategyComponent with QueueListener with InitializingBean with Logging with UserLookupComponent {
		var permissionsDao:PermissionsDao = permsDao
		val rolesByIdCache:GrantedRoleByIdCache = new GrantedRoleByIdCache(permsDao)
		val permissionsByIdCache = new GrantedPermissionsByIdCache(permsDao)
		val cacheStrategy = CacheStrategy.InMemoryOnly
		val userLookup = new MockUserLookup()
	}
	service.queue = mock[Queue]

	@Before
	def setup(){
		permsDao.sessionFactory = sessionFactory
	}


	@Test def crud = transactional { t =>
		val dept1 = Fixtures.department("dp1")
		val dept2 = Fixtures.department("dp2")

		session.save(dept1)
		session.save(dept2)
		session.flush()

		val gr1 = new DepartmentGrantedRole(dept1, DepartmentalAdministratorRoleDefinition)
		gr1.users.knownType.addUserId("cuscav")
		gr1.users.knownType.addUserId("cusebr")
		service.saveOrUpdate(gr1)

		val crd = new CustomRoleDefinition
		crd.department = dept1
		crd.name = "Custom def"
		crd.builtInBaseRoleDefinition = ModuleManagerRoleDefinition

		val ro = new RoleOverride
		ro.permission = Permissions.Module.ManageAssignments
		ro.overrideType = RoleOverride.Deny

		crd.overrides.add(ro)

		service.saveOrUpdate(crd)

		val gr2 = new DepartmentGrantedRole(dept1, crd)
		gr2.users.knownType.addUserId("cuscav")
		gr2.users.knownType.addUserId("cuscao")
		service.saveOrUpdate(gr2)

		val gp = new DepartmentGrantedPermission(dept1, Permissions.Module.Create, GrantedPermission.Allow)
		gp.users.knownType.addUserId("cuscav")
		gp.users.knownType.addUserId("cuscao")
		service.saveOrUpdate(gp)

		session.flush()

		service.getGrantedRole(dept1, DepartmentalAdministratorRoleDefinition) should be (Some(gr1))
		service.getGrantedRole(dept1, crd) should be (Some(gr2))
		service.getGrantedRole(dept1, ModuleManagerRoleDefinition) should be (None)
		service.getGrantedRole(dept2, DepartmentalAdministratorRoleDefinition) should be (None)
		service.getGrantedRole(dept2, crd) should be (None)

		service.getGrantedPermission(dept1, Permissions.Module.Create, GrantedPermission.Allow) should be (Some(gp))
		service.getGrantedPermission(dept1, Permissions.Module.Create, GrantedPermission.Deny) should be (None)
		service.getGrantedPermission(dept1, Permissions.Module.ManageAssignments, GrantedPermission.Allow) should be (None)
		service.getGrantedPermission(dept2, Permissions.Module.Create, GrantedPermission.Allow) should be (None)

		withUser("cuscav") {
			service.getGrantedRolesFor(currentUser, dept1).toSet should be (Set(gr1, gr2))
			service.getGrantedRolesFor(currentUser, dept2) should be (Seq())

			service.getGrantedPermissionsFor(currentUser, dept1) should be (Seq(gp))
			service.getGrantedPermissionsFor(currentUser, dept2) should be (Seq())
		}

		withUser("cuscao") {
			service.getGrantedRolesFor(currentUser, dept1) should be (Seq(gr2))
			service.getGrantedRolesFor(currentUser, dept2) should be (Seq())

			service.getGrantedPermissionsFor(currentUser, dept1) should be (Seq(gp))
			service.getGrantedPermissionsFor(currentUser, dept2) should be (Seq())
		}

		withUser("curef") {
			service.getGrantedRolesFor(currentUser, dept1) should be (Seq())
			service.getGrantedRolesFor(currentUser, dept2) should be (Seq())

			service.getGrantedPermissionsFor(currentUser, dept1) should be (Seq())
			service.getGrantedPermissionsFor(currentUser, dept2) should be (Seq())
		}

		service.ensureUserGroupFor(dept1, DepartmentalAdministratorRoleDefinition) should be (gr1.users)
		service.ensureUserGroupFor(dept2, DepartmentalAdministratorRoleDefinition).asInstanceOf[UserGroup].id should not be (null)
	}

	@Test def guards = transactional { t => withUser("cuscav") {
		// Make sure we don't throw an exception with a permissions type we don't know how to set roles/permissions for
		val scope = Fixtures.userSettings("cuscav")

		service.getGrantedRolesFor(currentUser, scope) should be ('empty)
		service.getGrantedPermissionsFor(currentUser, scope) should be ('empty)

		val crd = new CustomRoleDefinition
		session.save(crd)

		service.getGrantedRole(scope, DepartmentalAdministratorRoleDefinition) should be ('empty)
		service.getGrantedRole(scope, crd) should be ('empty)
		service.getGrantedPermission(scope, Permissions.Module.Create, true) should be ('empty)
	}}

}