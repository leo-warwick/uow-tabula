package uk.ac.warwick.tabula.commands.groups.admin.reusable

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroup, DepartmentSmallGroupSet}
import uk.ac.warwick.tabula.data.model.{Department, UnspecifiedTypeUserGroup, UserGroup}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SmallGroupService, SmallGroupServiceComponent, UserGroupCacheManager, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

class UpdateStudentsForDepartmentSmallGroupSetCommandTest extends TestBase with Mockito {

	val userLookup = new MockUserLookup

	def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
		case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
		case ug: UserGroup => ug.userLookup = userLookup
	}

	private trait CommandTestSupport extends UserLookupComponent with SmallGroupServiceComponent with RemovesUsersFromDepartmentGroups {
		val userLookup: MockUserLookup = UpdateStudentsForDepartmentSmallGroupSetCommandTest.this.userLookup
		val smallGroupService: SmallGroupService = smartMock[SmallGroupService]

		def removeFromGroup(user: User, group: DepartmentSmallGroup): Unit = group.students.remove(user)
	}

	private trait Fixture {
		val department: Department = Fixtures.department("in", "IT Services")
		val set: DepartmentSmallGroupSet = Fixtures.departmentSmallGroupSet("Existing set")
		set.id = "existingId"
		set.department = department
		wireUserLookup(set.members)

		val user1 = new User("cuscav")
		user1.setFoundUser(true)
		user1.setFirstName("Mathew")
		user1.setLastName("Mannion")
		user1.setWarwickId("0000001")

		val user2 = new User("cusebr")
		user2.setFoundUser(true)
		user2.setFirstName("Nick")
		user2.setLastName("Howes")
		user2.setWarwickId("0000002")

		val user3 = new User("cusfal")
		user3.setFoundUser(true)
		user3.setFirstName("Matthew")
		user3.setLastName("Jones")
		user3.setWarwickId("0000003")

		val user4 = new User("curef")
		user4.setFoundUser(true)
		user4.setFirstName("John")
		user4.setLastName("Dale")
		user4.setWarwickId("0000004")

		val user5 = new User("cusmab")
		user5.setFoundUser(true)
		user5.setFirstName("Steven")
		user5.setLastName("Carpenter")
		user5.setWarwickId("0000005")

		userLookup.users +=(
			user1.getUserId -> user1,
			user2.getUserId -> user2,
			user3.getUserId -> user3,
			user4.getUserId -> user4,
			user5.getUserId -> user5
		)
	}

	private trait CommandFixture extends Fixture {
		val command = new UpdateStudentsForDepartmentSmallGroupSetCommandInternal(department, set) with CommandTestSupport
	}

	@Test def linkToSits { new CommandFixture {
		department.autoGroupDeregistration = false

		command.staticStudentIds.addAll(Seq("0000001", "0000002", "0000004").asJavaCollection)
		command.includedStudentIds.addAll(Seq("0000003").asJavaCollection)
		command.excludedStudentIds.addAll(Seq("0000004").asJavaCollection)
		command.filterQueryString = "sprStatuses=C"

		command.linkToSits = true

		command.applyInternal() should be (set)

		set.members.knownType.staticUserIds should be (Seq("0000001", "0000002", "0000004"))
		set.members.knownType.includedUserIds should be (Seq("0000003"))
		set.members.knownType.excludedUserIds should be (Seq("0000004"))
		set.memberQuery should be ("sprStatuses=C")

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def dontLinkToSits { new CommandFixture {
		department.autoGroupDeregistration = false

		command.staticStudentIds.addAll(Seq("0000001", "0000002", "0000004").asJavaCollection)
		command.includedStudentIds.addAll(Seq("0000003").asJavaCollection)
		command.excludedStudentIds.addAll(Seq("0000004").asJavaCollection)
		command.filterQueryString = "sprStatuses=C"

		command.linkToSits = false

		command.applyInternal() should be (set)

		set.members.knownType.staticUserIds should be ('empty)
		set.members.knownType.includedUserIds should be (Seq("0000001", "0000002", "0000003"))
		set.members.knownType.excludedUserIds should be ('empty)
		set.memberQuery should be ('empty)

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def autoDeregistration { new CommandFixture {
		department.autoGroupDeregistration = true

		set.members.knownType.includedUserIds = Seq("0000001", "0000002", "0000003", "0000004")
		command.includedStudentIds.addAll(Seq("0000001", "0000002", "0000003").asJavaCollection)

		command.linkToSits = false

		val groupA: DepartmentSmallGroup = Fixtures.departmentSmallGroup("Group A")
		groupA.groupSet = set
		set.groups.add(groupA)
		wireUserLookup(groupA.students)
		groupA.students.knownType.includedUserIds = Seq("0000001", "0000002")

		val groupB: DepartmentSmallGroup = Fixtures.departmentSmallGroup("Group B")
		groupB.groupSet = set
		set.groups.add(groupB)
		wireUserLookup(groupB.students)
		groupB.students.knownType.includedUserIds = Seq("0000003", "0000004")

		command.applyInternal() should be (set)

		set.members.knownType.staticUserIds should be ('empty)
		set.members.knownType.includedUserIds should be (Seq("0000001", "0000002", "0000003"))
		set.members.knownType.excludedUserIds should be ('empty)
		set.memberQuery should be ('empty)

		groupA.students.knownType.includedUserIds should be (Seq("0000001", "0000002"))
		groupB.students.knownType.includedUserIds should be (Seq("0000003")) // 4 has been removed

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def permissions { new Fixture {
		val (theDepartment, theSet) = (department, set)
		val command = new UpdateStudentsForDepartmentSmallGroupSetPermissions with UpdateStudentsForDepartmentSmallGroupSetCommandState with CommandTestSupport {
			val department: Department = theDepartment
			val set: DepartmentSmallGroupSet = theSet
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Update, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoDepartment {
		val command = new UpdateStudentsForDepartmentSmallGroupSetPermissions with UpdateStudentsForDepartmentSmallGroupSetCommandState with CommandTestSupport {
			val department = null
			val set = new DepartmentSmallGroupSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoSet {
		val command = new UpdateStudentsForDepartmentSmallGroupSetPermissions with UpdateStudentsForDepartmentSmallGroupSetCommandState with CommandTestSupport {
			val department: Department = Fixtures.department("in")
			val set = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsUnlinkedSet {
		val command = new UpdateStudentsForDepartmentSmallGroupSetPermissions with UpdateStudentsForDepartmentSmallGroupSetCommandState with CommandTestSupport {
			val department: Department = Fixtures.department("in")
			department.id = "set id"

			val set = new DepartmentSmallGroupSet(Fixtures.department("other"))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test def describe { new Fixture {
		val (dept, s) = (department, set)
		val command = new UpdateStudentsForDepartmentSmallGroupSetDescription with UpdateStudentsForDepartmentSmallGroupSetCommandState with CommandTestSupport {
			override lazy val eventName = "test"
			val department: Department = dept
			val set: DepartmentSmallGroupSet = s
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"smallGroupSet" -> "existingId"
		))
	}}

	@Test def wires { new Fixture {
		val command = UpdateStudentsForDepartmentSmallGroupSetCommand(department, set)

		command should be (anInstanceOf[Appliable[DepartmentSmallGroupSet]])
		command should be (anInstanceOf[Describable[DepartmentSmallGroupSet]])
		command should be (anInstanceOf[UpdateStudentsForDepartmentSmallGroupSetPermissions])
		command should be (anInstanceOf[UpdateStudentsForDepartmentSmallGroupSetCommandState])
	}}

}
