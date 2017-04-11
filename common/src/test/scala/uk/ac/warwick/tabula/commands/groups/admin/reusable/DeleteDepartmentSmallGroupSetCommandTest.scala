package uk.ac.warwick.tabula.commands.groups.admin.reusable

import org.springframework.validation.BindException
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.{Appliable, Describable, DescriptionImpl, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroupSet, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SmallGroupService, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking

class DeleteDepartmentSmallGroupSetCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends SmallGroupServiceComponent {
		val smallGroupService: SmallGroupService = mock[SmallGroupService]
	}

	private trait Fixture {
		val department: Department = Fixtures.department("in", "IT Services")
		val set = new DepartmentSmallGroupSet(department)
		set.id = "existingId"
		set.name = "Existing set"
	}

	private trait CommandFixture extends Fixture {
		val command = new DeleteDepartmentSmallGroupSetCommandInternal(department, set) with CommandTestSupport
	}

	@Test def apply { new CommandFixture {
		set.deleted.booleanValue() should be (false)
		command.applyInternal() should be (set)
		set.deleted.booleanValue() should be (true)

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def permissions { new Fixture {
		val (d, s) = (department, set)

		val command = new DeleteDepartmentSmallGroupSetPermissions with DeleteDepartmentSmallGroupSetCommandState {
			val department: Department = d
			val set: DepartmentSmallGroupSet = s
		}

		val checking: PermissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Delete, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoDepartment {
		val command = new DeleteDepartmentSmallGroupSetPermissions with DeleteDepartmentSmallGroupSetCommandState {
			val department = null
			val set = new DepartmentSmallGroupSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoSet {
		val command = new DeleteDepartmentSmallGroupSetPermissions with DeleteDepartmentSmallGroupSetCommandState {
			val department: Department = Fixtures.department("in")
			val set = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsUnlinkedSet {
		val command = new DeleteDepartmentSmallGroupSetPermissions with DeleteDepartmentSmallGroupSetCommandState {
			val department: Department = Fixtures.department("in")
			department.id = "set id"

			val set = new DepartmentSmallGroupSet(Fixtures.department("other"))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	private trait ValidationFixture extends Fixture {
		val command = new DeleteDepartmentSmallGroupSetValidation with DeleteDepartmentSmallGroupSetCommandState {
			val department: Department = ValidationFixture.this.department
			val set: DepartmentSmallGroupSet = ValidationFixture.this.set
		}
	}

	@Test def validationPasses { new ValidationFixture {
		command.confirm = true

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateNotConfirmed { new ValidationFixture {
		command.confirm = false

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("confirm")
		errors.getFieldError.getCodes should contain ("smallGroupSet.delete.confirm")
	}}

	@Test def validateAlreadyDeleted { new ValidationFixture {
		command.confirm = true
		set.markDeleted()

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getGlobalError.getCodes should contain ("smallGroupSet.delete.deleted")
	}}

	@Test def validateLinkedReleasedToStudents { new ValidationFixture {
		command.confirm = true

		val linked: SmallGroupSet = Fixtures.smallGroupSet("linked")
		linked.releasedToStudents = true
		set.linkedSets.add(linked)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getGlobalError.getCodes should contain ("smallGroupSet.delete.released")
	}}

	@Test def describe { new Fixture {
		val (dept, s) = (department, set)
		val command = new DeleteDepartmentSmallGroupSetDescription with DeleteDepartmentSmallGroupSetCommandState {
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

	@Test def wires { new Fixture {
		val command = DeleteDepartmentSmallGroupSetCommand(department, set)

		command should be (anInstanceOf[Appliable[DepartmentSmallGroupSet]])
		command should be (anInstanceOf[Describable[DepartmentSmallGroupSet]])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[DeleteDepartmentSmallGroupSetPermissions])
		command should be (anInstanceOf[DeleteDepartmentSmallGroupSetCommandState])
	}}

}
