package uk.ac.warwick.tabula.commands.groups.admin.reusable

import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SmallGroupService, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.Department

class ListDepartmentSmallGroupSetsCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends SmallGroupServiceComponent {
		val smallGroupService: SmallGroupService = smartMock[SmallGroupService]
	}

	private trait Fixture {
		val department: Department = Fixtures.department("in", "IT Services")
		val academicYear = AcademicYear(2015)

		val set1 = new DepartmentSmallGroupSet(department)
		set1.name = "Set 1"

		val set2 = new DepartmentSmallGroupSet(department)
		set2.name = "Set 2"
	}

	private trait CommandFixture extends Fixture {
		val command = new ListDepartmentSmallGroupSetsCommandInternal(department, academicYear) with CommandTestSupport
	}

	@Test def apply() { new CommandFixture {
		command.smallGroupService.getDepartmentSmallGroupSets(department, academicYear) returns Seq(set1, set2)

		command.applyInternal() should be (Seq(set1, set2))
	}}

	@Test def permissions() {
		val command = new ListDepartmentSmallGroupSetsPermissions with ListDepartmentSmallGroupSetsCommandState {
			override val department: Department = Fixtures.department("in")
			override val academicYear = null
		}

		val checking = smartMock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Create, command.department)
	}

	@Test(expected = classOf[ItemNotFoundException]) def noDepartment() {
		val command = new ListDepartmentSmallGroupSetsPermissions with ListDepartmentSmallGroupSetsCommandState {
			override val department = null
			override val academicYear = null
		}

		val checking = smartMock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test def wiresTogether() { new Fixture {
		val command = ListDepartmentSmallGroupSetsCommand(department, academicYear)

		command should be (anInstanceOf[Appliable[Seq[DepartmentSmallGroupSet]]])
		command should be (anInstanceOf[ListDepartmentSmallGroupSetsPermissions])
		command should be (anInstanceOf[ListDepartmentSmallGroupSetsCommandState])
	}}

}
