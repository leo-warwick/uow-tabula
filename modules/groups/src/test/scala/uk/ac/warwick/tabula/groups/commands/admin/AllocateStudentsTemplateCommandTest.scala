package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.{Unaudited, ReadOnly, Appliable}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, UserGroupCacheManager}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.web.views.ExcelView
import uk.ac.warwick.userlookup.User
import org.apache.poi.xssf.usermodel.XSSFSheet
import uk.ac.warwick.tabula.data.model.{UnspecifiedTypeUserGroup, UserGroup}

class AllocateStudentsTemplateCommandTest extends TestBase with Mockito {

	private trait Fixture {
		val userLookup = new MockUserLookup

		def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
			case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
			case ug: UserGroup => ug.userLookup = userLookup
		}

		val module = Fixtures.module("in101", "Introduction to Scala")
		val set = Fixtures.smallGroupSet("My small groups")

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

		val group1 = Fixtures.smallGroup("Group 1")
		val group2 = Fixtures.smallGroup("Group 2")
		val group3 = Fixtures.smallGroup("Group 3")
		val group4 = Fixtures.smallGroup("Group 4")

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

		set.module = module
		wireUserLookup(set.members)

		set.membershipService = smartMock[AssessmentMembershipService]
		set.membershipService.determineMembershipUsers(Seq(), Some(set.members)) returns (set.members.users)
	}

	private trait CommandFixture extends Fixture {
		val command = new AllocateStudentsToGroupsTemplateCommandInternal(module, set)
	}

	@Test def allocateUsersSheet { new CommandFixture {
		implicit class SearchableSheet(self:XSSFSheet) {
			def containsDataRow(id:String, name:String, maxRows:Int = self.getLastRowNum):Boolean = {
				val rows = for (i<- 1 to maxRows) yield self.getRow(i)
				val matchingRow = rows.find(r=>r.getCell(0).toString == id && r.getCell(1).toString == name)
				matchingRow.isDefined
			}
		}

		val workbook = command.generateWorkbook()

		val allocateSheet = workbook.getSheet(command.allocateSheetName)

		val headerRow = allocateSheet.getRow(0)
		headerRow.getCell(0).toString should be ("student_id")
		headerRow.getCell(1).toString should be ("Student name")
		headerRow.getCell(2).toString should be ("Group name")
		headerRow.getCell(3).toString should be ("group_id")

		allocateSheet.containsDataRow("0672089","Mathew Mannion", maxRows = 6) should be(true)
		allocateSheet.containsDataRow("0672088","Nick Howes", maxRows = 6) should be(true)
		allocateSheet.containsDataRow("8888888","Steven Carpenter", maxRows = 6) should be(true)
		allocateSheet.containsDataRow("9293883","Matthew Jones", maxRows = 6) should be(true)
		allocateSheet.containsDataRow("0200202","John Dale", maxRows = 6) should be(true)
		allocateSheet.containsDataRow("","", maxRows = 6) should be(true)

		val blankRow = allocateSheet.getRow(6)
		blankRow.getCell(0).toString should be ("")
		blankRow.getCell(1).toString should be ("")
	}}

	@Test def groupLookupSheet { new CommandFixture {
		val workbook = command.generateWorkbook()

		val groupLookupSheet = workbook.getSheet(command.groupLookupSheetName)

		var groupRow = groupLookupSheet.getRow(1)
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
		val excelDownload = command.applyInternal()

		excelDownload.getContentType() should be ("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
	}}

	@Test def permissions { new Fixture {
		val (theModule, theSet) = (module, set)
		val command = new AllocateStudentsToGroupsTemplatePermissions with AllocateStudentsToGroupsTemplateCommandState {
			val module = theModule
			val set = theSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		there was one(checking).PermissionCheck(Permissions.SmallGroups.Allocate, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoDepartment {
		val command = new AllocateStudentsToGroupsTemplatePermissions with AllocateStudentsToGroupsTemplateCommandState {
			val module = null
			val set = new SmallGroupSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoSet {
		val command = new AllocateStudentsToGroupsTemplatePermissions with AllocateStudentsToGroupsTemplateCommandState {
			val module = Fixtures.module("in101")
			val set = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsUnlinkedSet {
		val command = new AllocateStudentsToGroupsTemplatePermissions with AllocateStudentsToGroupsTemplateCommandState {
			val module = Fixtures.module("in101")
			module.id = "set id"

			val set = new SmallGroupSet(Fixtures.module("other"))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test def wires { new Fixture {
		val command = AllocateStudentsToGroupsTemplateCommand(module, set)

		command should be (anInstanceOf[Appliable[ExcelView]])
		command should be (anInstanceOf[AllocateStudentsToGroupsTemplatePermissions])
		command should be (anInstanceOf[AllocateStudentsToGroupsTemplateCommandState])
		command should be (anInstanceOf[ReadOnly])
		command should be (anInstanceOf[Unaudited])
	}}

}
