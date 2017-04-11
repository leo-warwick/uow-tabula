package uk.ac.warwick.tabula.commands.groups.admin

import org.apache.poi.xssf.usermodel.{XSSFRow, XSSFSheet, XSSFWorkbook}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.{Appliable, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, ProfileService, ProfileServiceComponent, UserGroupCacheManager}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.web.views.ExcelView
import uk.ac.warwick.userlookup.User

class AllocateStudentsTemplateCommandTest extends TestBase with Mockito {

	private trait Fixture {
		val userLookup = new MockUserLookup

		def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
			case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
			case ug: UserGroup => ug.userLookup = userLookup
		}


		def studentMemberWithCourse(universityId: String, userId: String, firstName: String, lastName: String): StudentMember ={
			val studentWithUsercode1 = new StudentMember
			studentWithUsercode1.universityId = universityId
			studentWithUsercode1.userId = userId
			studentWithUsercode1.firstName = firstName
			studentWithUsercode1.lastName = lastName
			studentWithUsercode1.inUseFlag= "Active"
			Fixtures.studentCourseDetails(studentWithUsercode1,null,null)
			studentWithUsercode1
		}

		val module: Module = Fixtures.module("in101", "Introduction to Scala")
		val set: SmallGroupSet = Fixtures.smallGroupSet("My small groups")

		val user1 = new User("cuscav")
		user1.setFoundUser(true)
		user1.setFirstName("Mathew")
		user1.setLastName("Mannion")
		user1.setWarwickId("0672089")

		val studentWithUsercode1: StudentMember = studentMemberWithCourse("0672089","cuscav", "Mathew", "Mannion")


		val user2 = new User("cusebr")
		user2.setFoundUser(true)
		user2.setFirstName("Nick")
		user2.setLastName("Howes")
		user2.setWarwickId("0672088")
		val studentWithUsercode2: StudentMember = studentMemberWithCourse("0672088","cusebr", "Nick", "Howes")


		val user3 = new User("cusfal")
		user3.setFoundUser(true)
		user3.setFirstName("Matthew")
		user3.setLastName("Jones")
		user3.setWarwickId("9293883")
		val studentWithUsercode3: StudentMember = studentMemberWithCourse("9293883","cusfal", "Matthew", "Jones")

		val user4 = new User("curef")
		user4.setFoundUser(true)
		user4.setFirstName("John")
		user4.setLastName("Dale")
		user4.setWarwickId("0200202")
		val studentWithUsercode4: StudentMember = studentMemberWithCourse("0200202","curef", "John", "Dale")

		val user5 = new User("cusmab")
		user5.setFoundUser(true)
		user5.setFirstName("Steven")
		user5.setLastName("Carpenter")
		user5.setWarwickId("8888888")
		val studentWithUsercode5: StudentMember = studentMemberWithCourse("8888888","cusmab", "Steven", "Carpenter")

		userLookup.users += (
			user1.getUserId -> user1,
			user2.getUserId -> user2,
			user3.getUserId -> user3,
			user4.getUserId -> user4,
			user5.getUserId -> user5
		)

		val group1: SmallGroup = Fixtures.smallGroup("Group 1")
		val group2: SmallGroup = Fixtures.smallGroup("Group 2")
		val group3: SmallGroup = Fixtures.smallGroup("Group 3")
		val group4: SmallGroup = Fixtures.smallGroup("Group 4")

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
		val command = new AllocateStudentsToGroupsTemplateCommandInternal(module, set) with ProfileServiceComponent {
			val profileService: ProfileService = smartMock[ProfileService]

		}
		command.profileService.getMemberByUser(user1) returns (Some(studentWithUsercode1))
		command.profileService.getMemberByUser(user2) returns (Some(studentWithUsercode2))
		command.profileService.getMemberByUser(user3) returns (Some(studentWithUsercode3))
		command.profileService.getMemberByUser(user4) returns (Some(studentWithUsercode4))
		command.profileService.getMemberByUser(user5) returns (Some(studentWithUsercode5))
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
		val (theModule, theSet) = (module, set)
		val command = new AllocateStudentsToGroupsTemplatePermissions with AllocateStudentsToGroupsTemplateCommandState {
			val module: Module = theModule
			val set: SmallGroupSet = theSet
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Allocate, set)
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
			val module: Module = Fixtures.module("in101")
			val set = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsUnlinkedSet {
		val command = new AllocateStudentsToGroupsTemplatePermissions with AllocateStudentsToGroupsTemplateCommandState {
			val module: Module = Fixtures.module("in101")
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
