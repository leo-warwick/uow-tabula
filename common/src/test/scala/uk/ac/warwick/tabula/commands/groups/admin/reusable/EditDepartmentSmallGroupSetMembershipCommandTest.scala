package uk.ac.warwick.tabula.commands.groups.admin.reusable

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, StaffMember, StudentMember}
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.UserLookupComponent
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

class EditDepartmentSmallGroupSetMembershipCommandTest extends TestBase with Mockito {

	val userLookup = new MockUserLookup

	private trait CommandTestSupport extends UserLookupComponent {
		val userLookup: MockUserLookup = EditDepartmentSmallGroupSetMembershipCommandTest.this.userLookup
	}

	private trait Fixture {
		val department: Department = Fixtures.department("in", "IT Services")
		val set: DepartmentSmallGroupSet = Fixtures.departmentSmallGroupSet("My set")
		set.department = department

		val user1 = new User("cuscav")
		user1.setFoundUser(true)
		user1.setFirstName("Mathew")
		user1.setLastName("Mannion")
		user1.setWarwickId("0672089")

		val user2 = new User("cusebr")
		user2.setFoundUser(true)
		user2.setFirstName("Nick")
		user2.setLastName("Howes")
		user2.setWarwickId("0672088")

		val user3 = new User("cusfal")
		user3.setFoundUser(true)
		user3.setFirstName("Matthew")
		user3.setLastName("Jones")
		user3.setWarwickId("9293883")

		val user4 = new User("curef")
		user4.setFoundUser(true)
		user4.setFirstName("John")
		user4.setLastName("Dale")
		user4.setWarwickId("0200202")

		val user5 = new User("cusmab")
		user5.setFoundUser(true)
		user5.setFirstName("Steven")
		user5.setLastName("Carpenter")
		user5.setWarwickId("8888888")

		userLookup.users +=(
			user1.getUserId -> user1,
			user2.getUserId -> user2,
			user3.getUserId -> user3,
			user4.getUserId -> user4,
			user5.getUserId -> user5
		)
	}

	private trait CommandFixture extends Fixture {
		val command =
			new EditDepartmentSmallGroupSetMembershipCommandInternal(department, set)
				with CommandTestSupport
	}

	@Test def apply { new CommandFixture {
		command.includedStudentIds.add(user1.getWarwickId)
		command.includedStudentIds.add(user2.getWarwickId)
		command.excludedStudentIds.add(user3.getWarwickId)

		val result: EditDepartmentSmallGroupSetMembershipCommandResult = command.applyInternal()
		result.includedStudentIds.asScala should be (Seq(user1.getWarwickId, user2.getWarwickId))
		result.excludedStudentIds.asScala should be (Seq(user3.getWarwickId))
		result.membershipItems should be (Seq(
			DepartmentSmallGroupSetMembershipItem(DepartmentSmallGroupSetMembershipIncludeType, "Nick", "Howes", "0672088", "cusebr"),
			DepartmentSmallGroupSetMembershipItem(DepartmentSmallGroupSetMembershipExcludeType, "Matthew", "Jones", "9293883", "cusfal"),
			DepartmentSmallGroupSetMembershipItem(DepartmentSmallGroupSetMembershipIncludeType, "Mathew", "Mannion", "0672089", "cuscav")
		))
	}}

	private trait AddsCommandFixture extends Fixture {
		val command =
			new EditDepartmentSmallGroupSetMembershipCommandInternal(department, set)
				with AddsUsersToEditDepartmentSmallGroupSetMembershipCommand
				with CommandTestSupport
	}

	@Test def addUsers { new AddsCommandFixture {
		val validStudent: StudentMember = Fixtures.student("1111111","abcd")
		val validStudentWithUsercode: StudentMember = Fixtures.student("2222222","cdef")
		val invalidStaff: StaffMember = Fixtures.staff("3333333","abcd")
		val invalidNoone = "unknown"

		command.massAddUsers = "%s\n%s\n%s\n%s" format (
			validStudent.universityId,
			validStudentWithUsercode.userId,
			invalidStaff.universityId,
			invalidNoone
		)

		command.userLookup.registerUserObjects(MemberOrUser(validStudent).asUser)
		command.userLookup.registerUserObjects(MemberOrUser(validStudentWithUsercode).asUser)

		val result: AddUsersToEditDepartmentSmallGroupSetMembershipCommandResult = command.addUsers()
		result.missingUsers.size should be (2)
		result.missingUsers.contains(invalidNoone) should be {true}
		result.missingUsers.contains(invalidStaff.universityId) should be {true}
		command.includedStudentIds.size should be (2)
		command.includedStudentIds.contains(validStudent.universityId) should be {true}
		command.includedStudentIds.contains(validStudentWithUsercode.universityId) should be {true}
	}}

	@Test def permissions { new Fixture {
		val (theDepartment, theSet) = (department, set)
		val command = new EditDepartmentSmallGroupSetMembershipPermissions with EditDepartmentSmallGroupSetMembershipCommandState {
			val department: Department = theDepartment
			val set: DepartmentSmallGroupSet = theSet
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Update, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoDepartment {
		val command = new EditDepartmentSmallGroupSetMembershipPermissions with EditDepartmentSmallGroupSetMembershipCommandState {
			val department = null
			val set = new DepartmentSmallGroupSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoSet {
		val command = new EditDepartmentSmallGroupSetMembershipPermissions with EditDepartmentSmallGroupSetMembershipCommandState {
			val department: Department = Fixtures.department("in")
			val set = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsUnlinkedSet {
		val command = new EditDepartmentSmallGroupSetMembershipPermissions with EditDepartmentSmallGroupSetMembershipCommandState {
			val department: Department = Fixtures.department("in")
			department.id = "set id"

			val set = new DepartmentSmallGroupSet(Fixtures.department("other"))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test def wires { new Fixture {
		val command = EditDepartmentSmallGroupSetMembershipCommand(department, set)

		command should be (anInstanceOf[Appliable[EditDepartmentSmallGroupSetMembershipCommandResult]])
		command should be (anInstanceOf[EditDepartmentSmallGroupSetMembershipPermissions])
		command should be (anInstanceOf[EditDepartmentSmallGroupSetMembershipCommandState])
		command should be (anInstanceOf[PopulateOnForm])
		command should be (anInstanceOf[ReadOnly])
		command should be (anInstanceOf[Unaudited])
		command should be (anInstanceOf[AddsUsersToEditDepartmentSmallGroupSetMembershipCommand])
		command should be (anInstanceOf[RemovesUsersFromEditDepartmentSmallGroupSetMembershipCommand])
		command should be (anInstanceOf[ResetsMembershipInEditDepartmentSmallGroupSetMembershipCommand])
	}}

}
