package uk.ac.warwick.tabula.commands.groups.admin.reusable

import org.springframework.validation.{BindException, BindingResult}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SmallGroupService, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.mutable

class EditDepartmentSmallGroupsCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends SmallGroupServiceComponent {
		val smallGroupService: SmallGroupService = mock[SmallGroupService]
	}

	private trait Fixture {
		val department: Department = Fixtures.department("in", "IT Services")
		val set = new DepartmentSmallGroupSet(department)
		set.id = "existingId"
		set.name = "Existing set"
	}

	private trait ExistingGroupsFixture extends Fixture {
		val groupA = new DepartmentSmallGroup(set) {
			name = "Group A"
			id = "1"
		}
		val groupB = new DepartmentSmallGroup(set) {
			name = "Group B"
			id = "2"
		}
		val groupC = new DepartmentSmallGroup(set) {
			name = "Group C"
			id = "3"
		}
		val groupD = new DepartmentSmallGroup(set) {
			name = "Group D"
			id = "4"
		}

		set.groups.add(groupA)
		set.groups.add(groupB)
		set.groups.add(groupC)
		set.groups.add(groupD)
	}

	private trait CommandFixture extends Fixture {
		val command = new EditDepartmentSmallGroupsCommandInternal(department, set) with CommandTestSupport
	}

	private trait CommandWithExistingFixture extends ExistingGroupsFixture {
		val command = new EditDepartmentSmallGroupsCommandInternal(department, set) with CommandTestSupport with PopulateEditDepartmentSmallGroupsCommand with EditDepartmentSmallGroupsCommandRemoveTrailingEmptyGroups
		command.populate()
	}

	@Test def create() { new CommandFixture {
		command.groupNames.add("Group A")
		command.groupNames.add("Group B")
		command.groupNames.add("Group C")
		command.groupNames.add("Group D")
		command.groupNames.add("Group E")

		val groups: mutable.Buffer[DepartmentSmallGroup] = command.applyInternal()
		groups.size should be (5)

		groups.head.name should be ("Group A")
		groups(1).name should be ("Group B")
		groups(2).name should be ("Group C")
		groups(3).name should be ("Group D")
		groups(4).name should be ("Group E")

		set.groups.asScala should be (groups)

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def edit() { new CommandWithExistingFixture {
		command.groupNames.asScala should be (Seq("Group A", "Group B", "Group C", "Group D"))
		command.groupIds.asScala should be (Seq("1", "2", "3", "4"))

		// Rename Group B
		command.groupNames.set(1, "Edited group")
		// Remove Group C
		command.groupNames.remove(2)
		command.groupIds.remove(2)

		command.onBind(smartMock[BindingResult])

		val groups: mutable.Buffer[DepartmentSmallGroup] = command.applyInternal()
		groups should be (Seq(groupA, groupB, groupD))

		groupA.name should be ("Group A")
		groupA.id should be ("1")
		groupB.name should be ("Edited group")
		groupB.id should be ("2")
		groupD.name should be ("Group D")
		groupD.id should be ("4")

		set.groups.asScala should be (groups)

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def permissions() { new Fixture {
		val (theDepartment, theSet) = (department, set)
		val command = new EditDepartmentSmallGroupsPermissions with EditDepartmentSmallGroupsCommandState {
			val department: Department = theDepartment
			val set: DepartmentSmallGroupSet = theSet
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Update, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoDepartment() {
		val command = new EditDepartmentSmallGroupsPermissions with EditDepartmentSmallGroupsCommandState {
			val department = null
			val set = new DepartmentSmallGroupSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoSet() {
		val command = new EditDepartmentSmallGroupsPermissions with EditDepartmentSmallGroupsCommandState {
			val department: Department = Fixtures.department("in")
			val set = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsUnlinkedSet() {
		val command = new EditDepartmentSmallGroupsPermissions with EditDepartmentSmallGroupsCommandState {
			val department: Department = Fixtures.department("in")
			department.id = "set id"

			val set = new DepartmentSmallGroupSet(Fixtures.department("other"))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	private trait ValidationFixture extends ExistingGroupsFixture {
		val command = new EditDepartmentSmallGroupsValidation with EditDepartmentSmallGroupsCommandState
			with PopulateEditDepartmentSmallGroupsCommand with CommandTestSupport {
			val department: Department = ValidationFixture.this.department
			val set: DepartmentSmallGroupSet = ValidationFixture.this.set
		}
		command.populate()
	}

	@Test def validationPasses() { new ValidationFixture {
		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validationAddNewPasses() { new ValidationFixture {
		command.groupNames.add("Group E")

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validationEditPasses() { new ValidationFixture {
		command.groupNames.set(1, "Edited group")
		command.groupNames.remove(3)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateNoName() { new ValidationFixture {
		command.groupNames.set(1, "             ")

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("groupNames[1]")
		errors.getFieldError.getCodes should contain ("smallGroup.name.NotEmpty")
	}}

	@Test def validateNameTooLong() { new ValidationFixture {
		command.groupNames.set(1, (1 to 300).map { _ => "a" }.mkString(""))

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("groupNames[1]")
		errors.getFieldError.getCodes should contain ("smallGroup.name.Length")
	}}

	@Test def validateCantRemoveNonEmpty() { new ValidationFixture {
		groupD.students.add(new User("cuscav") {{ setWarwickId("0672089") }})
		command.groupNames.remove(3)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("groupNames[3]")
		errors.getFieldError.getCodes should contain ("smallGroup.delete.notEmpty")
	}}

	@Test def validateCantRemoveWhenAttendanceRecorded() { new ValidationFixture {
		val event: SmallGroupEvent = Fixtures.smallGroupEvent("An Event")
		val linkedGroup: SmallGroup = Fixtures.smallGroup("Linked group")
		groupD.linkedGroups.add(linkedGroup)
		linkedGroup.addEvent(event)

		command.groupNames.remove(3)

		val eventOccurrence = new SmallGroupEventOccurrence
		eventOccurrence.event = event

		val missedAuthorisedAttendance = new SmallGroupEventAttendance
		missedAuthorisedAttendance.occurrence = eventOccurrence
		missedAuthorisedAttendance.state = AttendanceState.MissedAuthorised

		eventOccurrence.attendance.add(missedAuthorisedAttendance)

		command.smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event) returns Seq(eventOccurrence)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("groupNames[3]")
		errors.getFieldError.getCodes should contain ("smallGroupEvent.delete.hasAttendance")
	}}

	@Test def validateCanRemoveWhenAttendaneNotRecorded() { new ValidationFixture {
		val event: SmallGroupEvent = Fixtures.smallGroupEvent("An Event")
		val linkedGroup: SmallGroup = Fixtures.smallGroup("Linked group")
		groupD.linkedGroups.add(linkedGroup)
		linkedGroup.addEvent(event)

		command.groupNames.remove(3)

		val eventOccurrence = new SmallGroupEventOccurrence
		eventOccurrence.event = event

		val notRecordedAttendance = new SmallGroupEventAttendance
		notRecordedAttendance.occurrence = eventOccurrence
		notRecordedAttendance.state = AttendanceState.NotRecorded

		eventOccurrence.attendance.add(notRecordedAttendance)

		command.smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event) returns Seq(eventOccurrence)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def describe() { new Fixture {
		val (dept, s) = (department, set)
		val command = new EditDepartmentSmallGroupsDescription with EditDepartmentSmallGroupsCommandState {
			override val eventName = "test"
			val department: Department = dept
			val set: DepartmentSmallGroupSet = s
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"department" -> "in",
			"smallGroupSet" -> "existingId"
		))
	}}

	@Test def wires() { new Fixture {
		val command = EditDepartmentSmallGroupsCommand(department, set)

		command should be (anInstanceOf[Appliable[Seq[DepartmentSmallGroup]]])
		command should be (anInstanceOf[Describable[Seq[DepartmentSmallGroup]]])
		command should be (anInstanceOf[EditDepartmentSmallGroupsPermissions])
		command should be (anInstanceOf[EditDepartmentSmallGroupsCommandState])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[PopulateOnForm])
		command should be (anInstanceOf[BindListener])
	}}

}
