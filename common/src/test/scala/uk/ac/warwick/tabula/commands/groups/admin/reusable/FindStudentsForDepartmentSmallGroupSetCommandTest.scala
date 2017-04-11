package uk.ac.warwick.tabula.commands.groups.admin.reusable

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.data.model.{Department, Route, SitsStatus, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking

import scala.collection.JavaConverters._

class FindStudentsForDepartmentSmallGroupSetCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends ProfileServiceComponent with UserLookupComponent with FiltersStudents {
		val profileService: ProfileService = smartMock[ProfileService]
		val userLookup = new MockUserLookup
	}

	private trait Fixture {
		val department: Department = Fixtures.department("in", "IT Services")
		val set: DepartmentSmallGroupSet = Fixtures.departmentSmallGroupSet("my set")
		set.department = department
		val student1: StudentMember = Fixtures.student(universityId = "1234", userId = "1234")
		val student2: StudentMember = Fixtures.student(universityId = "2345", userId = "2345")
		val student3: StudentMember = Fixtures.student(universityId = "3456", userId = "3456")
	}

	private trait CommandFixture extends Fixture {
		val command = new FindStudentsForDepartmentSmallGroupSetCommandInternal(department, set) with CommandTestSupport
	}

	@Test	def apply() { new CommandFixture {
		command.routes.add(new Route(){ this.code = "a100" })

		command.profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(
			any[Department], any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]
		) returns Seq(student1.universityId, student2.universityId)

		command.userLookup.registerUserObjects(
			MemberOrUser(student1).asUser,
			MemberOrUser(student2).asUser,
			MemberOrUser(student3).asUser
		)

		command.includedStudentIds.add(student3.universityId)
		command.excludedStudentIds.add(student2.universityId)

		// Enable searching
		command.findStudents = "submit"

		val result: FindStudentsForDepartmentSmallGroupSetCommandResult = command.applyInternal()
		// 2 results from search, even with 1 removed
		result.membershipItems.size should be (2)
		// 1 marked static
		result.membershipItems.count(_.itemType == DepartmentSmallGroupSetMembershipStaticType) should be (1)
		// 1 marked removed
		result.membershipItems.count(_.itemType == DepartmentSmallGroupSetMembershipExcludeType) should be (1)
		// 0 marked included (not displayed if not in search)
		result.membershipItems.count(_.itemType == DepartmentSmallGroupSetMembershipIncludeType) should be (0)

		result.membershipItems.size should be (result.staticStudentIds.size)
		result.staticStudentIds.asScala should be (Seq(student1.universityId, student2.universityId))
	}}

	@Test def populate() { new Fixture {
		val (d, s) = (department, set)
		val command = new PopulateFindStudentsForDepartmentSmallGroupSetCommand with FindStudentsForDepartmentSmallGroupSetCommandState with FiltersStudents with DeserializesFilterImpl {
			val department: Department = d
			val set: DepartmentSmallGroupSet = s
			val profileService: ProfileService = smartMock[ProfileService]
			val sitsStatusDao: SitsStatusDao = smartMock[SitsStatusDao]
			val moduleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
			val courseAndRouteService: CourseAndRouteService = smartMock[CourseAndRouteService]
			val modeOfAttendanceDao: ModeOfAttendanceDao = smartMock[ModeOfAttendanceDao]
		}

		val currentStatus: SitsStatus = Fixtures.sitsStatus("C")
		val withdrawnStatus: SitsStatus = Fixtures.sitsStatus("P")

		set.members.knownType.staticUserIds = Seq("0000001", "0000002", "0000004")
		set.members.knownType.includedUserIds = Seq("0000003")
		set.members.knownType.excludedUserIds = Seq("0000004")
		set.memberQuery = "sprStatuses=C"

		command.profileService.allSprStatuses(department) returns (Seq(currentStatus, withdrawnStatus))
		command.sitsStatusDao.getByCode("C") returns (Some(currentStatus))

		command.populate()

		command.staticStudentIds.asScala should be (Seq("0000001", "0000002", "0000004"))
		command.includedStudentIds.asScala should be (Seq("0000003"))
		command.excludedStudentIds.asScala should be (Seq("0000004"))
		command.filterQueryString should be ("sprStatuses=C")
		command.linkToSits should be (true)
		command.sprStatuses.asScala should be (Seq(currentStatus))
	}}

	@Test def permissions() { new Fixture {
		val (theDepartment, theSet) = (department, set)
		val command = new FindStudentsForDepartmentSmallGroupSetPermissions with FindStudentsForDepartmentSmallGroupSetCommandState {
			val department: Department = theDepartment
			val set: DepartmentSmallGroupSet = theSet
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Update, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoDepartment() {
		val command = new FindStudentsForDepartmentSmallGroupSetPermissions with FindStudentsForDepartmentSmallGroupSetCommandState {
			val department = null
			val set = new DepartmentSmallGroupSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoSet() {
		val command = new FindStudentsForDepartmentSmallGroupSetPermissions with FindStudentsForDepartmentSmallGroupSetCommandState {
			val department: Department = Fixtures.department("in")
			val set = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsUnlinkedSet() {
		val command = new FindStudentsForDepartmentSmallGroupSetPermissions with FindStudentsForDepartmentSmallGroupSetCommandState {
			val department: Department = Fixtures.department("in")
			department.id = "set id"

			val set = new DepartmentSmallGroupSet(Fixtures.department("other"))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test def wires() { new Fixture {
		val command = FindStudentsForDepartmentSmallGroupSetCommand(department, set)

		command should be (anInstanceOf[Appliable[FindStudentsForDepartmentSmallGroupSetCommandResult]])
		command should be (anInstanceOf[FindStudentsForDepartmentSmallGroupSetPermissions])
		command should be (anInstanceOf[FindStudentsForDepartmentSmallGroupSetCommandState])
		command should be (anInstanceOf[PopulateOnForm])
		command should be (anInstanceOf[ReadOnly])
		command should be (anInstanceOf[Unaudited])
		command should be (anInstanceOf[UpdatesFindStudentsForDepartmentSmallGroupSetCommand])
	}}

}
