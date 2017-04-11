package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.{PersistenceTestBase, Fixtures}
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.data.model.permissions.DepartmentGrantedPermission
import uk.ac.warwick.tabula.data.model.permissions.DepartmentGrantedRole
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import uk.ac.warwick.tabula.data.model.permissions.RoleOverride
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.roles.ModuleManagerRoleDefinition
import uk.ac.warwick.userlookup.User
import org.junit.Before

class PermissionsDaoTest extends PersistenceTestBase {

	val dao = new PermissionsDaoImpl

	@Before
	def setup() {
		dao.sessionFactory = sessionFactory
	}

	@Test def crud = transactional { t =>
		val dept1 = Fixtures.department("dp1")
		val dept2 = Fixtures.department("dp2")

		session.save(dept1)
		session.save(dept2)
		session.flush()

		val gr1 = new DepartmentGrantedRole(dept1, DepartmentalAdministratorRoleDefinition)
		gr1.users.knownType.addUserId("cusbruv")
		gr1.users.knownType.addUserId("cusxar")
		dao.saveOrUpdate(gr1)

		val crd = new CustomRoleDefinition
		crd.department = dept1
		crd.name = "Custom def"
		crd.builtInBaseRoleDefinition = ModuleManagerRoleDefinition

		val ro = new RoleOverride
		ro.permission = Permissions.Module.ManageAssignments
		ro.overrideType = RoleOverride.Deny

		crd.overrides.add(ro)

		dao.saveOrUpdate(crd)

		val gr2 = new DepartmentGrantedRole(dept1, crd)
		gr2.users.knownType.addUserId("cusbruv")
		gr2.users.knownType.addUserId("cuscao")
		dao.saveOrUpdate(gr2)

		val gp = new DepartmentGrantedPermission(dept1, Permissions.Module.Create, GrantedPermission.Allow)
		gp.users.knownType.addUserId("cusbruv")
		gp.users.knownType.addUserId("cuscao")
		dao.saveOrUpdate(gp)

		session.flush()

		dao.getGrantedRole(dept1, DepartmentalAdministratorRoleDefinition) should be (Some(gr1))
		dao.getGrantedRole(dept1, crd) should be (Some(gr2))
		dao.getGrantedRole(dept1, ModuleManagerRoleDefinition) should be (None)
		dao.getGrantedRole(dept2, DepartmentalAdministratorRoleDefinition) should be (None)
		dao.getGrantedRole(dept2, crd) should be (None)

		dao.getGrantedPermission(dept1, Permissions.Module.Create, GrantedPermission.Allow) should be (Some(gp))
		dao.getGrantedPermission(dept1, Permissions.Module.Create, GrantedPermission.Deny) should be (None)
		dao.getGrantedPermission(dept1, Permissions.Module.ManageAssignments, GrantedPermission.Allow) should be (None)
		dao.getGrantedPermission(dept2, Permissions.Module.Create, GrantedPermission.Allow) should be (None)

		dao.getGrantedRolesForUser(new User("cusbruv")).toSet should be (Set(gr1, gr2))
		dao.getGrantedRolesForUser(new User("cusxar")) should be (Seq(gr1))
		dao.getGrantedRolesForUser(new User("cuscao")) should be (Seq(gr2))
		dao.getGrantedRolesForUser(new User("cusfaq")) should be (Seq())

		dao.getGrantedPermissionsForUser(new User("cusbruv")) should be (Seq(gp))
		dao.getGrantedPermissionsForUser(new User("cusxar")) should be (Seq())
		dao.getGrantedPermissionsForUser(new User("cuscao")) should be (Seq(gp))
		dao.getGrantedPermissionsForUser(new User("cusfaq")) should be (Seq())
	}

	@Test def guards = transactional { tx =>
		// Make sure we don't throw an exception with a permissions type we don't know how to set roles/permissions for
		val scope = Fixtures.userSettings("cuscav")

		dao.getGrantedRolesFor(scope) should be ('empty)
		dao.getGrantedPermissionsFor(scope) should be ('empty)

		val crd = new CustomRoleDefinition
		session.save(crd)

		dao.getGrantedRole(scope, DepartmentalAdministratorRoleDefinition) should be ('empty)
		dao.getGrantedRole(scope, crd) should be ('empty)
		dao.getGrantedPermission(scope, Permissions.Module.Create, true) should be ('empty)
	}

}
