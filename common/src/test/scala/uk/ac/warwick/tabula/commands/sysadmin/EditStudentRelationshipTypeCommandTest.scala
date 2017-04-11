package uk.ac.warwick.tabula.commands.sysadmin

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.commands.{Appliable, Description}
import uk.ac.warwick.tabula.data.model.{StudentRelationshipSource, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{RelationshipService, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking

class EditStudentRelationshipTypeCommandTest extends TestBase with Mockito {

	private trait Fixture {
		val testRelationshipType = new StudentRelationshipType
		testRelationshipType.id = "trtId"
		testRelationshipType.urlPart = "trt-url"
		testRelationshipType.description = "trt role description"
		testRelationshipType.agentRole = "trt agent"
		testRelationshipType.studentRole = "trt student"
		testRelationshipType.defaultSource = StudentRelationshipSource.Local
		testRelationshipType.defaultDisplay = true
		testRelationshipType.expectedUG = false
		testRelationshipType.expectedPGT = false
		testRelationshipType.expectedPGR = false
		testRelationshipType.sortOrder = 9

		val commandInternal = new EditStudentRelationshipTypeCommandInternal(testRelationshipType) with RelationshipServiceComponent {
			var relationshipService: RelationshipService = mock[RelationshipService]
		}
	}

	@Test
	def objectApplyCreatesCommand() {
		new Fixture {
			val command = EditStudentRelationshipTypeCommand(testRelationshipType)

			command.isInstanceOf[Appliable[StudentRelationshipType]] should be(true)
		}
	}

	@Test
	def commandEditsRelationshipWhenApplied() {
		new Fixture {
			commandInternal.id = "theId"
			commandInternal.urlPart = "theUrlPart"
			commandInternal.description = "role description"
			commandInternal.agentRole = "agent"
			commandInternal.studentRole = "student"
			commandInternal.defaultSource = StudentRelationshipSource.SITS
			commandInternal.defaultDisplay = false
			commandInternal.expectedUG = true
			commandInternal.expectedPGT = true
			commandInternal.expectedPGR = true
			commandInternal.sortOrder = 1234

			commandInternal.applyInternal() should be (testRelationshipType)

			testRelationshipType.id should be ("theId")
			testRelationshipType.urlPart should be ("theUrlPart")
			testRelationshipType.description should be ("role description")
			testRelationshipType.agentRole should be ("agent")
			testRelationshipType.studentRole should be ("student")
			testRelationshipType.defaultSource should be (StudentRelationshipSource.SITS)
			testRelationshipType.defaultDisplay.booleanValue should be (false)
			testRelationshipType.expectedUG.booleanValue should be (true)
			testRelationshipType.expectedPGT.booleanValue should be (true)
			testRelationshipType.expectedPGR.booleanValue should be (true)
			testRelationshipType.sortOrder should be (1234)
		}
	}

	@Test
	def commandApplyInvokesSaveOnRelationshipService() {
		new Fixture {
			commandInternal.applyInternal()
			verify(commandInternal.relationshipService, times(1)).saveOrUpdate(testRelationshipType)
		}
	}

	@Test
	def commandDescriptionDescribedProperties() {
		new Fixture {
			val describable = new ModifyStudentRelationshipTypeCommandDescription with StudentRelationshipTypeProperties {
				val eventName: String = "test"
			}

			describable.id = "theId"
			describable.urlPart = "theUrlPart"
			describable.description = "role description"

			val description: Description = mock[Description]
			describable.describe(description)
			verify(description, times(1)).properties(
				"id" -> "theId",
				"urlPart" -> "theUrlPart",
				"description" -> "role description"
			)
		}
	}

	@Test
	def permissionsRequireGlobalStudentRelationshipTypeUpdate {
		new Fixture {
			val perms = new EditStudentRelationshipTypeCommandPermissions with HasExistingStudentRelationshipType {
				val relationshipType: StudentRelationshipType = testRelationshipType
			}
			val checking: PermissionsChecking = mock[PermissionsChecking]
			perms.permissionsCheck(checking)
			verify(checking, times(1)).PermissionCheck(Permissions.StudentRelationshipType.Manage)
		}
	}

	@Test
	def duplicateValidation {
		new Fixture {
			val existing = StudentRelationshipType("existing", "existing", "existing", "existing")

			commandInternal.relationshipService.getStudentRelationshipTypeByUrlPart("url") returns (None)
			commandInternal.relationshipService.getStudentRelationshipTypeByUrlPart("existing") returns (Some(existing))
			commandInternal.relationshipService.getStudentRelationshipTypeByUrlPart(testRelationshipType.urlPart) returns (Some(testRelationshipType))

			commandInternal.urlPart = "existing"
			var errors = new BindException(commandInternal, "command")
			commandInternal.validate(errors)
			errors.hasErrors should be (true)

			commandInternal.urlPart = "url"
			errors = new BindException(commandInternal, "command")
			commandInternal.validate(errors)
			errors.hasErrors should be (false)

			// doesn't fail when it matches itself
			commandInternal.urlPart = testRelationshipType.urlPart
			errors = new BindException(commandInternal, "command")
			commandInternal.validate(errors)
			errors.hasErrors should be (false)
		}
	}

}