package uk.ac.warwick.tabula.commands.profiles.relationships

import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports.{JArrayList, JHashMap}
import uk.ac.warwick.tabula.commands.profiles.relationships.AllocateStudentsToRelationshipCommand.Result
import uk.ac.warwick.tabula.commands.profiles.relationships.ExtractRelationshipsFromFileCommand.AllocationTypes
import uk.ac.warwick.tabula.data.model.{ExternalStudentRelationship, StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.services.{ProfileService, ProfileServiceComponent, RelationshipService, RelationshipServiceComponent}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class AllocateStudentsToRelationshipCommandTest extends TestBase with Mockito {

	trait Fixture extends StudentAssociationFixture {
		protected val thisRelationshipType = StudentRelationshipType("tutor","tutor","tutor","tutee")
		val command = new AllocateStudentsToRelationshipCommandInternal(null, null, null)
			with AllocateStudentsToRelationshipCommandRequest with ManageStudentRelationshipsState
			with RelationshipServiceComponent with ProfileServiceComponent {

			val relationshipService: RelationshipService = smartMock[RelationshipService]
			relationshipService.getStudentAssociationDataWithoutRelationship(null, null, Seq()) returns mockDbUnallocated
			relationshipService.getStudentAssociationEntityData(null, null, Seq()) returns mockDbAllocated

			val profileService: ProfileService = smartMock[ProfileService]
		}
	}

	@Test
	def renderAdditions(): Unit = {
		new Fixture {
			command.additions = JHashMap(
				"1" -> JArrayList("1", "8"), // Student 1 already assigned to agent 2, but no problem with adding additional agent
				"2" -> JArrayList("1") // Student 1 already assigned to agent 2, so this should be ignored
			)
			command.renderAdditions.keySet.size should be (2)
			command.renderAdditions.keySet.map(_.universityId).contains("1") should be {true}
			command.renderAdditions.keySet.map(_.universityId).contains("8") should be {true}
			command.renderAdditions(student1).size should be (1)
			command.renderAdditions(student1).head should be ("Fred")
			command.renderAdditions(student8).size should be (1)
			command.renderAdditions(student8).head should be ("Fred")
		}
	}

	@Test
	def renderRemovalsNoAllocationType(): Unit = {
		new Fixture {
			command.removals = JHashMap(
				"2" -> JArrayList("1"),
				"3" -> JArrayList("2")
			)
			command.renderRemovals.keySet.size should be (0)
		}
	}

	@Test
	def renderRemovalsForAdd(): Unit = {
		new Fixture {
			command.allocationType = AllocationTypes.Add
			command.removals = JHashMap(
				"2" -> JArrayList("1"), // Regular removal
				"3" -> JArrayList("2") // Student 2 isn't assigned to agent 3, so should be ignored
			)
			command.renderRemovals.keySet.size should be (1)
			command.renderRemovals.keySet.map(_.universityId).contains("1") should be {true}
			command.renderRemovals(student1).size should be (1)
			command.renderRemovals(student1).head should be ("Jeff")
		}
	}

	@Test
	def renderRemovalsForReplace(): Unit = {
		new Fixture {
			command.allocationType = AllocationTypes.Replace
			command.additions = JHashMap(
				"3" -> JArrayList("1") // New tutor for student 1, replaces agent 2
			)
			command.removals = JHashMap(
				"2" -> JArrayList("2") // Should be ignored
			)
			command.renderRemovals.keySet.size should be (1)
			command.renderRemovals.keySet.map(_.universityId).contains("1") should be {true}
			command.renderRemovals(student1).size should be (1)
			command.renderRemovals(student1).head should be ("Jeff")
		}
	}

	trait DbFixture extends Fixture {
		protected val studentMember1: StudentMember = Fixtures.student("1")
		protected val studentMember2: StudentMember = Fixtures.student("2")
		protected val studentMember3: StudentMember = Fixtures.student("3")
		protected val studentMember4: StudentMember = Fixtures.student("4")
		command.relationshipService.listCurrentRelationshipsWithAgent(null, "1") returns Seq()
		protected val agent2Rels = Seq(
			ExternalStudentRelationship("2", null, studentMember1, DateTime.now),
			ExternalStudentRelationship("2", null, studentMember2, DateTime.now)
		)
		command.relationshipService.listCurrentRelationshipsWithAgent(null, "2") returns agent2Rels
		protected val agent3Rels = Seq(
			ExternalStudentRelationship("2", null, studentMember3, DateTime.now),
			ExternalStudentRelationship("2", null, studentMember4, DateTime.now)
		)
		command.relationshipService.listCurrentRelationshipsWithAgent(null, "3") returns agent3Rels
	}

	def applyAdd(): Unit = {
		new DbFixture {
			command.allocationType = AllocationTypes.Add
			command.additions = JHashMap(
				"1" -> JArrayList("1", "8"), // Student 1 already assigned to agent 2, but no problem with adding additional agent
				"2" -> JArrayList("2") // Student 2 already assigned to agent 2, so this should be ignored
			)
			command.removals = JHashMap(
				"2" -> JArrayList("1"), // Regular removal
				"3" -> JArrayList("2") // Student 2 isn't assigned to agent 3, so should be ignored
			)
			val result: Result = command.applyInternal()
			result.addedRelationships.size should be (1)
			result.addedRelationships.head.agent should be ("1")
			result.addedRelationships.head.studentMember.get.universityId should be ("1")
			result.expiredRelationships.size should be (1)
			result.expiredRelationships.head.agent should be ("2")
			result.expiredRelationships.head.studentMember.get.universityId should be ("1")
			verify(command.relationshipService, times(1)).endStudentRelationships(Seq(agent2Rels.head), DateTime.now)
			verify(command.relationshipService, times(1)).saveStudentRelationship(thisRelationshipType, studentMember1.mostSignificantCourse, Right("1"), DateTime.now)
		}
	}

	def applyReplace(): Unit = {
		new DbFixture {
			command.allocationType = AllocationTypes.Replace
			command.additions = JHashMap(
				"3" -> JArrayList("1") // New tutor for student 1, replaces agent 2
			)
			command.removals = JHashMap(
				"2" -> JArrayList("2") // Should be ignored
			)
			val result: Result = command.applyInternal()
			result.addedRelationships.size should be (1)
			result.addedRelationships.head.agent should be ("3")
			result.addedRelationships.head.studentMember.get.universityId should be ("1")
			result.expiredRelationships.size should be (1)
			result.expiredRelationships.head.agent should be ("2")
			result.expiredRelationships.head.studentMember.get.universityId should be ("1")
			verify(command.relationshipService, times(1)).endStudentRelationships(Seq(agent2Rels.head), DateTime.now)
			verify(command.relationshipService, times(1)).saveStudentRelationship(thisRelationshipType, studentMember3.mostSignificantCourse, Right("1"), DateTime.now)
		}
	}

}
