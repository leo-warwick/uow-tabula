package uk.ac.warwick.tabula.commands.admin.modules

import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, ModuleAndDepartmentServiceComponent}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions

class AddModuleCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends AddModuleCommandState with ModuleAndDepartmentServiceComponent {
		val moduleAndDepartmentService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]
	}

	trait Fixture {
		val department: Department = Fixtures.department("in", "IT Services")

		val command = new AddModuleCommandInternal(department) with CommandTestSupport
	}

	@Test def init { new Fixture {
		command.department should be (department)
	}}

	@Test def apply { new Fixture {
		command.code = " CS205  "
		command.name = "Introduction to module creation"

		val module: Module = command.applyInternal()
		module.code should be ("cs205") // sanitised
		module.name should be ("Introduction to module creation")

		verify(command.moduleAndDepartmentService, times(1)).saveOrUpdate(module)
	}}

	trait ValidationFixture {
		val command = new AddModuleCommandValidation with CommandTestSupport {
			val department: Department = Fixtures.department("in", "IT Services")
		}
	}

	@Test def validateNoErrors { new ValidationFixture {
		command.code = "cs205"
		command.name = "Introduction to module creation"

		command.moduleAndDepartmentService.getModuleByCode("cs205") returns (None)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def rejectEmptyCode { new ValidationFixture {
		command.code = "    "
		command.name = "Introduction to module creation"

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("code")
		errors.getFieldError.getCodes should contain ("NotEmpty.code")
	}}

	@Test def rejectInvalidCode { new ValidationFixture {
		command.code = "CS 205"
		command.name = "Introduction to module creation"

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("code")
		errors.getFieldError.getCodes should contain ("code.invalid.module")
	}}

	@Test def rejectEmptyName { new ValidationFixture {
		command.code = "cs205"
		command.name = "        "

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("NotEmpty.name")
	}}

	@Test def rejectDuplicateModuleCodeGlobal { new ValidationFixture {
		command.code = "cs205"
		command.name = "Introduction to module creation"

		command.moduleAndDepartmentService.getModuleByCode("cs205") returns (Some(Fixtures.module("cs205", "Another name")))

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("code")
		errors.getFieldError.getCodes should contain ("code.duplicate.module")
	}}

	@Test def rejectDuplicateModuleNameInDepartment { new ValidationFixture {
		command.code = "cs205"
		command.name = "Introduction to module creation"

		command.moduleAndDepartmentService.getModuleByCode("cs205") returns (None)

		command.department.modules.add(Fixtures.module("cs101", "INTRODUCTION TO MODULE CREATION"))

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("name.duplicate.module")
	}}

	@Test
	def permissionsRequireCreateModuleOnDepartment {
		val dept = Fixtures.department("in")

		val command = new AddModuleCommandPermissions with CommandTestSupport {
			val department: Department = dept
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
		verify(checking, times(1)).PermissionCheck(Permissions.Module.Create, dept)
	}

	@Test
	def describe {
		val dept = Fixtures.department("in")

		val command = new AddModuleCommandDescription with CommandTestSupport {
			val eventName: String = "test"
			val department: Department = dept
		}

		command.code = "cs205"
		command.name = "Introduction to module creation"

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"department" -> "in",
			"moduleCode" -> "cs205",
			"moduleName" -> "Introduction to module creation"
		))
	}

	@Test
	def glueEverythingTogether() {
		val department = Fixtures.department("in", "IT Services")
		val command = AddModuleCommand(department)

		command should be (anInstanceOf[Appliable[Module]])
		command should be (anInstanceOf[AddModuleCommandState])
		command should be (anInstanceOf[AddModuleCommandPermissions])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[AddModuleCommandValidation])
		command should be (anInstanceOf[Describable[Module]])
	}

}
