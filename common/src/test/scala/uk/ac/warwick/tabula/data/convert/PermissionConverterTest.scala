package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.permissions.{PermissionsSelector, Permissions}
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

class PermissionConverterTest extends TestBase {

	val converter = new PermissionConverter

	@Test def validInput {
		converter.convertRight("Module.Create") should be (Permissions.Module.Create)
	}

	@Test def invalidInput {
		converter.convertRight("20X6") should be (null)
		converter.convertRight("") should be (null)
	}

	@Test def formatting {
		converter.convertLeft(Permissions.Module.Create) should be ("Module.Create")
		converter.convertLeft(null) should be (null)
	}

	@Test def selector {
		converter.convertRight("Profiles.StudentRelationship.Manage(*)") should be (Permissions.Profiles.StudentRelationship.Manage(PermissionsSelector.Any[StudentRelationshipType]))
		converter.convertLeft(Permissions.Profiles.StudentRelationship.Manage(PermissionsSelector.Any[StudentRelationshipType])) should be ("Profiles.StudentRelationship.Manage(*)")
	}

}