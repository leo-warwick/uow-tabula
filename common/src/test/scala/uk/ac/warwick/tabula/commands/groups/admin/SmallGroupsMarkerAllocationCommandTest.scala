package uk.ac.warwick.tabula.commands.groups.admin

import org.mockito.Mockito._
import uk.ac.warwick.tabula.data.model.{Assignment, Module, SeenSecondMarkingWorkflow, UserGroup}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

class SmallGroupsMarkerAllocationCommandTest extends TestBase with Mockito {

	@Test
	def getsAllocations() { new Fixture {
		val cmd = new SmallGroupsMarkerAllocationCommandInternal(assignment) with MockServices
		val allocations: Seq[SetAllocation] = cmd.applyInternal()
		allocations.size should be (1)
		val allocation: SetAllocation = allocations.head
		allocation.set should be (smallGroupSetA)
		val fg: Seq[GroupAllocation] = allocation.firstMarkerGroups
		fg.size should be (2)
		val a1: GroupAllocation = fg.filter(_.name == "A1").head
		a1.tutors should be (Seq(marker1))
		a1.students should be (Seq(student1, student2))
		val a2: GroupAllocation = fg.filter(_.name == "A2").head
		a2.tutors should be (Seq(marker3))
		a2.students should be (Seq(student3)) // student 4 is not on the assignment
	}}

	trait Fixture {

		val marker1: User = Fixtures.user("marker1", "marker1")
		val marker2: User = Fixtures.user("marker2", "marker2")
		val marker3: User = Fixtures.user("marker3", "marker3")
		val marker4: User = Fixtures.user("marker4", "marker4")
		val student1: User = Fixtures.user("student1", "student1")
		val student2: User = Fixtures.user("student2", "student2")
		val student3: User = Fixtures.user("student3", "student3")
		val student4: User = Fixtures.user("student4", "student4")
		val users = Map(
			"marker1" -> marker1,
			"marker2" -> marker2,
			"marker3" -> marker3,
			"marker4" -> marker4,
			"student1" -> student1,
			"student2" -> student2,
			"student3" -> student3,
			"student4" -> student4
		)

		val mockUserLookup: UserLookupService = mock[UserLookupService]

		def userGroup(usercodes: String*): UserGroup = {
			val userGroup = UserGroup.ofUsercodes
			userGroup.userLookup = mockUserLookup
			val map = usercodes.map(u => u -> users(u)).toMap.asJava
			when(mockUserLookup.getUsersByUserIds(usercodes.asJava)) thenReturn map
			userGroup.includedUserIds = usercodes
			userGroup
		}

		val ug1: UserGroup = userGroup("marker1", "marker3")
		val ug2: UserGroup = userGroup("marker2", "marker4")
		val ug3: UserGroup = userGroup("marker1", "marker2")
		val ug4: UserGroup = userGroup("marker3", "marker4")
		val ug5: UserGroup = userGroup("student1", "student2")
		val ug6: UserGroup = userGroup("student3", "student4")

		val module: Module = Fixtures.module("hz101")
		val assignment: Assignment = Fixtures.assignment("Herons are foul")
		assignment.module = module
		assignment.academicYear = AcademicYear(2014)
		val workflow: SeenSecondMarkingWorkflow = Fixtures.seenSecondMarkingWorkflow("wflow")
		workflow.firstMarkers = ug1
		workflow.secondMarkers = ug2
		assignment.markingWorkflow = workflow

		val smallGroupSetA: SmallGroupSet = Fixtures.smallGroupSet("A")
		val smallGroupA1: SmallGroup = Fixtures.smallGroup("A1")
		smallGroupA1.groupSet = smallGroupSetA
		smallGroupA1.students = ug5
		smallGroupA1.addEvent({
			val event = Fixtures.smallGroupEvent("event A1")
			event.tutors = ug3
			event
		})
		smallGroupSetA.groups.add(smallGroupA1)

		val smallGroupA2: SmallGroup = Fixtures.smallGroup("A2")
		smallGroupA2.groupSet = smallGroupSetA
		smallGroupA2.students = ug6
		smallGroupA2.addEvent({
			val event = Fixtures.smallGroupEvent("event A2")
			event.tutors = ug4
			event
		})
		smallGroupSetA.groups.add(smallGroupA2)


		trait MockServices extends SmallGroupServiceComponent with AssessmentMembershipServiceComponent {
			def smallGroupService: SmallGroupService = {
				val groupsService = mock[SmallGroupService]
				groupsService.getSmallGroupSets(assignment.module, assignment.academicYear) returns Seq(smallGroupSetA)
				groupsService
			}

			def assessmentMembershipService: AssessmentMembershipService = {
				val membershipService = mock[AssessmentMembershipService]
				membershipService.determineMembershipUsers(assignment) returns Seq(student1, student2, student3)
				membershipService
			}
		}

	}

}