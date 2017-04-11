package uk.ac.warwick.tabula.commands.sysadmin

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{RelationshipService, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking

class ListStudentRelationshipTypesCommandTest extends TestBase with Mockito {

	private trait Fixture {
		val commandInternal = new ListStudentRelationshipTypesCommandInternal with RelationshipServiceComponent {
			var relationshipService: RelationshipService = mock[RelationshipService]
		}
	}

	@Test
	def objectApplyCreatesCommand() {
		new Fixture {
			val command = ListStudentRelationshipTypesCommand()

			command.isInstanceOf[Appliable[Seq[StudentRelationshipType]]] should be(true)
		}
	}

	@Test
	def commandListsRelationshipTypesWhenApplied() {
		new Fixture {
			val tutorRT = StudentRelationshipType("tutor", "tutor", "tutor", "tutee")
			val supervisorRT = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisee")

			val allTypes = Seq(tutorRT, supervisorRT)

			commandInternal.relationshipService.allStudentRelationshipTypes returns (allTypes)

			commandInternal.applyInternal() should be (allTypes)

			verify(commandInternal.relationshipService, times(1)).allStudentRelationshipTypes
		}
	}

	@Test
	def permissionsRequireGlobalStudentRelationshipTypeRead {
		new Fixture {
			val perms = new ListStudentRelationshipTypesCommandPermissions() {}
			val checking: PermissionsChecking = mock[PermissionsChecking]
			perms.permissionsCheck(checking)
			verify(checking, times(1)).PermissionCheck(Permissions.StudentRelationshipType.Read)
		}
	}

}