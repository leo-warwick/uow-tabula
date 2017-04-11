package uk.ac.warwick.tabula.commands.groups.admin

import org.joda.time.LocalTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.{MapLocation, Module, NamedLocation}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SmallGroupService, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, Mockito, TestBase}

class EditSmallGroupSetDefaultPropertiesTest extends TestBase with Mockito {

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

	private trait CommandFixture extends Fixture {
		val command = new EditSmallGroupSetDefaultPropertiesCommandInternal(module, set) with CommandTestSupport
	}

	@Test def apply { new CommandFixture {
		command.defaultWeekRanges = Seq(WeekRange(1, 10))
		command.defaultDay = DayOfWeek.Thursday
		command.defaultStartTime = new LocalTime(11, 0)
		command.defaultEndTime = new LocalTime(13, 0)
		command.defaultLocation = "The park"

		command.applyInternal() should be (set)

		set.defaultWeekRanges should be (Seq(WeekRange(1, 10)))
		set.defaultLocation should be (NamedLocation("The park"))
		set.defaultDay should be (DayOfWeek.Thursday)
		set.defaultStartTime should be (new LocalTime(11, 0))
		set.defaultEndTime should be (new LocalTime(13, 0))

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def resetExistingEvents { new CommandFixture {
		command.defaultWeekRanges = Seq(WeekRange(1, 10))
		command.defaultDay = DayOfWeek.Thursday
		command.defaultStartTime = new LocalTime(11, 0)
		command.defaultEndTime = new LocalTime(13, 0)
		command.defaultLocation = "The park"

		val group1: SmallGroup = Fixtures.smallGroup("Group A")
		val group2: SmallGroup = Fixtures.smallGroup("Group B")

		val event1: SmallGroupEvent = Fixtures.smallGroupEvent("Event 1")
		val event2: SmallGroupEvent = Fixtures.smallGroupEvent("Event 2")
		event2.location = MapLocation("ITS WW", "12354")

		val event3: SmallGroupEvent = Fixtures.smallGroupEvent("Event 3")
		event3.weekRanges = Seq(WeekRange(1, 5))

		set.groups.add(group1)
		set.groups.add(group2)

		group1.addEvent(event1)
		group1.addEvent(event2)
		group2.addEvent(event3)

		command.resetExistingEvents = true

		command.applyInternal() should be (set)

		set.defaultWeekRanges should be (Seq(WeekRange(1, 10)))
		set.defaultLocation should be (NamedLocation("The park"))
		set.defaultDay should be (DayOfWeek.Thursday)
		set.defaultStartTime should be (new LocalTime(11, 0))
		set.defaultEndTime should be (new LocalTime(13, 0))

		event1.weekRanges should be (Seq(WeekRange(1, 10)))
		event1.location should be (NamedLocation("The park"))
		event1.day should be (DayOfWeek.Thursday)
		event1.startTime should be (new LocalTime(11, 0))
		event1.endTime should be (new LocalTime(13, 0))

		event2.weekRanges should be (Seq(WeekRange(1, 10)))
		event2.location should be (NamedLocation("The park"))
		event2.day should be (DayOfWeek.Thursday)
		event2.startTime should be (new LocalTime(11, 0))
		event2.endTime should be (new LocalTime(13, 0))

		event3.weekRanges should be (Seq(WeekRange(1, 10)))
		event3.location should be (NamedLocation("The park"))
		event3.day should be (DayOfWeek.Thursday)
		event3.startTime should be (new LocalTime(11, 0))
		event3.endTime should be (new LocalTime(13, 0))

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def permissions { new Fixture {
		val (theModule, theSet) = (module, set)
		val command = new EditSmallGroupSetDefaultPropertiesPermissions with EditSmallGroupSetDefaultPropertiesCommandState {
			val module: Module = theModule
			val set: SmallGroupSet = theSet
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Update, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoModule {
		val command = new EditSmallGroupSetDefaultPropertiesPermissions with EditSmallGroupSetDefaultPropertiesCommandState {
			val module = null
			val set = new SmallGroupSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoSet {
		val command = new EditSmallGroupSetDefaultPropertiesPermissions with EditSmallGroupSetDefaultPropertiesCommandState {
			val module: Module = Fixtures.module("in101")
			val set = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsUnlinkedSet {
		val command = new EditSmallGroupSetDefaultPropertiesPermissions with EditSmallGroupSetDefaultPropertiesCommandState {
			val module: Module = Fixtures.module("in101")
			module.id = "set id"

			val set = new SmallGroupSet(Fixtures.module("other"))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	private trait ValidationFixture extends Fixture {
		val command = new EditSmallGroupSetDefaultPropertiesValidation with EditSmallGroupSetDefaultPropertiesCommandState {
			val module: Module = ValidationFixture.this.module
			val set: SmallGroupSet = ValidationFixture.this.set
		}
	}

	@Test def validationPasses { new ValidationFixture {
		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validationInvalidLocation { new ValidationFixture {
		command.defaultLocation = "Location with | a pipe"

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("defaultLocation")
		errors.getFieldError.getCodes should contain ("smallGroupEvent.location.invalidChar")
	}}

	@Test def validationEndTimeBeforeStart { new ValidationFixture {
		command.defaultStartTime = new LocalTime(14, 0)
		command.defaultEndTime = new LocalTime(13, 0)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("defaultEndTime")
		errors.getFieldError.getCodes should contain ("smallGroupEvent.endTime.beforeStartTime")
	}}

	@Test def describe { new Fixture {
		val (mod, s) = (module, set)
		val command = new EditSmallGroupSetDefaultPropertiesDescription with EditSmallGroupSetDefaultPropertiesCommandState {
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
		val command = EditSmallGroupSetDefaultPropertiesCommand(module, set)

		command should be (anInstanceOf[Appliable[Seq[SmallGroupSet]]])
		command should be (anInstanceOf[Describable[Seq[SmallGroupSet]]])
		command should be (anInstanceOf[EditSmallGroupSetDefaultPropertiesPermissions])
		command should be (anInstanceOf[EditSmallGroupSetDefaultPropertiesCommandState])
		command should be (anInstanceOf[SelfValidating])
	}}

}
