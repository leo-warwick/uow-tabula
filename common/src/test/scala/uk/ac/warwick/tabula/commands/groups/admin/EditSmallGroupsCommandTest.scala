package uk.ac.warwick.tabula.commands.groups.admin

import org.springframework.validation.{BindException, BindingResult}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SmallGroupService, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.mutable

class EditSmallGroupsCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends SmallGroupServiceComponent {
		val smallGroupService: SmallGroupService = mock[SmallGroupService]
	}

	private trait Fixture {
		val module: Module = Fixtures.module("in101", "Introduction to Scala")
		module.id = "moduleId"

		val set: SmallGroupSet = Fixtures.smallGroupSet("IN101 Seminars")
		set.id = "existingId"
		set.module = module
	}

	private trait ExistingGroupsFixture extends Fixture {
		val groupA: SmallGroup = Fixtures.smallGroup("Group A").tap { _.id = "groupAid" }
		val groupB: SmallGroup = Fixtures.smallGroup("Group B").tap { _.id = "groupBid" }
		val groupC: SmallGroup = Fixtures.smallGroup("Group C").tap { _.id = "groupCid" }
		val groupD: SmallGroup = Fixtures.smallGroup("Group D").tap { _.id = "groupDid" }

		set.groups.add(groupA)
		set.groups.add(groupB)
		set.groups.add(groupC)
		set.groups.add(groupD)
	}

	private trait CommandFixture extends Fixture {
		val command = new EditSmallGroupsCommandInternal(module, set) with CommandTestSupport with PopulateEditSmallGroupsCommand
	}

	private trait CommandWithExistingFixture extends ExistingGroupsFixture {
		val command = new EditSmallGroupsCommandInternal(module, set) with CommandTestSupport with PopulateEditSmallGroupsCommand with EditSmallGroupsCommandRemoveTrailingEmptyGroups
		command.populate()
	}

	@Test def populate { new CommandFixture {
		set.groups.add(Fixtures.smallGroup("Group A").tap { _.id = "groupAId" })
		set.groups.add(Fixtures.smallGroup("Group B").tap { _.id = "groupBId" })

		command.populate()

		command.existingGroups.values().asScala.map { _.name }.toSet should be (Set("Group A", "Group B"))
		command.existingGroups.values().asScala.map { _.maxGroupSize }.toSet should be (Set(null, null))
	}}

	@Test def create { new CommandFixture {
		command.populate()
		command.newGroups.get(0).name = "Group A"
		command.newGroups.get(0).maxGroupSize = 15
		command.newGroups.get(1).name = "Group B"
		command.newGroups.get(1).maxGroupSize = 15

		val groups: mutable.Buffer[SmallGroup] = command.applyInternal()
		set.groups.asScala should be (groups)

		groups(0).name should be ("Group A")
		groups(0).maxGroupSize should be (15)

		groups(1).name should be ("Group B")
		groups(1).maxGroupSize should be (15)

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def createUnlimitedGroup { new CommandFixture {
		command.populate()
		command.newGroups.get(0).name = "Group A"
		command.newGroups.get(0).maxGroupSize = 15
		command.newGroups.get(1).name = "Group B"
		command.newGroups.get(1).maxGroupSize = null

		val groups: mutable.Buffer[SmallGroup] = command.applyInternal()
		set.groups.asScala should be (groups)

		groups(0).name should be ("Group A")
		groups(0).maxGroupSize should be (15)

		groups(1).name should be ("Group B")
		groups(1).maxGroupSize should be (null)

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def edit { new CommandWithExistingFixture {
		command.existingGroups.asScala.values.map { _.name }.toSet should be (Set("Group A", "Group B", "Group C", "Group D"))
		command.existingGroups.get(groupB.id).name = "Edited group"
		command.existingGroups.get(groupD.id).delete = true

		command.onBind(mock[BindingResult])

		val groups: mutable.Buffer[SmallGroup] = command.applyInternal()
		groups should be (Seq(groupA, groupB, groupC))

		groupA.name should be ("Group A")
		groupB.name should be ("Edited group")
		groupC.name should be ("Group C")

		set.groups.asScala should be (groups)

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def permissions { new Fixture {
		val (theModule, theSet) = (module, set)
		val command = new EditSmallGroupsPermissions with EditSmallGroupsCommandState {
			val module: Module = theModule
			val set: SmallGroupSet = theSet
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Update, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoDepartment {
		val command = new EditSmallGroupsPermissions with EditSmallGroupsCommandState {
			val module = null
			val set = new SmallGroupSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoSet {
		val command = new EditSmallGroupsPermissions with EditSmallGroupsCommandState {
			val module: Module = Fixtures.module("in101")
			val set = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsUnlinkedSet {
		val command = new EditSmallGroupsPermissions with EditSmallGroupsCommandState {
			val module: Module = Fixtures.module("in101")
			module.id = "set id"

			val set = new SmallGroupSet(Fixtures.module("other"))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	private trait ValidationFixture extends ExistingGroupsFixture {
		val command = new EditSmallGroupsValidation with EditSmallGroupsCommandState with PopulateEditSmallGroupsCommand with CommandTestSupport {
			val module: Module = ValidationFixture.this.module
			val set: SmallGroupSet = ValidationFixture.this.set
		}
		command.populate()
	}

	@Test def validationPasses { new ValidationFixture {
		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validationAddNewPasses { new ValidationFixture {
		command.newGroups.get(0).name = "Group E"

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validationEditPasses { new ValidationFixture {
		command.existingGroups.get(groupB.id).name = "Edited group"
		command.existingGroups.get(groupD.id).delete = true

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateLinked { new ValidationFixture {
		set.allocationMethod = SmallGroupAllocationMethod.Linked

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getGlobalError.getCodes should contain ("smallGroupSet.linked")
	}}

	@Test def validateNoName { new ValidationFixture {
		command.existingGroups.get(groupB.id).name = "             "

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("existingGroups[groupBid].name")
		errors.getFieldError.getCodes should contain ("smallGroup.name.NotEmpty")
	}}

	@Test def validateNameTooLong { new ValidationFixture {
		command.existingGroups.get(groupB.id).name = (1 to 300).map { _ => "a" }.mkString("")

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("existingGroups[groupBid].name")
		errors.getFieldError.getCodes should contain ("smallGroup.name.Length")
	}}

	@Test def validateCantRemoveNonEmpty { new ValidationFixture {
		groupD.students.add(new User("cuscav") {{ setWarwickId("0672089") }})
		command.existingGroups.get(groupD.id).delete = true

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("existingGroups[groupDid].delete")
		errors.getFieldError.getCodes should contain ("smallGroup.delete.notEmpty")
	}}

	@Test def validateCantRemoveWhenAttendanceRecorded { new ValidationFixture {
		val event: SmallGroupEvent = Fixtures.smallGroupEvent("An Event")
		groupD.addEvent(event)

		command.existingGroups.get(groupD.id).delete = true

		val eventOccurrence = new SmallGroupEventOccurrence
		eventOccurrence.event = event

		val missedAuthorisedAttendance = new SmallGroupEventAttendance
		missedAuthorisedAttendance.occurrence = eventOccurrence
		missedAuthorisedAttendance.state = AttendanceState.MissedAuthorised

		eventOccurrence.attendance.add(missedAuthorisedAttendance)

		command.smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event) returns (Seq(eventOccurrence))

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("existingGroups[groupDid].delete")
		errors.getFieldError.getCodes should contain ("smallGroupEvent.delete.hasAttendance")
	}}

	@Test def validateCanRemoveWhenAttendaneNotRecorded { new ValidationFixture {
		val event: SmallGroupEvent = Fixtures.smallGroupEvent("An Event")
		groupD.addEvent(event)

		command.existingGroups.get(groupD.id).delete = true

		val eventOccurrence = new SmallGroupEventOccurrence
		eventOccurrence.event = event

		val notRecordedAttendance = new SmallGroupEventAttendance
		notRecordedAttendance.occurrence = eventOccurrence
		notRecordedAttendance.state = AttendanceState.NotRecorded

		eventOccurrence.attendance.add(notRecordedAttendance)

		command.smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event) returns (Seq(eventOccurrence))

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def describe { new Fixture {
		val (mod, s) = (module, set)
		val command = new EditSmallGroupsDescription with EditSmallGroupsCommandState {
			override val eventName = "test"
			val module: Module = mod
			val set: SmallGroupSet = s
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"module" -> "moduleId",
			"smallGroupSet" -> "existingId"
		))
	}}

	@Test def wires { new Fixture {
		val command = EditSmallGroupsCommand(module, set)

		command should be (anInstanceOf[Appliable[Seq[SmallGroup]]])
		command should be (anInstanceOf[Describable[Seq[SmallGroup]]])
		command should be (anInstanceOf[EditSmallGroupsPermissions])
		command should be (anInstanceOf[EditSmallGroupsCommandState])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[PopulateEditSmallGroupsCommand])
		command should be (anInstanceOf[BindListener])
	}}

}
