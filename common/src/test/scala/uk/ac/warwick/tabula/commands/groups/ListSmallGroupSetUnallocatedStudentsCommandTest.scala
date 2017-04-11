package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember, UnspecifiedTypeUserGroup, UserGroup}
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, ProfileService, ProfileServiceComponent, UserGroupCacheManager}
import uk.ac.warwick.tabula.{CurrentUser, Fixtures, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class ListSmallGroupSetUnallocatedStudentsCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends ProfileServiceComponent {
		var profileService: ProfileService = mock[ProfileService]
	}

	def createUser(userId: String, firstName: String, lastName:String, warwickId:String): User = {
		val user = new User(userId)
		user.setFoundUser(true)
		user.setFirstName(firstName)
		user.setLastName(lastName)
		user.setWarwickId(warwickId)
		user
	}

	trait Fixture {
		val department: Department = Fixtures.department("in", "IT Services")
		val userLookup = new MockUserLookup
		val membershipService: AssessmentMembershipService = mock[AssessmentMembershipService]
		val profileService: ProfileService = mock[ProfileService]
		val set = new SmallGroupSet

		def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
			case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
			case ug: UserGroup => ug.userLookup = userLookup
		}

		val user1: User = createUser("cuscav", "Mathew", "Mannion", "0672089")
		val user2: User = createUser("cusebr", "Nick", "Howes", "0672088")
		val user3: User = createUser("cusfal", "Matthew", "Jones", "9293883")
		val user4: User = createUser("curef", "John", "Dale", "0200202")
		var user5: User = createUser("cusmab", "Steven", "Carpenter", "8888888")
		val user6: User = createUser("caaaaa", "Terence", "Trent D'arby", "6666666")
		val user7: User = createUser("cutrue", "Fabrice", "Morvan", "7777777")
		val user8: User = createUser("cugirl", "Robert", "Pilatus", "1111111")

		userLookup.users += (
			user1.getUserId -> user1,
			user2.getUserId -> user2,
			user3.getUserId -> user3,
			user4.getUserId -> user4,
			user5.getUserId -> user5,
			user6.getUserId -> user6,
			user7.getUserId -> user7,
			user8.getUserId -> user8
			)

		val group1 = new SmallGroup
		val group2 = new SmallGroup
		val group3 = new SmallGroup
		val group4 = new SmallGroup

		Seq(group1, group2, group3, group4).zipWithIndex.foreach {
			case(group, index) =>
				group.name = "Group " + index
				group.id = "abcdefgh" + index
				set.groups.add(group)
				group.groupSet = set
				wireUserLookup(group.students)
		}

		val student1: StudentMember = Fixtures.student(user1.getWarwickId, user1.getUserId, department)
		val student2: StudentMember = Fixtures.student(user2.getWarwickId, user2.getUserId, department)
		val student3: StudentMember = Fixtures.student(user3.getWarwickId, user3.getUserId, department)
		val student4: StudentMember = Fixtures.student(user4.getWarwickId, user4.getUserId, department)
		val student5: StudentMember = Fixtures.student(user5.getWarwickId, user5.getUserId, department)
		val student6: StudentMember = Fixtures.student(user6.getWarwickId, user6.getUserId, department)
		val student7: StudentMember = Fixtures.student(user7.getWarwickId, user7.getUserId, department)
		val student8: StudentMember = Fixtures.student(user8.getWarwickId, user8.getUserId, department)

		val allUsers = Seq(user1, user2, user3, user4, user5, user6, user7, user8)
		allUsers.foreach(set.members.add(_))

		val userToStudent =	Map(user1.getWarwickId -> student1,
			user2.getWarwickId -> student2,
			user3.getWarwickId -> student3,
			user4.getWarwickId -> student4,
			user5.getWarwickId -> student5,
			user6.getWarwickId -> student6,
			user7.getWarwickId -> student7,
			user8.getWarwickId -> student8
		)

		set.membershipService = membershipService
		wireUserLookup(set.members)
		membershipService.determineMembershipUsers(set.upstreamAssessmentGroups, Some(set.members)) returns (set.members.users)

		allUsers.foreach {
			user => profileService.getMemberByUniversityId(user.getWarwickId) returns Some(userToStudent(user.getWarwickId))
		}
	}

	@Test
	def emptyGroups() = withUser("snow") { new Fixture() {
		var command = new ListSmallGroupSetUnallocatedStudentsCommandInternal(set, currentUser) with CommandTestSupport
		command.profileService = profileService
		val info: UnallocatedStudentsInformation = command.applyInternal()

		info.smallGroupSet should be (set)
		info.membersNotInGroups.length should be (8)
		group1.students.add(user1)
	}
	}

	@Test
	def oneStudentInAGroup() = withUser("cutrue") { new Fixture() {
		group1.students.add(user1)

		var command = new ListSmallGroupSetUnallocatedStudentsCommandInternal(set, currentUser) with CommandTestSupport
		command.profileService = profileService
		val info: UnallocatedStudentsInformation = command.applyInternal()

		info.smallGroupSet should be (set)
		info.membersNotInGroups.length should be (7)
		info.membersNotInGroups.exists(_.universityId == user1.getWarwickId) should be (false)
	}
	}

	@Test
	def checkCurrentUserIsInGroup() = withUser("cutrue") { new Fixture() {
		val fakeCurrentUser = new CurrentUser(user8, user8)
		var command = new ListSmallGroupSetUnallocatedStudentsCommandInternal(set, fakeCurrentUser) with CommandTestSupport
		command.profileService = profileService

		val info: UnallocatedStudentsInformation = command.applyInternal()
		info.userIsMember should be (true)
	}
	}


	@Test
	def sevenStudentsInOneGroup() = withUser("cutrue") { new Fixture() {
		/* user8 should be unallocated */
		group1.students.add(user1)
		group1.students.add(user2)
		group1.students.add(user3)
		group1.students.add(user4)
		group1.students.add(user5)
		group1.students.add(user6)
		group1.students.add(user7)

		var command = new ListSmallGroupSetUnallocatedStudentsCommandInternal(set, currentUser) with CommandTestSupport
		command.profileService = profileService
		val info: UnallocatedStudentsInformation = command.applyInternal()

		info.membersNotInGroups.length should be (1)
		info.membersNotInGroups.exists(_.universityId == user8.getWarwickId) should be (true)
		info.membersNotInGroups.exists(_.universityId == user4.getWarwickId) should be (false)
	}
	}


	@Test
	def sixStudentsAllocatedAcrossGroups() = withUser("cutrue") { new Fixture() {
		/* only user7 and user8 are unallocated */
		group1.students.add(user1)
		group2.students.add(user2)
		group2.students.add(user3)
		group3.students.add(user3)
		group4.students.add(user4)
		group1.students.add(user5)
		group2.students.add(user6)

		var command = new ListSmallGroupSetUnallocatedStudentsCommandInternal(set, currentUser) with CommandTestSupport
		command.profileService = profileService
		val info: UnallocatedStudentsInformation = command.applyInternal()

		info.membersNotInGroups.length should be (2)
		info.membersNotInGroups.exists(_.universityId == user8.getWarwickId) should be (true)
		info.membersNotInGroups.exists(_.universityId == user7.getWarwickId) should be (true)
		info.membersNotInGroups.exists(_.universityId == user1.getWarwickId) should be (false)
	}
	}
}