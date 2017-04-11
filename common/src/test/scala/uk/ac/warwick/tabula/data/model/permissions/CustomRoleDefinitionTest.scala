package uk.ac.warwick.tabula.data.model.permissions

import uk.ac.warwick.tabula.{TestBase, Fixtures}
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.JavaImports._

class CustomRoleDefinitionTest extends TestBase {

	case object TestBuiltInDefinition extends BuiltInRoleDefinition {
		import Permissions._

		override def description = "Test"

		GrantsScopelessPermission(ImportSystemData)
		GrantsScopedPermission(Department.ManageExtensionSettings)
		GrantsGlobalPermission(Module.Create)
		def canDelegateThisRolesPermissions:JBoolean = false

	}

	@Test def polymorphicGetterAndSetter {
		val crd = new CustomRoleDefinition
		crd.baseRoleDefinition should be (null)

		crd.baseRoleDefinition = TestBuiltInDefinition
		crd.baseRoleDefinition should be (TestBuiltInDefinition)
		crd.builtInBaseRoleDefinition should be (TestBuiltInDefinition)
		crd.customBaseRoleDefinition should be (null)

		val other = new CustomRoleDefinition
		crd.baseRoleDefinition = other
		crd.baseRoleDefinition should be (other)
		crd.builtInBaseRoleDefinition should be (null)
		crd.customBaseRoleDefinition should be (other)
	}

	@Test def getPermissions {
		val crd = new CustomRoleDefinition
		crd.baseRoleDefinition = TestBuiltInDefinition

		val dept = Fixtures.department("in")

		crd.permissions(Some(dept)) should be (Map(
			Permissions.Department.ManageExtensionSettings -> Some(dept),
			Permissions.Module.Create -> None,
			Permissions.ImportSystemData -> None
		))

		val ro1 = new RoleOverride
		ro1.permission = Permissions.Module.ManageAssignments
		ro1.overrideType = RoleOverride.Allow

		crd.overrides.add(ro1)

		crd.permissions(Some(dept)) should be (Map(
			Permissions.Department.ManageExtensionSettings -> Some(dept),
			Permissions.Module.Create -> None,
			Permissions.Module.ManageAssignments -> Some(dept),
			Permissions.ImportSystemData -> None
		))

		val ro2 = new RoleOverride
		ro2.permission = Permissions.Department.ManageExtensionSettings
		ro2.overrideType = RoleOverride.Deny

		crd.overrides.add(ro2)

		crd.permissions(Some(dept)) should be (Map(
			Permissions.Module.Create -> None,
			Permissions.Module.ManageAssignments -> Some(dept),
			Permissions.ImportSystemData -> None
		))

		val crd2 = new CustomRoleDefinition
		crd2.baseRoleDefinition = crd

		val ro3 = new RoleOverride
		ro3.permission = Permissions.Department.ManageExtensionSettings
		ro3.overrideType = RoleOverride.Allow

		crd2.overrides.add(ro3)

		crd2.permissions(Some(dept)) should be (Map(
			Permissions.Department.ManageExtensionSettings -> Some(dept),
			Permissions.Module.Create -> None,
			Permissions.Module.ManageAssignments -> Some(dept),
			Permissions.ImportSystemData -> None
		))
	}

	@Test
	def delegatablePermisionsIsNilIfCanDelegateIsFalse(){
		val crd = new CustomRoleDefinition
		crd.baseRoleDefinition = TestBuiltInDefinition
		val dept = Fixtures.department("in")

		crd.permissions(Some(dept)) should not be (Map.empty)

		crd.delegatablePermissions(Some(dept)) should be (Map.empty)
	}

	@Test
	def delegatablePermisionsIsEqualToAllPermisionsIfCanDelegateIsTrue(){
		val crd = new CustomRoleDefinition
		crd.baseRoleDefinition = TestBuiltInDefinition
		val dept = Fixtures.department("in")
		crd.canDelegateThisRolesPermissions = true
		crd.permissions(Some(dept)) should not be (Map.empty)

		crd.delegatablePermissions(Some(dept)) should be (crd.permissions(Some(dept)))
	}

	/*
	* Whether or not a base role definition sets the "canDelegate" flag is irrelevant to this Role's delegatability
	*
	* The permissions that a role grants are always delegatable if that role has canDelegate=true, and never if not
	*
	* Otherwise, you'd end up having to make delegatable copies of every base role, and keep them in sync with the
	* originals, which kind of misses the point of re-using the base role definition
	 */
	@Test
	def delegatablePermisionsIsIgnoredOnBaseRoleDefinition(){
		val baseCrd = new CustomRoleDefinition
		baseCrd.baseRoleDefinition = TestBuiltInDefinition
		val dept = Fixtures.department("in")

		baseCrd.canDelegateThisRolesPermissions = true

		val derivedCrd = new CustomRoleDefinition
		derivedCrd.customBaseRoleDefinition = baseCrd

		derivedCrd.permissions(Some(dept)) should not be (Map.empty)
		derivedCrd.delegatablePermissions(Some(dept)) should be (Map.empty)


		baseCrd.canDelegateThisRolesPermissions = false
		derivedCrd.canDelegateThisRolesPermissions = true

		derivedCrd.delegatablePermissions(Some(dept)) should be (baseCrd.permissions(Some(dept)))
	}


}