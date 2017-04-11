package uk.ac.warwick.tabula.commands.groups.admin

import org.joda.time.DateTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.groups.admin.ModifySmallGroupSetCommand.{Command, CreateCommand}
import uk.ac.warwick.tabula.commands.{Appliable, Describable, DescriptionImpl, SelfValidating}
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, Module, UpstreamAssessmentGroup}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking

class ModifySmallGroupSetCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends SmallGroupServiceComponent with AssessmentMembershipServiceComponent with GeneratesDefaultWeekRanges {
		val smallGroupService: SmallGroupService = smartMock[SmallGroupService]
		val assessmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]

		override def defaultWeekRanges(year: AcademicYear): Seq[WeekRange] = Nil
	}

	private trait Fixture {
		val module: Module = Fixtures.module("in101", "Introduction to Scala")
		module.id = "moduleId"
	}

	private trait ExistingSetFixture extends Fixture {
		val set = new SmallGroupSet(module)
		set.id = "existingId"
		set.name = "Existing set"
		set.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		set.format = SmallGroupFormat.Seminar
		set.allocationMethod = SmallGroupAllocationMethod.Manual
	}

	private trait CreateCommandFixture extends Fixture {
		val command = new CreateSmallGroupSetCommandInternal(module) with CommandTestSupport
	}

	private trait EditCommandFixture extends ExistingSetFixture {
		val command = new EditSmallGroupSetCommandInternal(module, set) with CommandTestSupport
	}

	@Test def create { new CreateCommandFixture {
		command.name = "Set name"
		command.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		command.assessmentMembershipService.getAssessmentComponents(module) returns (Seq())

		val set: SmallGroupSet = command.applyInternal()
		set.name should be ("Set name")
		set.academicYear should be (AcademicYear.guessSITSAcademicYearByDate(DateTime.now))
		set.members should not be (null)

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def createAutoLinkToSits { new CreateCommandFixture {
		command.name = "Set name"
		command.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		val ac1: AssessmentComponent = Fixtures.upstreamAssignment(Fixtures.module("in101"), 1)
		val upstream1: UpstreamAssessmentGroup = Fixtures.assessmentGroup(ac1)

		val ac2: AssessmentComponent = Fixtures.upstreamAssignment(Fixtures.module("in102"), 2)
		val upstream2: UpstreamAssessmentGroup = Fixtures.assessmentGroup(ac2)

		command.assessmentMembershipService.getAssessmentComponents(module) returns (Seq(ac1, ac2))
		command.assessmentMembershipService.getUpstreamAssessmentGroups(ac1, command.academicYear) returns (Seq(upstream1))
		command.assessmentMembershipService.getUpstreamAssessmentGroups(ac2, command.academicYear) returns (Seq(upstream2))

		val set: SmallGroupSet = command.applyInternal()
		set.name should be ("Set name")
		set.academicYear should be (AcademicYear.guessSITSAcademicYearByDate(DateTime.now))
		set.members should not be (null)
		set.assessmentGroups.size() should be (2)

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def edit { new EditCommandFixture {
		command.name should be (set.name)
		command.academicYear should be (set.academicYear)

		command.name = "Set name"
		command.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		command.applyInternal() should be (set)
		set.name should be ("Set name")
		set.academicYear should be (AcademicYear.guessSITSAcademicYearByDate(DateTime.now))
		set.members should not be (null)

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def createPermissions { new Fixture {
		val theDepartment: Module = module
		val command = new CreateSmallGroupSetPermissions with CreateSmallGroupSetCommandState {
			val module: Module = theDepartment
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Create, module)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def createNoDepartment {
		val command = new CreateSmallGroupSetPermissions with CreateSmallGroupSetCommandState {
			val module = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test def editPermissions { new ExistingSetFixture {
		val (m, s) = (module, set)

		val command = new EditSmallGroupSetPermissions with EditSmallGroupSetCommandState {
			val module: Module = m
			val set: SmallGroupSet = s
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Update, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def editNoDepartment {
		val command = new EditSmallGroupSetPermissions with EditSmallGroupSetCommandState {
			val module = null
			val set = new SmallGroupSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def editNoSet {
		val command = new EditSmallGroupSetPermissions with EditSmallGroupSetCommandState {
			val module: Module = Fixtures.module("in101")
			val set = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def editUnlinkedSet {
		val command = new EditSmallGroupSetPermissions with EditSmallGroupSetCommandState {
			val module: Module = Fixtures.module("in101")
			module.id = "set id"

			val set = new SmallGroupSet(Fixtures.module("other"))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	private trait ValidationFixture extends Fixture {
		val command = new ModifySmallGroupSetValidation with ModifySmallGroupSetCommandState with SmallGroupServiceComponent {
			val module: Module = ValidationFixture.this.module
			val existingSet = None
			val smallGroupService: SmallGroupService = mock[SmallGroupService]
		}
	}

	private trait ValidationFixtureExistingSet extends ExistingSetFixture {
		val command = new ModifySmallGroupSetValidation with ModifySmallGroupSetCommandState with SmallGroupServiceComponent {
			val module: Module = ValidationFixtureExistingSet.this.module
			val existingSet = Some(ValidationFixtureExistingSet.this.set)
			val smallGroupService: SmallGroupService = mock[SmallGroupService]
		}
	}

	@Test def validationPasses { new ValidationFixture {
		command.name = "That's not my name"
		command.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		command.format = SmallGroupFormat.Seminar
		command.allocationMethod = SmallGroupAllocationMethod.Manual

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateNoName { new ValidationFixture {
		command.name = "             "
		command.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		command.format = SmallGroupFormat.Seminar
		command.allocationMethod = SmallGroupAllocationMethod.Manual

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("smallGroupSet.name.NotEmpty")
	}}

	@Test def validateNameTooLong { new ValidationFixture {
		command.name = (1 to 300).map { _ => "a" }.mkString("")
		command.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		command.format = SmallGroupFormat.Seminar
		command.allocationMethod = SmallGroupAllocationMethod.Manual

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("smallGroupSet.name.Length")
	}}

	@Test def validateNoFormat { new ValidationFixture {
		command.name = "That's not my name"
		command.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		command.allocationMethod = SmallGroupAllocationMethod.Manual

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("format")
		errors.getFieldError.getCodes should contain ("smallGroupSet.format.NotEmpty")
	}}

	@Test def validateNoAllocationMethod { new ValidationFixture {
		command.name = "That's not my name"
		command.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		command.format = SmallGroupFormat.Seminar
		command.allocationMethod = null

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("allocationMethod")
		errors.getFieldError.getCodes should contain ("smallGroupSet.allocationMethod.NotEmpty")
	}}

	@Test def validateCantChangeAcademicYear { new ValidationFixtureExistingSet {
		set.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		command.name = "That's not my name"
		command.academicYear = set.academicYear + 1
		command.format = SmallGroupFormat.Seminar
		command.allocationMethod = SmallGroupAllocationMethod.Manual

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("academicYear")
		errors.getFieldError.getCodes should contain ("smallGroupSet.academicYear.cantBeChanged")
	}}

	@Test def validateCanUnlinkIfNotReleased { new ValidationFixtureExistingSet {
		command.name = "That's not my name"
		command.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		command.format = SmallGroupFormat.Seminar
		command.allocationMethod = SmallGroupAllocationMethod.Manual

		set.allocationMethod = SmallGroupAllocationMethod.Linked

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateCanChangeLinkIfNotReleased { new ValidationFixtureExistingSet {
		command.name = "That's not my name"
		command.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		command.format = SmallGroupFormat.Seminar
		command.allocationMethod = SmallGroupAllocationMethod.Linked
		command.linkedDepartmentSmallGroupSet = Fixtures.departmentSmallGroupSet("A set")

		set.allocationMethod = SmallGroupAllocationMethod.Linked
		set.linkedDepartmentSmallGroupSet = Fixtures.departmentSmallGroupSet("Another set")

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateCantChangeLinkIfAttendanceRecorded { new ValidationFixtureExistingSet {
		command.name = "That's not my name"
		command.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		command.format = SmallGroupFormat.Seminar
		command.allocationMethod = SmallGroupAllocationMethod.Linked
		command.linkedDepartmentSmallGroupSet = Fixtures.departmentSmallGroupSet("A set")

		set.allocationMethod = SmallGroupAllocationMethod.Linked
		set.linkedDepartmentSmallGroupSet = Fixtures.departmentSmallGroupSet("Another set")
		set.collectAttendance = true

		val group: SmallGroup = Fixtures.smallGroup("A Group")
		val event: SmallGroupEvent = Fixtures.smallGroupEvent("An Event")
		group.addEvent(event)

		val eventOccurrence = new SmallGroupEventOccurrence
		eventOccurrence.event = event

		val notRecordedAttendance = new SmallGroupEventAttendance
		notRecordedAttendance.occurrence = eventOccurrence
		notRecordedAttendance.state = AttendanceState.NotRecorded
		eventOccurrence.attendance.add(notRecordedAttendance)

		val eventOccurrence2 = new SmallGroupEventOccurrence
		eventOccurrence2.event = event

		val missedAuthorisedAttendance = new SmallGroupEventAttendance
		missedAuthorisedAttendance.occurrence = eventOccurrence2
		missedAuthorisedAttendance.state = AttendanceState.MissedAuthorised
		eventOccurrence.attendance.add(missedAuthorisedAttendance)

		command.smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event) returns (Seq(eventOccurrence, eventOccurrence2))

		set.groups.add(group)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("allocationMethod")
		errors.getFieldError.getCodes should contain ("smallGroupEvent.allocationMethod.hasAttendance")
	}}

	@Test def validateCanCreateLinkWhenAttendanceNotRecorded { new ValidationFixtureExistingSet {
		command.name = "That's not my name"
		command.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		command.format = SmallGroupFormat.Seminar
		command.allocationMethod = SmallGroupAllocationMethod.Linked
		command.linkedDepartmentSmallGroupSet = Fixtures.departmentSmallGroupSet("A set")

		set.allocationMethod = SmallGroupAllocationMethod.Manual
		set.collectAttendance = true

		val group: SmallGroup = Fixtures.smallGroup("A Group")
		val event: SmallGroupEvent = Fixtures.smallGroupEvent("An Event")
		group.addEvent(event)

		val eventOccurrence = new SmallGroupEventOccurrence
		eventOccurrence.event = event

		val notRecordedAttendance = new SmallGroupEventAttendance
		notRecordedAttendance.occurrence = eventOccurrence
		notRecordedAttendance.state = AttendanceState.NotRecorded
		eventOccurrence.attendance.add(notRecordedAttendance)

		command.smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event) returns (Seq(eventOccurrence))

		set.groups.add(group)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateNoChangesWhenReleased { new ValidationFixtureExistingSet {
		set.format = SmallGroupFormat.Seminar
		set.allocationMethod = SmallGroupAllocationMethod.Linked
		set.linkedDepartmentSmallGroupSet = { val set = Fixtures.departmentSmallGroupSet("A set"); set.id = "someId"; set }
		set.releasedToStudents = true
		set.releasedToTutors = true

		command.name = set.name
		command.format = set.format
		command.academicYear = set.academicYear
		command.allocationMethod = SmallGroupAllocationMethod.Linked
		command.linkedDepartmentSmallGroupSet = set.linkedDepartmentSmallGroupSet

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateCantUnlinkIfReleased { new ValidationFixtureExistingSet {
		command.name = "That's not my name"
		command.academicYear = set.academicYear
		command.format = SmallGroupFormat.Seminar
		command.allocationMethod = SmallGroupAllocationMethod.Manual

		set.allocationMethod = SmallGroupAllocationMethod.Linked
		set.releasedToStudents = true
		set.releasedToTutors = true

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("allocationMethod")
		errors.getFieldError.getCodes should contain ("smallGroupSet.allocationMethod.released")
	}}

	@Test def validateCantChangeLinkIfReleased { new ValidationFixtureExistingSet {
		command.name = "That's not my name"
		command.format = SmallGroupFormat.Seminar
		command.academicYear = set.academicYear
		command.allocationMethod = SmallGroupAllocationMethod.Linked
		command.linkedDepartmentSmallGroupSet = Fixtures.departmentSmallGroupSet("A set")

		set.allocationMethod = SmallGroupAllocationMethod.Linked
		set.linkedDepartmentSmallGroupSet = Fixtures.departmentSmallGroupSet("Another set")
		set.releasedToStudents = true
		set.releasedToTutors = true

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("allocationMethod")
		errors.getFieldError.getCodes should contain ("smallGroupSet.allocationMethod.released")
	}}

	@Test def describeCreate { new Fixture {
		val mod: Module = module
		val command = new CreateSmallGroupSetDescription with CreateSmallGroupSetCommandState {
			override val eventName = "test"
			val module: Module = mod
		}

		command.name = "new name"

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"module" -> "moduleId",
			"name" -> "new name"
		))
	}}

	@Test def describeEdit { new ExistingSetFixture {
		val (mod, s) = (module, set)
		val command = new EditSmallGroupSetDescription with EditSmallGroupSetCommandState {
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

	@Test def wiresCreate { new Fixture {
		val command: CreateCommand = ModifySmallGroupSetCommand.create(module)

		command should be (anInstanceOf[Appliable[SmallGroupSet]])
		command should be (anInstanceOf[Describable[SmallGroupSet]])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[CreateSmallGroupSetPermissions])
		command should be (anInstanceOf[CreateSmallGroupSetCommandState])
	}}

	@Test def wiresEdit { new ExistingSetFixture {
		val command: Command = ModifySmallGroupSetCommand.edit(module, set)

		command should be (anInstanceOf[Appliable[SmallGroupSet]])
		command should be (anInstanceOf[Describable[SmallGroupSet]])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[EditSmallGroupSetPermissions])
		command should be (anInstanceOf[EditSmallGroupSetCommandState])
	}}

	@Test def defaultWeekRanges {
		val generator = new GeneratesDefaultWeekRangesWithTermService with TermServiceComponent {
			val termService = new TermServiceImpl
		}

		generator.defaultWeekRanges(AcademicYear.parse("13/14")) should be (Seq(
			WeekRange(1, 10),
			WeekRange(15, 24),
			WeekRange(30, 34)
		))
	}

}
