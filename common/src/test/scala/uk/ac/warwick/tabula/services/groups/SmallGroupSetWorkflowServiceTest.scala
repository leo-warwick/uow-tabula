package uk.ac.warwick.tabula.services.groups

import uk.ac.warwick.tabula.WorkflowStageHealth._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.{Department, Module, UnspecifiedTypeUserGroup, UserGroup}
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, UserGroupCacheManager}

import scala.collection.immutable.ListMap

// scalastyle:off magic.number
class SmallGroupSetWorkflowServiceTest extends TestBase with Mockito {

	val department: Department = Fixtures.department("in", "IT Services")
	val module: Module = Fixtures.module("in101", "Introduction to Web Development")
	val set: SmallGroupSet = Fixtures.smallGroupSet("IN101 Seminars")
	set.module = module
	module.adminDepartment = department

	val service = new SmallGroupSetWorkflowService
	val userLookup = new MockUserLookup

	private def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
		case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
		case ug: UserGroup => ug.userLookup = userLookup
	}

	@Test def stagesForAssignment() {
		import SmallGroupSetWorkflowStages._

		set.allocationMethod = SmallGroupAllocationMethod.Random

		// Random allocation stages
		service.getStagesFor(set) should be (Seq(
			AddGroups, AddStudents, AddEvents, SendNotifications
		))

		set.allocationMethod = SmallGroupAllocationMethod.Manual

		// Manual allocation stages
		service.getStagesFor(set) should be (Seq(
			AddGroups, AddStudents, AddEvents, AllocateStudents, SendNotifications
		))

		set.allocationMethod = SmallGroupAllocationMethod.StudentSignUp

		// Self sign-up allocation stages
		service.getStagesFor(set) should be (Seq(
			AddGroups, AddStudents, AddEvents, OpenSignUp, CloseSignUp, SendNotifications
		))

		set.allocationMethod = SmallGroupAllocationMethod.Linked
		set.linkedDepartmentSmallGroupSet = Fixtures.departmentSmallGroupSet("UG Y1")

		// Linked allocation stages
		service.getStagesFor(set) should be (Seq(
			AddGroups, AddStudents, AddEvents, SendNotifications
		))
	}

	@Test def progress() {
		// Start with a very basic set, using random allocation.
		set.allocationMethod = SmallGroupAllocationMethod.Random

		// Start with no students
		set.membershipService = mock[AssessmentMembershipService]
		set.membershipService.countMembershipWithUniversityIdGroup(set.upstreamAssessmentGroups, Some(set.members)) returns (0)

		// lines were getting a bit long...
		import SmallGroupSetWorkflowStages._
		import WorkflowStages._

		{
			val p = service.progress(set)
			p.stages should be (ListMap(
				"AddGroups" -> StageProgress(AddGroups, started = false, "workflow.smallGroupSet.AddGroups.empty", Danger, completed = false, preconditionsMet = true),
				"AddStudents" -> StageProgress(AddStudents, started = false, "workflow.smallGroupSet.AddStudents.empty", Danger, completed = false, preconditionsMet = true),
				"AddEvents" -> StageProgress(AddEvents, started = false, "workflow.smallGroupSet.AddEvents.empty", Danger, completed = false, preconditionsMet = false),
				"SendNotifications" -> StageProgress(SendNotifications, started = false, "workflow.smallGroupSet.SendNotifications.notSent", Warning, completed = false, preconditionsMet = false)
			))
			p.percentage should be (0)
			p.nextStage should be (Some(AddGroups))
			p.messageCode should be ("workflow.smallGroupSet.AddGroups.empty")
			p.cssClass should be ("danger")
		}

		wireUserLookup(set.members)

		// Add some groups
		val group1 = Fixtures.smallGroup("Group 1")
		wireUserLookup(group1.students)

		val group2 = Fixtures.smallGroup("Group 2")
		wireUserLookup(group2.students)

		set.groups.add(group1)
		set.groups.add(group2)

		{
			val p = service.progress(set)
			p.stages should be (ListMap(
				"AddGroups" -> StageProgress(AddGroups, started = true, "workflow.smallGroupSet.AddGroups.added", Warning, completed = true, preconditionsMet = true),
				"AddStudents" -> StageProgress(AddStudents, started = false, "workflow.smallGroupSet.AddStudents.empty", Danger, completed = false, preconditionsMet = true),
				"AddEvents" -> StageProgress(AddEvents, started = false, "workflow.smallGroupSet.AddEvents.empty", Danger, completed = false, preconditionsMet = true),
				"SendNotifications" -> StageProgress(SendNotifications, started = false, "workflow.smallGroupSet.SendNotifications.notSent", Warning, completed = false, preconditionsMet = false)
			))
			p.percentage should be (25)
			p.nextStage should be (Some(AddStudents))
			p.messageCode should be ("workflow.smallGroupSet.AddGroups.added")
			p.cssClass should be ("warning")
		}

		// Add some students
		set.membershipService = mock[AssessmentMembershipService]
		set.membershipService.countMembershipWithUniversityIdGroup(set.upstreamAssessmentGroups, Some(set.members)) returns (2)

		{
			val p = service.progress(set)
			p.stages should be (ListMap(
				"AddGroups" -> StageProgress(AddGroups, started = true, "workflow.smallGroupSet.AddGroups.added", Warning, completed = true, preconditionsMet = true),
				"AddStudents" -> StageProgress(AddStudents, started = true, "workflow.smallGroupSet.AddStudents.hasStudents", Warning, completed = true, preconditionsMet = true),
				"AddEvents" -> StageProgress(AddEvents, started = false, "workflow.smallGroupSet.AddEvents.empty", Danger, completed = false, preconditionsMet = true),
				"SendNotifications" -> StageProgress(SendNotifications, started = false, "workflow.smallGroupSet.SendNotifications.notSent", Warning, completed = false, preconditionsMet = false)
			))
			p.percentage should be (50)
			p.nextStage should be (Some(AddEvents))
			p.messageCode should be ("workflow.smallGroupSet.AddStudents.hasStudents")
			p.cssClass should be ("warning")
		}

		// Add some events
		val event1 = Fixtures.smallGroupEvent("Event 1")
		val event2 = Fixtures.smallGroupEvent("Event 2")

		group1.addEvent(event1)
		group2.addEvent(event2)

		{
			val p = service.progress(set)
			p.stages should be (ListMap(
				"AddGroups" -> StageProgress(AddGroups, started = true, "workflow.smallGroupSet.AddGroups.added", Warning, completed = true, preconditionsMet = true),
				"AddStudents" -> StageProgress(AddStudents, started = true, "workflow.smallGroupSet.AddStudents.hasStudents", Warning, completed = true, preconditionsMet = true),
				"AddEvents" -> StageProgress(AddEvents, started = true, "workflow.smallGroupSet.AddEvents.added", Warning, completed = true, preconditionsMet = true),
				"SendNotifications" -> StageProgress(SendNotifications, started = false, "workflow.smallGroupSet.SendNotifications.notSent", Warning, completed = false, preconditionsMet = true)
			))
			p.percentage should be (75)
			p.nextStage should be (Some(SendNotifications))
			p.messageCode should be ("workflow.smallGroupSet.AddEvents.added")
			p.cssClass should be ("warning")
		}

		// Release
		set.releasedToStudents = true
		set.releasedToTutors = true

		userLookup.registerUsers("user1", "user2", "user3")
		val user1 = userLookup.getUserByUserId("user1")
		val user2 = userLookup.getUserByUserId("user2")
		val user3 = userLookup.getUserByUserId("user3")

		set.membershipService.determineMembershipUsers(set.upstreamAssessmentGroups, Some(set.members)) returns (Seq(user1, user2, user3))
		set.membershipService.determineMembershipIds(set.upstreamAssessmentGroups, Some(set.members)) returns (Seq(user1.getWarwickId, user2.getWarwickId, user3.getWarwickId))
		group1.students.add(user1)
		group2.students.add(user2)

		{
			val p = service.progress(set)
			p.stages should be (ListMap(
				"AddGroups" -> StageProgress(AddGroups, started = true, "workflow.smallGroupSet.AddGroups.added", Warning, completed = true, preconditionsMet = true),
				"AddStudents" -> StageProgress(AddStudents, started = true, "workflow.smallGroupSet.AddStudents.hasStudents", Warning, completed = true, preconditionsMet = true),
				"AddEvents" -> StageProgress(AddEvents, started = true, "workflow.smallGroupSet.AddEvents.added", Warning, completed = true, preconditionsMet = true),
				"SendNotifications" -> StageProgress(SendNotifications, started = true, "workflow.smallGroupSet.SendNotifications.fullyReleased", Good, completed = true, preconditionsMet = true),
				"AllocateAfterNotifications" -> StageProgress(AllocateAfterNotifications, started = true, "workflow.smallGroupSet.SendNotifications.fullyReleased", Warning, completed = false, preconditionsMet = true)
			))
			p.percentage should be (100)
			p.nextStage should be (Some(AllocateAfterNotifications))
			p.messageCode should be ("workflow.smallGroupSet.SendNotifications.fullyReleased")
			p.cssClass should be ("warning")
		}

		group1.students.add(user3)

		{
			val p = service.progress(set)
			p.stages should be (ListMap(
				"AddGroups" -> StageProgress(AddGroups, started = true, "workflow.smallGroupSet.AddGroups.added", Warning, completed = true, preconditionsMet = true),
				"AddStudents" -> StageProgress(AddStudents, started = true, "workflow.smallGroupSet.AddStudents.hasStudents", Warning, completed = true, preconditionsMet = true),
				"AddEvents" -> StageProgress(AddEvents, started = true, "workflow.smallGroupSet.AddEvents.added", Warning, completed = true, preconditionsMet = true),
				"SendNotifications" -> StageProgress(SendNotifications, started = true, "workflow.smallGroupSet.SendNotifications.fullyReleased", Good, completed = true, preconditionsMet = true),
				"AllocateAfterNotifications" -> StageProgress(AllocateAfterNotifications, started = true, "workflow.smallGroupSet.SendNotifications.fullyReleased", Good, completed = true, preconditionsMet = true)
			))
			p.percentage should be (100)
			p.nextStage should be (None)
			p.messageCode should be ("workflow.smallGroupSet.SendNotifications.fullyReleased")
			p.cssClass should be ("success")
		}

		// TODO fuller example would be nice
	}

}