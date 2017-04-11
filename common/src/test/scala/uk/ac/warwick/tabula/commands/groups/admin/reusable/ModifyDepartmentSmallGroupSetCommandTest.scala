package uk.ac.warwick.tabula.commands.groups.admin.reusable

import org.joda.time.DateTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupService, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking

class ModifyDepartmentSmallGroupSetCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends SmallGroupServiceComponent {
		val smallGroupService: SmallGroupService = mock[SmallGroupService]
	}

	private trait Fixture {
		val department: Department = Fixtures.department("in", "IT Services")
		val academicYear = AcademicYear(2015)
	}

	private trait ExistingSetFixture extends Fixture {
		val set = new DepartmentSmallGroupSet(department)
		set.id = "existingId"
		set.name = "Existing set"
	}

	private trait CreateCommandFixture extends Fixture {
		val command = new CreateDepartmentSmallGroupSetCommandInternal(department, academicYear) with CommandTestSupport
	}

	private trait EditCommandFixture extends ExistingSetFixture {
		val command = new EditDepartmentSmallGroupSetCommandInternal(department, academicYear, set) with CommandTestSupport
	}

	@Test def create() { new CreateCommandFixture {
		command.name = "Set name"

		val set: DepartmentSmallGroupSet = command.applyInternal()
		set.name should be ("Set name")
		set.members should not be null

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def edit() { new EditCommandFixture {
		command.name should be (set.name)

		command.name = "Set name"

		command.applyInternal() should be (set)
		set.name should be ("Set name")
		set.members should not be null

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def createPermissions() { new Fixture {
		val theDepartment: Department = department
		val theAcademicYear: AcademicYear = academicYear
		val command = new CreateDepartmentSmallGroupSetPermissions with CreateDepartmentSmallGroupSetCommandState {
			val department: Department = theDepartment
			val academicYear: AcademicYear = theAcademicYear
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Create, department)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def createNoDepartment() {
		val command = new CreateDepartmentSmallGroupSetPermissions with CreateDepartmentSmallGroupSetCommandState {
			val department = null
			val academicYear = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test def editPermissions() { new ExistingSetFixture {
		val (d, a, s) = (department, academicYear, set)

		val command = new EditDepartmentSmallGroupSetPermissions with EditDepartmentSmallGroupSetCommandState {
			val department: Department = d
			val academicYear: AcademicYear = a
			val smallGroupSet: DepartmentSmallGroupSet = s
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Update, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def editNoDepartment() {
		val command = new EditDepartmentSmallGroupSetPermissions with EditDepartmentSmallGroupSetCommandState {
			val department = null
			val academicYear = null
			val smallGroupSet = new DepartmentSmallGroupSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def editNoSet() {
		val command = new EditDepartmentSmallGroupSetPermissions with EditDepartmentSmallGroupSetCommandState {
			val department: Department = Fixtures.department("in")
			val academicYear = null
			val smallGroupSet = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def editUnlinkedSet() {
		val command = new EditDepartmentSmallGroupSetPermissions with EditDepartmentSmallGroupSetCommandState {
			val department: Department = Fixtures.department("in")
			val academicYear = null
			department.id = "set id"

			val smallGroupSet = new DepartmentSmallGroupSet(Fixtures.department("other"))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	private trait ValidationFixture extends Fixture {
		val command = new ModifyDepartmentSmallGroupSetCommandValidation with ModifyDepartmentSmallGroupSetState {
			val department: Department = ValidationFixture.this.department
			val academicYear: AcademicYear = ValidationFixture.this.academicYear
			val existingSet = None
		}
	}

	private trait ValidationFixtureExistingSet extends ExistingSetFixture {
		val command = new ModifyDepartmentSmallGroupSetCommandValidation with ModifyDepartmentSmallGroupSetState {
			val department: Department = ValidationFixtureExistingSet.this.department
			val academicYear: AcademicYear = ValidationFixtureExistingSet.this.academicYear
			val existingSet = Some(ValidationFixtureExistingSet.this.set)
		}
	}

	@Test def validationPasses() { new ValidationFixture {
		command.name = "That's not my name"

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateNoName() { new ValidationFixture {
		command.name = "             "

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("smallGroupSet.name.NotEmpty")
	}}

	@Test def validateNameTooLong() { new ValidationFixture {
		command.name = (1 to 300).map { _ => "a" }.mkString("")

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("smallGroupSet.name.Length")
	}}

	@Test def describeCreate() { new Fixture {
		val dept: Department = department
		val year: AcademicYear = academicYear
		val command = new CreateDepartmentSmallGroupSetDescription with CreateDepartmentSmallGroupSetCommandState {
			override val eventName = "test"
			val department: Department = dept
			val academicYear: AcademicYear = year
		}

		command.name = "new name"

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"department" -> "in",
			"name" -> "new name"
		))
	}}

	@Test def describeEdit() { new ExistingSetFixture {
		val (dept, year, s) = (department, academicYear, set)
		val command = new EditDepartmentSmallGroupSetDescription with EditDepartmentSmallGroupSetCommandState {
			override val eventName = "test"
			val department: Department = dept
			val academicYear: AcademicYear = year
			val smallGroupSet: DepartmentSmallGroupSet = s
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"department" -> "in",
			"smallGroupSet" -> "existingId"
		))
	}}

	@Test def wiresCreate() { new Fixture {
		val command: CreateDepartmentSmallGroupSetCommandInternal with ComposableCommand[DepartmentSmallGroupSet] with ModifyDepartmentSmallGroupSetCommandValidation with CreateDepartmentSmallGroupSetPermissions with CreateDepartmentSmallGroupSetDescription with AutowiringSmallGroupServiceComponent = ModifyDepartmentSmallGroupSetCommand.create(department, academicYear)

		command should be (anInstanceOf[Appliable[DepartmentSmallGroupSet]])
		command should be (anInstanceOf[Describable[DepartmentSmallGroupSet]])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[CreateDepartmentSmallGroupSetPermissions])
		command should be (anInstanceOf[CreateDepartmentSmallGroupSetCommandState])
	}}

	@Test def wiresEdit() { new ExistingSetFixture {
		val command: EditDepartmentSmallGroupSetCommandInternal with ComposableCommand[DepartmentSmallGroupSet] with ModifyDepartmentSmallGroupSetCommandValidation with EditDepartmentSmallGroupSetPermissions with EditDepartmentSmallGroupSetDescription with AutowiringSmallGroupServiceComponent = ModifyDepartmentSmallGroupSetCommand.edit(department, academicYear, set)

		command should be (anInstanceOf[Appliable[DepartmentSmallGroupSet]])
		command should be (anInstanceOf[Describable[DepartmentSmallGroupSet]])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[EditDepartmentSmallGroupSetPermissions])
		command should be (anInstanceOf[EditDepartmentSmallGroupSetCommandState])
	}}

}
