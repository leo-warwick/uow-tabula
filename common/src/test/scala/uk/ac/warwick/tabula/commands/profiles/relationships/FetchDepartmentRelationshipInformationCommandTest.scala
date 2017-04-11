package uk.ac.warwick.tabula.commands.profiles.relationships

import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.commands.{StudentAssociationData, StudentAssociationEntityData, StudentAssociationResult}
import uk.ac.warwick.tabula.services.{ProfileService, ProfileServiceComponent, RelationshipService, RelationshipServiceComponent}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

import scala.collection.JavaConverters._

trait StudentAssociationFixture {
	val student1 = StudentAssociationData("1", "", "", null, null, 0)
	val student2 = StudentAssociationData("2", "", "", null, null, 0)
	val student3 = StudentAssociationData("3", "", "", null, null, 0)
	val student4 = StudentAssociationData("4", "", "", null, null, 0)
	val student5 = StudentAssociationData("5", "", "", null, null, 0)
	val student6 = StudentAssociationData("6", "", "", null, null, 0)
	val student7 = StudentAssociationData("7", "", "", null, null, 0)
	val student8 = StudentAssociationData("8", "Fred", "Jones", null, null, 0)
	val student9 = StudentAssociationData("9", "Samuel Jones", "Partington Smyth", null, null, 0)
	val student10 = StudentAssociationData("10", "", "", null, null, 0)
	val student11 = StudentAssociationData("11", "", "", null, null, 0)

	val mockDbUnallocated = Seq(student8, student9, student10, student11)

	val mockDbAllocated = Seq(
		StudentAssociationEntityData("1", "Fred", "Fred", null, null, Seq()),
		StudentAssociationEntityData("2", "Jeff", "Jeff", null, null, Seq(student1, student2)),
		StudentAssociationEntityData("3", "Steve", "Steve", null, null, Seq(student3, student4)),
		StudentAssociationEntityData("4", null, null, null, null, Seq(student5, student6, student7))
	)
}

class FetchDepartmentRelationshipInformationCommandTest extends TestBase with Mockito {

	trait Fixture extends StudentAssociationFixture {
		val command = new FetchDepartmentRelationshipInformationCommandInternal(null, null)
			with FetchDepartmentRelationshipInformationCommandRequest with FetchDepartmentRelationshipInformationCommandState
			with RelationshipServiceComponent with ProfileServiceComponent {

			val relationshipService: RelationshipService = smartMock[RelationshipService]
			relationshipService.getStudentAssociationDataWithoutRelationship(null, null, Seq()) returns mockDbUnallocated
			relationshipService.getStudentAssociationEntityData(null, null, Seq()) returns mockDbAllocated
			val profileService: ProfileService = smartMock[ProfileService]
			profileService.getAllMembersWithUserId("cusfal") returns Seq(Fixtures.staff("0770884", "cusfal"))
			relationshipService.getStudentAssociationEntityData(null, null, Seq("0770884")) returns Seq(
				StudentAssociationEntityData("1", null, null, null, null, Seq()),
				StudentAssociationEntityData("2", null, null, null, null, Seq(student1, student2)),
				StudentAssociationEntityData("3", null, null, null, null, Seq(student3, student4)),
				StudentAssociationEntityData("4", null, null, null, null, Seq(student5, student6, student7)),
				StudentAssociationEntityData("0770884", null, null, null, null, Seq())
			)
		}
	}

	@Test
	def query(): Unit = {
		new Fixture {
			command.query = "JO"
			val result: StudentAssociationResult = command.applyInternal()
			result.unallocated.size should be (2)
			result.unallocated.contains(student8) should be {true}
			result.unallocated.contains(student9) should be {true}
		}
		new Fixture {
			command.query = "pa sm"
			val result: StudentAssociationResult = command.applyInternal()
			result.unallocated.size should be (1)
			result.unallocated.contains(student9) should be {true}
		}
		new Fixture {
			command.query = "Sam Jones"
			val result: StudentAssociationResult = command.applyInternal()
			result.unallocated.size should be (1)
			result.unallocated.contains(student9) should be {true}
		}
	}

	@Test
	def distributeAll(): Unit = {
		new Fixture {
			command.entities.addAll(Seq("1", "2", "3").asJava)
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.DistributeAll
			val result: StudentAssociationResult = command.applyInternal()
			result.unallocated.size should be (0)
			result.allocated.size should be (4)
			result.allocated.flatMap(_.students).distinct.size should be (11)
			val sizes: Seq[Int] = result.allocated.map(_.students.size)
			sizes.count(_ == 3) should be (3)
			sizes.count(_ == 2) should be (1)
			mockDbUnallocated.map(_.universityId).foreach(id => {
				command.additions.get(result.allocated.find(_.students.exists(_.universityId == id)).get.entityId).contains(id) should be {true}
			})
		}

		new Fixture {
			command.entities.addAll(Seq("1", "2", "3", "4").asJava)
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.DistributeAll
			val result: StudentAssociationResult = command.applyInternal()
			result.unallocated.size should be (0)
			result.allocated.size should be (4)
			result.allocated.flatMap(_.students).distinct.size should be (11)
			val sizes: Seq[Int] = result.allocated.map(_.students.size)
			sizes.count(_ == 3) should be (3)
			sizes.count(_ == 2) should be (1)
			mockDbUnallocated.map(_.universityId).foreach(id => {
				command.additions.get(result.allocated.find(_.students.exists(_.universityId == id)).get.entityId).contains(id) should be {true}
			})
		}

		new Fixture {
			command.entities.addAll(Seq("3", "4").asJava)
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.DistributeAll
			val result: StudentAssociationResult = command.applyInternal()
			result.unallocated.size should be (0)
			result.allocated.size should be (4)
			result.allocated.flatMap(_.students).distinct.size should be (11)
			result.allocated.find(_.entityId == "1").get.students.size should be (0)
			result.allocated.find(_.entityId == "2").get.students.size should be (2)
			result.allocated.find(_.entityId == "3").get.students.size should be (5)
			result.allocated.find(_.entityId == "4").get.students.size should be (4)
			mockDbUnallocated.map(_.universityId).foreach(id => {
				command.additions.get(result.allocated.find(_.students.exists(_.universityId == id)).get.entityId).contains(id) should be {true}
			})
		}
	}

	@Test
	def distributeSelected(): Unit = {
		new Fixture {
			val allocated = Seq("8", "9", "10", "11")
			command.allocate.addAll(allocated.asJava)
			command.entities.addAll(Seq("1", "2", "3").asJava)
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.DistributeSelected
			val result: StudentAssociationResult = command.applyInternal()
			result.unallocated.size should be (0)
			result.allocated.size should be (4)
			result.allocated.flatMap(_.students).distinct.size should be (11)
			val sizes: Seq[Int] = result.allocated.map(_.students.size)
			sizes.count(_ == 3) should be (3)
			sizes.count(_ == 2) should be (1)
			allocated.foreach(id => {
				command.additions.get(result.allocated.find(_.students.exists(_.universityId == id)).get.entityId).contains(id) should be {true}
			})
		}

		new Fixture {
			val allocated = Seq("8", "9", "10")
			command.allocate.addAll(allocated.asJava)
			command.entities.addAll(Seq("1", "2", "3", "4").asJava)
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.DistributeSelected
			val result: StudentAssociationResult = command.applyInternal()
			result.unallocated.size should be (1)
			result.allocated.size should be (4)
			result.allocated.flatMap(_.students).distinct.size should be (10)
			val sizes: Seq[Int] = result.allocated.map(_.students.size)
			sizes.count(_ == 3) should be (2)
			sizes.count(_ == 2) should be (2)
			allocated.foreach(id => {
				command.additions.get(result.allocated.find(_.students.exists(_.universityId == id)).get.entityId).contains(id) should be {true}
			})
		}

		new Fixture {
			val allocated = Seq("8", "9", "10")
			command.allocate.addAll(allocated.asJava)
			command.entities.addAll(Seq("3", "4").asJava)
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.DistributeSelected
			val result: StudentAssociationResult = command.applyInternal()
			result.unallocated.size should be (1)
			result.allocated.size should be (4)
			result.allocated.flatMap(_.students).distinct.size should be (10)
			result.allocated.find(_.entityId == "1").get.students.size should be (0)
			result.allocated.find(_.entityId == "2").get.students.size should be (2)
			result.allocated.find(_.entityId == "3").get.students.size should be (4)
			result.allocated.find(_.entityId == "4").get.students.size should be (4)
			allocated.foreach(id => {
				command.additions.get(result.allocated.find(_.students.exists(_.universityId == id)).get.entityId).contains(id) should be {true}
			})
		}
	}

	@Test
	def removeSingle(): Unit = {
		new Fixture {
			command.studentToRemove = "1"
			command.entityToRemoveFrom = "2"
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.RemoveSingle
			val result: StudentAssociationResult = command.applyInternal()
			result.unallocated.size should be (5)
			result.allocated.size should be (4)
			result.allocated.flatMap(_.students).distinct.size should be (6)
			result.allocated.find(_.entityId == "2").get.students.size should be (1)
			command.removals.get("2").contains("1") should be {true}
		}
	}

	@Test
	def removeFromAll(): Unit = {
		new Fixture {
			command.entities.addAll(Seq("3", "4").asJava)
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.RemoveFromAll
			val result: StudentAssociationResult = command.applyInternal()
			result.unallocated.size should be (9)
			result.allocated.size should be (4)
			result.allocated.flatMap(_.students).distinct.size should be (2)
			result.allocated.find(_.entityId == "3").get.students.size should be (0)
			result.allocated.find(_.entityId == "4").get.students.size should be (0)
			command.removals.get("3").contains("3") should be {true}
			command.removals.get("3").contains("4") should be {true}
			command.removals.get("4").contains("5") should be {true}
			command.removals.get("4").contains("6") should be {true}
			command.removals.get("4").contains("7") should be {true}
		}
	}

	@Test
	def removeAndReAdd(): Unit = {
		new Fixture {
			// Remove
			command.studentToRemove = "1"
			command.entityToRemoveFrom = "2"
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.RemoveSingle
			val result: StudentAssociationResult = command.applyInternal()
			result.unallocated.size should be (5)
			result.allocated.size should be (4)
			result.allocated.flatMap(_.students).distinct.size should be (6)
			result.allocated.find(_.entityId == "2").get.students.size should be (1)
			command.removals.get("2").contains("1") should be {true}
			// Re-add
			command.allocate = Seq("1").asJava
			command.entities = Seq("2").asJava
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.DistributeSelected
			val result2: StudentAssociationResult = command.applyInternal()
			result2.unallocated.size should be (4)
			result2.allocated.size should be (4)
			result2.allocated.flatMap(_.students).distinct.size should be (7)
			result2.allocated.find(_.entityId == "2").get.students.size should be (2)
			command.removals.get("2").contains("1") should be {false}
			command.additions.get("2").contains("1") should be {false} // This is important, don't add already in the DB
		}
	}

	@Test
	def addAndReRemove(): Unit = {
		new Fixture {
			// Add
			command.allocate = JArrayList("8")
			command.entities = JArrayList("2")
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.DistributeSelected
			val result: StudentAssociationResult = command.applyInternal()
			result.unallocated.size should be (3)
			result.allocated.size should be (4)
			result.allocated.flatMap(_.students).distinct.size should be (8)
			result.allocated.find(_.entityId == "2").get.students.size should be (3)
			command.additions.get("2").contains("8") should be {true}
			// Re-remove
			command.studentToRemove = "8"
			command.entityToRemoveFrom = "2"
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.RemoveSingle
			val result2: StudentAssociationResult = command.applyInternal()
			result2.unallocated.size should be (4)
			result2.allocated.size should be (4)
			result2.allocated.flatMap(_.students).distinct.size should be (7)
			result2.allocated.find(_.entityId == "2").get.students.size should be (2)
			command.removals.get("2").contains("8") should be {false}
			command.additions.get("2").contains("8") should be {false} // This is important, don't remove not in the DB
		}
	}

	@Test
	def additionalAgents(): Unit = {
		new Fixture {
			command.additionalEntityUserIds = JArrayList("cusfal")
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.AddAdditionalEntities
			val result: StudentAssociationResult = command.applyInternal()
			result.unallocated.size should be (4)
			result.allocated.size should be (5)
		}
	}

}
