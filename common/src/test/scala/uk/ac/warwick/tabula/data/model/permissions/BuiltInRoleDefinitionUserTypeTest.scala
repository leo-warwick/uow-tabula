package uk.ac.warwick.tabula.data.model.permissions

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.roles._
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.services.RelationshipService

class BuiltInRoleDefinitionUserTypeTest extends TestBase with Mockito {

	@Test def convertToObject() {
		val t = new BuiltInRoleDefinitionUserType
		t.convertToObject("DepartmentalAdministratorRoleDefinition") should be (DepartmentalAdministratorRoleDefinition)
		t.convertToObject("ModuleManagerRoleDefinition") should be (ModuleManagerRoleDefinition)
		t.convertToObject("SettingsOwnerRoleDefinition") should be (SettingsOwnerRoleDefinition)
		t.convertToObject("SubmitterRoleDefinition") should be (SubmitterRoleDefinition)
		an [IllegalArgumentException] should be thrownBy { t.convertToObject("Q") }
	}

	@Test def convertToValue() {
		val t = new BuiltInRoleDefinitionUserType
		t.convertToValue(DepartmentalAdministratorRoleDefinition) should be ("DepartmentalAdministratorRoleDefinition")
		t.convertToValue(ModuleManagerRoleDefinition) should be ("ModuleManagerRoleDefinition")
		t.convertToValue(SettingsOwnerRoleDefinition) should be ("SettingsOwnerRoleDefinition")
		t.convertToValue(SubmitterRoleDefinition) should be ("SubmitterRoleDefinition")
	}

	@Test def selectorRoles() {
		val t = new BuiltInRoleDefinitionUserType

		val tutorType = StudentRelationshipType("personalTutor", "tutor", "tutor", "tutee")
		t.relationshipService.set(mock[RelationshipService])
		t.relationshipService.get.getStudentRelationshipTypeById("personalTutor") returns (Some(tutorType))

		t.convertToObject("StudentRelationshipAgentRoleDefinition(*)") should be (StudentRelationshipAgentRoleDefinition(PermissionsSelector.Any[StudentRelationshipType]))
		t.convertToObject("StudentRelationshipAgentRoleDefinition(personalTutor)") should be (StudentRelationshipAgentRoleDefinition(tutorType))

		t.convertToValue(StudentRelationshipAgentRoleDefinition(PermissionsSelector.Any[StudentRelationshipType])) should be ("StudentRelationshipAgentRoleDefinition(*)")
		t.convertToValue(StudentRelationshipAgentRoleDefinition(tutorType)) should be ("StudentRelationshipAgentRoleDefinition(personalTutor)")
	}

}