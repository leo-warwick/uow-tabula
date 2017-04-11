package uk.ac.warwick.tabula.commands.profiles.relationships

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Tap.tap
import uk.ac.warwick.tabula.services.{RelationshipService, RelationshipServiceComponent, SortableAgentIdentifier}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

import scala.collection.immutable.TreeMap

class ViewStudentRelationshipsCommandTest extends TestBase with Mockito {

	private trait Fixture {
		val mockRelationshipService: RelationshipService =  smartMock[RelationshipService]

		val departmentOfXXX: Department = new Department().tap(_.code = "xxx")
		val tutorRelType: StudentRelationshipType = new StudentRelationshipType().tap(_.description = "tutor")

		// give them both the same surname, just to prove that we don't accidentally merge users when sorting
		val staff1: StaffMember = new StaffMember(id = "111").tap(_.lastName = "Smith")
		val staff2: StaffMember = new StaffMember(id = "222").tap(_.lastName = "Smith")

		val student1: StudentMember = Fixtures.student(universityId = "1")
		val student2: StudentMember = Fixtures.student(universityId = "2")
		val student3: StudentMember = Fixtures.student(universityId = "3")

		val studentRel1: MemberStudentRelationship = new MemberStudentRelationship().tap(r => {
			r.id = "1"
			r.agentMember = staff1
			r.studentMember = student1
			r.startDate = DateTime.now
		})

		val studentRel2: MemberStudentRelationship = new MemberStudentRelationship().tap(r => {
			r.id = "2"
			r.agentMember = staff2
			r.studentMember = student2
			r.startDate = DateTime.now
		})
	}

	@Test //thisTestHasARidculouslyLongNameButICantThinkOfASensibleWayToShortenItWhichProbablyMeansTheCommandNeedsRefactoring...
	def applyCombinesStudentsInDepartmentWithStudentsWhoHaveTutorsInDepartment() {
		new Fixture {
			mockRelationshipService.listAgentRelationshipsByDepartment(tutorRelType, departmentOfXXX) returns TreeMap(
				SortableAgentIdentifier(studentRel1) -> Seq(studentRel1),
				SortableAgentIdentifier(studentRel2) -> Seq(studentRel2)
			)(SortableAgentIdentifier.KeyOrdering)

			mockRelationshipService.listStudentsWithoutCurrentRelationship(tutorRelType, departmentOfXXX) returns Seq(student3)
			mockRelationshipService.listScheduledRelationshipChanges(tutorRelType, departmentOfXXX) returns Seq()

			private val command = new ViewStudentRelationshipsCommandInternal(departmentOfXXX, tutorRelType) with RelationshipServiceComponent {
				override def relationshipService: RelationshipService = mockRelationshipService
			}

			//When I invoke the command
			private val relationshipInfo = command.applyInternal()

			//Then I should get the results I expect
			relationshipInfo.studentMap(SortableAgentIdentifier("111",Some("Smith"))) should be(Seq(studentRel1))
			relationshipInfo.studentMap(SortableAgentIdentifier("222",Some("Smith"))) should be(Seq(studentRel2))
			relationshipInfo.missingCount should be(1) // the other one
		}
	}

}
