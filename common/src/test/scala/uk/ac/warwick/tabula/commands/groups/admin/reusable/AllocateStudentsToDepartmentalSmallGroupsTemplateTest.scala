package uk.ac.warwick.tabula.commands.groups.admin.reusable

import org.apache.poi.xssf.usermodel.{XSSFRow, XSSFSheet, XSSFWorkbook}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.{Appliable, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroup, DepartmentSmallGroupSet}
import uk.ac.warwick.tabula.data.model.{Department, UnspecifiedTypeUserGroup, UserGroup}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{ProfileService, ProfileServiceComponent, UserGroupCacheManager}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.web.views.ExcelView
import uk.ac.warwick.userlookup.User

class AllocateStudentsToDepartmentalSmallGroupsTemplateTest extends TestBase with Mockito {

	private trait Fixture {
		val userLookup = new MockUserLookup

		def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
			case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
			case ug: UserGroup => ug.userLookup = userLookup
		}

		val department: Department = Fixtures.department("in", "IT Services")
		val set: DepartmentSmallGroupSet = Fixtures.departmentSmallGroupSet("My small groups")

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

		userLookup.users += (
			user1.getUserId -> user1,
			user2.getUserId -> user2,
			user3.getUserId -> user3,
			user4.getUserId -> user4,
			user5.getUserId -> user5
			)

		val group1: DepartmentSmallGroup = Fixtures.departmentSmallGroup("Group 1")
		val group2: DepartmentSmallGroup = Fixtures.departmentSmallGroup("Group 2")
		val group3: DepartmentSmallGroup = Fixtures.departmentSmallGroup("Group 3")
		val group4: DepartmentSmallGroup = Fixtures.departmentSmallGroup("Group 4")

		group1.name = "Group 1"
		group1.id = "abcdefgh1"
		group2.name = "Group 2"
		group2.id = "abcdefgh2"
		group3.name = "Group 3"
		group3.id = "abcdefgh3"
		group4.name = "Group 4"
		group4.id = "abcdefgh4"

		set.groups.add(group1)
		set.groups.add(group2)
		set.groups.add(group3)
		set.groups.add(group4)
		group1.groupSet = set
		group2.groupSet = set
		group3.groupSet = set
		group4.groupSet = set
		wireUserLookup(group1.students)
		wireUserLookup(group2.students)
		wireUserLookup(group3.students)
		wireUserLookup(group4.students)

		set.members.add(user1)
		set.members.add(user2)
		set.members.add(user3)
		set.members.add(user4)
		set.members.add(user5)

		set.department = department
		wireUserLookup(set.members)
	}

	private trait CommandFixture extends Fixture {
		val command = new AllocateStudentsToDepartmentalSmallGroupsTemplateCommandInternal(department, set) with ProfileServiceComponent {
			val profileService: ProfileService = smartMock[ProfileService]
		}
	}

	@Test def allocateUsersSheet { new CommandFixture {
		implicit class SearchableSheet(self:XSSFSheet) {
			def containsDataRow(id:String, name:String, maxRows:Int = self.getLastRowNum):Boolean = {
				val rows = for (i<- 1 to maxRows) yield self.getRow(i)
				val matchingRow = rows.find(r=>r.getCell(0).toString == id && r.getCell(1).toString == name)
				matchingRow.isDefined
			}
		}

		val workbook: XSSFWorkbook = command.generateWorkbook()

		val allocateSheet: XSSFSheet = workbook.getSheet(command.allocateSheetName)

		val headerRow: XSSFRow = allocateSheet.getRow(0)
		headerRow.getCell(0).toString should be ("student_id")
		headerRow.getCell(1).toString should be ("Student name")
		headerRow.getCell(2).toString should be ("Group name")
		headerRow.getCell(3).toString should be ("group_id")

		allocateSheet.containsDataRow("0672089","Mathew Mannion", maxRows = 6) should be(true)
		allocateSheet.containsDataRow("0672088","Nick Howes", maxRows = 6) should be(true)
		allocateSheet.containsDataRow("8888888","Steven Carpenter", maxRows = 6) should be(true)
		allocateSheet.containsDataRow("9293883","Matthew Jones", maxRows = 6) should be(true)
		allocateSheet.containsDataRow("0200202","John Dale", maxRows = 6) should be(true)
	}}

	@Test def groupLookupSheet { new CommandFixture {
		val workbook: XSSFWorkbook = command.generateWorkbook()

		val groupLookupSheet: XSSFSheet = workbook.getSheet(command.groupLookupSheetName)

		var groupRow: XSSFRow = groupLookupSheet.getRow(1)
		groupRow.getCell(0).toString should be ("Group 1")
		groupRow.getCell(1).toString should be ("abcdefgh1")

		groupRow = groupLookupSheet.getRow(2)
		groupRow.getCell(0).toString should be ("Group 2")
		groupRow.getCell(1).toString should be ("abcdefgh2")

		groupRow = groupLookupSheet.getRow(3)
		groupRow.getCell(0).toString should be ("Group 3")
		groupRow.getCell(1).toString should be ("abcdefgh3")

		groupRow = groupLookupSheet.getRow(4)
		groupRow.getCell(0).toString should be ("Group 4")
		groupRow.getCell(1).toString should be ("abcdefgh4")
	}}

	@Test def checkExcelView { new CommandFixture {
		val excelDownload: ExcelView = command.applyInternal()

		excelDownload.getContentType() should be ("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
	}}

	@Test def permissions { new Fixture {
		val (theDepartment, theSet) = (department, set)
		val command = new AllocateStudentsToDepartmentalSmallGroupsTemplatePermissions with AllocateStudentsToDepartmentalSmallGroupsTemplateCommandState {
			val department: Department = theDepartment
			val set: DepartmentSmallGroupSet = theSet
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Allocate, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoDepartment {
		val command = new AllocateStudentsToDepartmentalSmallGroupsTemplatePermissions with AllocateStudentsToDepartmentalSmallGroupsTemplateCommandState {
			val department = null
			val set = new DepartmentSmallGroupSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoSet {
		val command = new AllocateStudentsToDepartmentalSmallGroupsTemplatePermissions with AllocateStudentsToDepartmentalSmallGroupsTemplateCommandState {
			val department: Department = Fixtures.department("in")
			val set = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsUnlinkedSet {
		val command = new AllocateStudentsToDepartmentalSmallGroupsTemplatePermissions with AllocateStudentsToDepartmentalSmallGroupsTemplateCommandState {
			val department: Department = Fixtures.department("in")
			department.id = "set id"

			val set = new DepartmentSmallGroupSet(Fixtures.department("other"))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test def wires { new Fixture {
		val command = AllocateStudentsToDepartmentalSmallGroupsTemplateCommand(department, set)

		command should be (anInstanceOf[Appliable[ExcelView]])
		command should be (anInstanceOf[AllocateStudentsToDepartmentalSmallGroupsTemplatePermissions])
		command should be (anInstanceOf[AllocateStudentsToDepartmentalSmallGroupsTemplateCommandState])
		command should be (anInstanceOf[ReadOnly])
		command should be (anInstanceOf[Unaudited])
	}}

}
