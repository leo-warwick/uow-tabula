package uk.ac.warwick.tabula.commands.groups.admin.reusable

import java.io.{File, FileInputStream}

import org.springframework.validation.{BindException, BindingResult}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroup, DepartmentSmallGroupSet}
import uk.ac.warwick.tabula.data.model.{Department, FileAttachment, UnspecifiedTypeUserGroup, UserGroup}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.groups.docconversion.{AllocateStudentItem, GroupsExtractor, GroupsExtractorComponent}
import uk.ac.warwick.tabula.services.objectstore.ObjectStorageService
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

class AllocateStudentsToDepartmentalSmallGroupsCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends SmallGroupServiceComponent with AllocateStudentsToDepartmentalSmallGroupsSorting {
		val smallGroupService: SmallGroupService = smartMock[SmallGroupService]
	}

	private trait Fixture {
		val userLookup = new MockUserLookup

		def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
			case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
			case ug: UserGroup => ug.userLookup = userLookup
		}

		val department: Department = Fixtures.department("in", "IT Services")
		val set: DepartmentSmallGroupSet = Fixtures.departmentSmallGroupSet("My small groups")
		set.id = "existingId"
		set.department = department
		wireUserLookup(set.members)

		val user1 = new User("cuscav")
		user1.setFoundUser{true}
		user1.setFirstName("Mathew")
		user1.setLastName("Mannion")
		user1.setWarwickId("0672089")

		val user2 = new User("cusebr")
		user2.setFoundUser{true}
		user2.setFirstName("Nick")
		user2.setLastName("Howes")
		user2.setWarwickId("0672088")

		val user3 = new User("cusfal")
		user3.setFoundUser{true}
		user3.setFirstName("Matthew")
		user3.setLastName("Jones")
		user3.setWarwickId("9293883")

		val user4 = new User("curef")
		user4.setFoundUser{true}
		user4.setFirstName("John")
		user4.setLastName("Dale")
		user4.setWarwickId("0200202")

		val user5 = new User("cusmab")
		user5.setFoundUser{true}
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

		val group1: DepartmentSmallGroup = Fixtures.departmentSmallGroup("Group 1")
		group1.id = "group1Id"

		val group2: DepartmentSmallGroup = Fixtures.departmentSmallGroup("Group 2")
		group2.id = "group2Id"

		set.groups.add(group1)
		set.groups.add(group2)
		group1.groupSet = set
		group2.groupSet = set
		wireUserLookup(group1.students)
		wireUserLookup(group2.students)

		set.members.add(user1)
		set.members.add(user2)
		set.members.add(user3)
		set.members.add(user4)
		set.members.add(user5)

//		val sitsStatus = Fixtures.sitsStatus() // defaults to fully enrolled
//
//		val student1 = Fixtures.student("0672089", "cuscav", department, department, sitsStatus)
//		student1.firstName = "Mathew"
//		student1.lastName = "Mannion"
//
//		val student2 = Fixtures.student("0672088", "cusebr", department, department, sitsStatus)
//		student2.firstName = "Nick"
//		student2.lastName = "Howes"
//
//		val student3 = Fixtures.student("9293883", "cusfal", department, department, sitsStatus)
//		student3.firstName = "Matthew"
//		student3.lastName = "Jones"
//
//		val student4 = Fixtures.student("0200202", "curef", department, department, sitsStatus)
//		student4.firstName = "John"
//		student4.lastName = "Dale"
//
//		val student5 = Fixtures.student("8888888", "cusmab", department, department, sitsStatus)
//		student5.firstName = "Steven"
//		student5.lastName = "Carpenter"
//
//		profileService.getMemberByUser(user1) returns (Option(student1))
//		profileService.getMemberByUser(user2) returns (Option(student2))
//		profileService.getMemberByUser(user3) returns (Option(student3))
//		profileService.getMemberByUser(user4) returns (Option(student4))
//		profileService.getMemberByUser(user5) returns (Option(student5))
	}

	private trait CommandFixture extends Fixture {
		val command =
			new AllocateStudentsToDepartmentalSmallGroupsCommandInternal(department, set, new CurrentUser(user1, user1))
				with CommandTestSupport
				with PopulateAllocateStudentsToDepartmentalSmallGroupsCommand
	}

	@Test def apply() { new CommandFixture {
		command.unallocated should be(JList())
		command.mapping should be(JMap(group1 -> JArrayList(), group2 -> JArrayList()))

		command.populate()
		command.sort()

		command.unallocated should be(JList(user5, user4, user2, user3, user1))
		command.mapping should be(JMap(group1 -> JArrayList(), group2 -> JArrayList()))

		command.mapping.get(group1).addAll(Seq(user4, user2).asJavaCollection)
		command.mapping.get(group2).addAll(Seq(user1, user5).asJavaCollection)

		command.sort()

		command.mapping should be(JMap(group1 -> JArrayList(user4, user2), group2 -> JArrayList(user5, user1)))

		command.applyInternal() should be(set)

		verify(command.smallGroupService, times(1)).saveOrUpdate(group1)
		verify(command.smallGroupService, times(1)).saveOrUpdate(group2)

		group1.students.asInstanceOf[UserGroup].includedUserIds should be(Seq("0200202", "0672088"))
		group2.students.asInstanceOf[UserGroup].includedUserIds should be(Seq("8888888", "0672089"))
	}}

	private trait ValidationFixture extends Fixture {
		val command =
			new AllocateStudentsToDepartmentalSmallGroupsCommandInternal(department, set, new CurrentUser(user1, user1))
				with CommandTestSupport
				with PopulateAllocateStudentsToDepartmentalSmallGroupsCommand
				with AllocateStudentsToDepartmentalSmallGroupsValidation
	}

	@Test def validatePasses() { new ValidationFixture {
		command.populate()
		command.sort()

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be {false}
	}}

	@Test def validateCantSubmitUnrelatedGroup() { new ValidationFixture {
		command.populate()
		command.sort()

		val group3: DepartmentSmallGroup = Fixtures.departmentSmallGroup("Group 3")
		group3.id = "group3Id"
		group3.groupSet = Fixtures.departmentSmallGroupSet("Another set")

		command.mapping.put(group3, Seq(user3).toList.asJava)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getGlobalError.getCodes should contain ("smallGroup.allocation.groups.invalid")
	}}

	private trait FileUploadSupportFixture extends Fixture {
		val command = new AllocateStudentsToDepartmentalSmallGroupsCommandInternal(department, set, new CurrentUser(user1, user1))
			with AllocateStudentsToDepartmentalSmallGroupsFileUploadSupport
			with CommandTestSupport
			with PopulateAllocateStudentsToDepartmentalSmallGroupsCommand
			with GroupsExtractorComponent with UserLookupComponent {

			val groupsExtractor: GroupsExtractor = smartMock[GroupsExtractor]
			val userLookup: MockUserLookup = FileUploadSupportFixture.this.userLookup
		}

		command.smallGroupService.getDepartmentSmallGroupById(group1.id) returns Some(group1)
		command.smallGroupService.getDepartmentSmallGroupById(group2.id) returns Some(group2)
	}

	@Test def fileUploadSupport() { new FileUploadSupportFixture {
		command.populate()
		command.sort()

		val attachment = new FileAttachment
		attachment.id = "123"
		attachment.name = "file.xlsx"

		val backingFile: File = createTemporaryFile()
		attachment.objectStorageService = smartMock[ObjectStorageService]
		attachment.objectStorageService.keyExists(attachment.id) returns true
		attachment.objectStorageService.metadata(attachment.id) returns Some(ObjectStorageService.Metadata(backingFile.length(), "application/octet-stream", None))
		attachment.objectStorageService.fetch(attachment.id) answers { _ => Some(new FileInputStream(backingFile)) }

		val file = new UploadedFile
		file.attached.add(attachment)
		file.maintenanceMode = smartMock[MaintenanceModeService]
		command.file = file

		command.groupsExtractor.readXSSFExcelFile(any[FileInputStream]) returns Seq(
			new AllocateStudentItem(user1.getWarwickId, group1.id),
			new AllocateStudentItem(user2.getWarwickId, group1.id),
			new AllocateStudentItem(user3.getWarwickId, group2.id),
			new AllocateStudentItem(user4.getWarwickId, group2.id),
			new AllocateStudentItem(user5.getWarwickId, null)
		).toList.asJava

		command.onBind(mock[BindingResult])

		command.mapping should be(JMap(group1 -> JArrayList(user1, user2), group2 -> JArrayList(user3, user4)))
	}}

	@Test def validateUploadedFilePasses() { new FileUploadSupportFixture {
		val attachment = new FileAttachment
		attachment.id = "456"
		attachment.name = "file.xlsx" // We only accept xlsx

		val backingFile: File = createTemporaryFile()
		attachment.objectStorageService = smartMock[ObjectStorageService]
		attachment.objectStorageService.keyExists(attachment.id) returns true
		attachment.objectStorageService.metadata(attachment.id) returns Some(ObjectStorageService.Metadata(backingFile.length(), "application/octet-stream", None))
		attachment.objectStorageService.fetch(attachment.id) answers { _ => Some(new FileInputStream(backingFile)) }

		val file = new UploadedFile
		file.attached.add(attachment)
		command.file = file

		val errors = new BindException(command, "command")
		command.validateUploadedFile(errors)

		errors.hasErrors should be {false}
	}}

	@Test def validateUploadedFileWrongExtension() { new FileUploadSupportFixture {
		val attachment = new FileAttachment
		attachment.id = "789"
		attachment.name = "file.xls" // We only accept xlsx

		val file = new UploadedFile
		file.attached.add(attachment)
		command.file = file

		val errors = new BindException(command, "command")
		command.validateUploadedFile(errors)

		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("file")
		errors.getFieldError.getCodes should contain ("file.wrongtype.one")
	}}

	@Test def permissions() { new Fixture {
		val (theDepartment, theSet) = (department, set)
		val command = new AllocateStudentsToDepartmentalSmallGroupsPermissions with AllocateStudentsToDepartmentalSmallGroupsCommandState {
			val department: Department = theDepartment
			val set: DepartmentSmallGroupSet = theSet
			val viewer = new CurrentUser(user1, user1)
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Allocate, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoDepartment() {
		val command = new AllocateStudentsToDepartmentalSmallGroupsPermissions with AllocateStudentsToDepartmentalSmallGroupsCommandState {
			val department = null
			val set = new DepartmentSmallGroupSet
			val viewer = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoSet() {
		val command = new AllocateStudentsToDepartmentalSmallGroupsPermissions with AllocateStudentsToDepartmentalSmallGroupsCommandState {
			val department: Department = Fixtures.department("in")
			val set = null
			val viewer = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsUnlinkedSet() {
		val command = new AllocateStudentsToDepartmentalSmallGroupsPermissions with AllocateStudentsToDepartmentalSmallGroupsCommandState {
			val department: Department = Fixtures.department("in")
			department.id = "set id"

			val set = new DepartmentSmallGroupSet(Fixtures.department("other"))
			val viewer = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test def describe() { new Fixture {
		val (dept, s) = (department, set)
		val command = new AllocateStudentsToDepartmentalSmallGroupsDescription with AllocateStudentsToDepartmentalSmallGroupsCommandState {
			override val eventName = "test"
			val department: Department = dept
			val set: DepartmentSmallGroupSet = s
			val viewer = null
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"department" -> "in",
			"smallGroupSet" -> "existingId"
		))
	}}

	@Test def wires() { new Fixture { withUser("cuscav") {
		val command = AllocateStudentsToDepartmentalSmallGroupsCommand(department, set, currentUser)

		command should be (anInstanceOf[Appliable[Seq[DepartmentSmallGroup]]])
		command should be (anInstanceOf[Describable[Seq[DepartmentSmallGroup]]])
		command should be (anInstanceOf[AllocateStudentsToDepartmentalSmallGroupsPermissions])
		command should be (anInstanceOf[AllocateStudentsToDepartmentalSmallGroupsCommandState])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[PopulateOnForm])
		command should be (anInstanceOf[BindListener])
		command should be (anInstanceOf[GroupsObjects[User, DepartmentSmallGroup]])
		command should be (anInstanceOf[GroupsObjectsWithFileUpload[User, DepartmentSmallGroup]])
	}}}

}
