package uk.ac.warwick.tabula.commands.groups.admin

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupAllocationMethod, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.{Department, Module, UnspecifiedTypeUserGroup, UserGroup}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class EditSmallGroupSetMembershipCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends SmallGroupServiceComponent with UserLookupComponent with AssessmentMembershipServiceComponent with RemovesUsersFromGroups {
		val smallGroupService: SmallGroupService = mock[SmallGroupService]
		val userLookup = new MockUserLookup
		var assessmentMembershipService: AssessmentMembershipService = mock[AssessmentMembershipService]

		def removeFromGroup(user: User, group: SmallGroup): Unit = group.students.remove(user)
	}

	private trait Fixture {
		val department = new Department()

		val module: Module = Fixtures.module("in101", "Introduction to Scala")
		module.id = "moduleId"
		module.adminDepartment = department

		val set = new SmallGroupSet(module)
		set.id = "existingId"
		set.name = "Existing set"
	}

	private trait CommandFixture extends Fixture {
		val command = new EditSmallGroupSetMembershipCommandInternal(module, set) with CommandTestSupport with SmallGroupAutoDeregistration with ModifiesSmallGroupSetMembership
	}

	@Test def autoDeregister { new CommandFixture {
		val (user1, user2, user3, user4, user5) = (new User("user1"), new User("user2"), new User("user3"), new User("user4"), new User("user5"))
		command.userLookup.registerUserObjects(user1, user2, user3, user4, user5)

		def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
			case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
			case ug: UserGroup => ug.userLookup = command.userLookup
		}

		set.members = UserGroup.ofUsercodes
		set.membershipService = mock[AssessmentMembershipService] // Intentionally different to the command's one

		set.members.add(user1)
		set.members.add(user2)
		set.members.add(user3)
		set.members.add(user4)

		val group1 = new SmallGroup()
		group1.students = UserGroup.ofUsercodes
		group1.groupSet = set
		group1.students.add(user1)
		group1.students.add(user2)

		val group2 = new SmallGroup()
		group2.students = UserGroup.ofUsercodes
		group2.groupSet = set
		group2.students.add(user3)
		group2.students.add(user4)
		group2.students.add(user5)

		set.groups.add(group1)
		set.groups.add(group2)

		command.members = UserGroup.ofUsercodes

		wireUserLookup(set.members)
		wireUserLookup(group1.students)
		wireUserLookup(group2.students)
		wireUserLookup(command.members)

		command.assessmentGroups = set.assessmentGroups
		command.members.copyFrom(set.members)
		command.academicYear = set.academicYear

		command.assessmentMembershipService.determineMembershipUsers(Seq(), Some(set.members)) returns (set.members.users)
		command.assessmentMembershipService.determineMembershipUsers(Seq(), Some(command.members)) returns (command.members.users)
		command.applyInternal()

		set.members.users.toSet should be (Set(user1, user2, user3, user4))
		group1.students.users.toSet should be (Set(user1, user2))
		group2.students.users.toSet should be (Set(user3, user4, user5))

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)

		command.members.remove(user2)
		command.members.remove(user3)
		command.members.remove(user4)
		command.members.add(user5)

		command.assessmentMembershipService = mock[AssessmentMembershipService] // Intentionally different to the SmallGroupSet's one
		command.assessmentMembershipService.determineMembershipUsers(Seq(), Some(set.members)) returns (set.members.users)
		command.assessmentMembershipService.determineMembershipUsers(Seq(), Some(command.members)) returns (command.members.users)
		command.applyInternal()

		set.members.users.toSet should be (Set(user1, user5))
		group1.students.users.toSet should be (Set(user1))
		group2.students.users.toSet should be (Set(user5))

		// Two now, because it includes the one from before
		verify(command.smallGroupService, times(2)).saveOrUpdate(set)
	}}

	private trait ValidationFixture extends Fixture {
		val command = new EditSmallGroupSetMembershipValidation with EditSmallGroupSetMembershipCommandState {
			val module: Module = ValidationFixture.this.module
			val set: SmallGroupSet = ValidationFixture.this.set
		}
	}

	@Test def validateLinked { new ValidationFixture {
		set.allocationMethod = SmallGroupAllocationMethod.Linked

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getGlobalError.getCodes should contain ("smallGroupSet.linked")
	}}

}