package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.{PermissionsSelector, PermissionsTarget}
import uk.ac.warwick.tabula.roles._
import uk.ac.warwick.tabula.{CurrentUser, Fixtures, Mockito, TestBase}
import scala.language.reflectiveCalls

class RoleProviderTest extends TestBase with Mockito {

	val personalTutorRelationshipType = StudentRelationshipType("personalTutor", "personalTutor", "personalTutor", "personalTutor")

	@Test def noElevatedSelector() = withUser("cuscav", "0672089") {
		val service = new RoleProvider {
			override def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role] = Stream()
			override def rolesProvided: Set[Class[_ <: Role]] = Set()
			def testCustomRolesFor[A <: PermissionsTarget](department: Department, definition: RoleDefinition, scope: A): Option[Role] =
				customRoleFor(department)(definition, scope)
		}

		val customWildcardSelectorRoleDefinition = new CustomRoleDefinition
		customWildcardSelectorRoleDefinition.baseRoleDefinition = StudentRelationshipAgentRoleDefinition(PermissionsSelector.Any[StudentRelationshipType])
		customWildcardSelectorRoleDefinition.replacesBaseDefinition = true
		val department = Fixtures.department("its")
		department.customRoleDefinitions = JArrayList(customWildcardSelectorRoleDefinition)

		val originalRoleDefinition = StudentRelationshipAgentRoleDefinition(personalTutorRelationshipType)

		val customRole = service.testCustomRolesFor(department, originalRoleDefinition, null)

		customRole.get.definition match {
			case customRoleDefinition: CustomRoleDefinition =>
				// Check original hasn't been changed (otherwise the restricted selector is persisted)
				department.customRoleDefinitions.size should be (1)
				department.customRoleDefinitions.get(0).builtInBaseRoleDefinition.asInstanceOf[SelectorBuiltInRoleDefinition[StudentRelationshipType]].selector should be (PermissionsSelector.Any[StudentRelationshipType])
				customRoleDefinition.baseRoleDefinition match {
					case selectorDefinition: SelectorBuiltInRoleDefinition[_] =>
						selectorDefinition.selector should be(personalTutorRelationshipType)
					case _ =>
						fail("customRole.head.definition should be a SelectorBuiltInRoleDefinition")
				}
		}
	}

}