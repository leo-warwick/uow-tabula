package uk.ac.warwick.tabula.commands.admin.modules

import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.{Fixtures, TestBase, Mockito}
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.commands.{Describable, SelfValidating, Appliable, DescriptionImpl, GroupsObjects}
import uk.ac.warwick.tabula.data.model.{Module, Department}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions

class SortModulesCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends SortModulesCommandState with ModuleAndDepartmentServiceComponent {
		val moduleAndDepartmentService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]
	}

	trait SortModulesWorld {
		val department: Department = Fixtures.department("in", "IT Services")
		val ugDepartment: Department = Fixtures.department("in-ug", "ITS Undergraduates")
		val pgDepartment: Department = Fixtures.department("in-pg", "ITS Postgraduates")

		department.children.add(ugDepartment)
		department.children.add(pgDepartment)

		ugDepartment.parent = department
		pgDepartment.parent = department

		val mod1: Module = Fixtures.module("in101")
		val mod2: Module = Fixtures.module("in102")
		val mod3: Module = Fixtures.module("in103")
		val mod4: Module = Fixtures.module("in104")
		val mod5: Module = Fixtures.module("in105")
		val mod6: Module = Fixtures.module("in106")
		val mod7: Module = Fixtures.module("in107")

		department.modules.add(mod1)
		department.modules.add(mod2)

		// Mix up the order here to make sure sort() works
		ugDepartment.modules.add(mod4)
		ugDepartment.modules.add(mod5)
		ugDepartment.modules.add(mod3)

		pgDepartment.modules.add(mod6)
		pgDepartment.modules.add(mod7)
	}

	trait Fixture extends SortModulesWorld {
		val command = new SortModulesCommandInternal(department) with CommandTestSupport
	}

	@Test def init { new Fixture {
		command.department should be (department)
		command.departments.toSet should be (Set(department, ugDepartment, pgDepartment))
	}}

	@Test def populateAndSort { new Fixture {
		command.mapping.asScala should be ('empty)

		command.populate()
		command.mapping.asScala.mapValues(_.asScala.toSeq) should be (Map(
			department -> Seq(mod1, mod2),
			ugDepartment -> Seq(mod4, mod5, mod3), // wrong order until .sort() is called
			pgDepartment -> Seq(mod6, mod7)
		))

		command.sort()
		command.mapping.asScala.mapValues(_.asScala.toSeq) should be (Map(
			department -> Seq(mod1, mod2),
			ugDepartment -> Seq(mod3, mod4, mod5),
			pgDepartment -> Seq(mod6, mod7)
		))
	}}

	@Test def mappingByCode { new Fixture {
		command.mappingByCode should be ('empty)

		command.populate()
		command.sort()

		// Mapping by code is also sorted
		command.mappingByCode.mapValues(_.asScala.toSeq) should be (Map(
			"in" -> Seq(mod1, mod2),
			"in-ug" -> Seq(mod3, mod4, mod5),
			"in-pg" -> Seq(mod6, mod7)
		))
	}}

	@Test def apply { new Fixture {
		command.populate()
		command.sort()

		// Move mod1 into in-ug and mod2 into in-pg
		command.mapping.get(ugDepartment).add(mod1)
		command.mapping.get(pgDepartment).add(mod2)
		command.mapping.get(department).clear()

		command.applyInternal()

		mod1.adminDepartment should be (ugDepartment)
		mod2.adminDepartment should be (pgDepartment)

		department.modules.asScala should be ('empty)
		ugDepartment.modules.asScala.toSet should be (Set(mod1, mod3, mod4, mod5))
		pgDepartment.modules.asScala.toSet should be (Set(mod2, mod6, mod7))

		verify(command.moduleAndDepartmentService, times(1)).saveOrUpdate(department)
		verify(command.moduleAndDepartmentService, times(1)).saveOrUpdate(ugDepartment)
		verify(command.moduleAndDepartmentService, times(1)).saveOrUpdate(pgDepartment)
	}}

	trait ValidationFixture extends SortModulesWorld {
		val d: Department = department
		val command = new SortModulesCommandValidation with CommandTestSupport with SortModulesCommandGrouping {
			val department: Department = d
		}

		command.populate()
		command.sort()
	}

	@Test def validateNoErrors { new ValidationFixture {
		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateLeaveRootDeptEmpty { new ValidationFixture {
		val errors = new BindException(command, "command")

		// Move remaining root department modules into in-ug
		command.mapping.get(ugDepartment).add(mod1)
		command.mapping.get(ugDepartment).add(mod2)
		command.mapping.remove(department)

		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def rejectUnrelatedDepartments { new ValidationFixture {
		command.mapping.put(Fixtures.department("other"), JArrayList())

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.getErrorCount should be (1)
		errors.getGlobalError.getCodes should contain ("sortModules.departments.invalid")
	}}

	@Test def rejectUnrelatedModules { new ValidationFixture {
		command.mapping.get(department).add(Fixtures.module("cs205"))

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.getErrorCount should be (1)
		errors.getGlobalError.getCodes should contain ("sortModules.modules.invalid")
	}}

	@Test def rejectOrphanModules { new ValidationFixture {
		command.mapping.get(department).remove(mod1)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.getErrorCount should be (1)
		errors.getGlobalError.getCodes should contain ("sortModules.modules.invalid")
	}}

	@Test
	def permissionsRequireCreateModuleOnDepartment {
		val dept = Fixtures.department("in")

		val command = new SortModulesCommandPermissions with CommandTestSupport {
			val department: Department = dept
			def sort() {}
			def populate() {}
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
		verify(checking, times(1)).PermissionCheck(Permissions.Department.ArrangeRoutesAndModules, dept)
	}

	@Test
	def describe {
		val dept = Fixtures.department("in")

		val command = new SortModulesCommandDescription with CommandTestSupport {
			val eventName: String = "test"
			val department: Department = dept
			def sort() {}
			def populate() {}
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"department" -> "in"
		))
	}

	@Test
	def glueEverythingTogether() {
		val department = Fixtures.department("in", "IT Services")
		val command = SortModulesCommand(department)

		command should be (anInstanceOf[Appliable[Unit]])
		command should be (anInstanceOf[GroupsObjects[Module, Department]])
		command should be (anInstanceOf[SortModulesCommandState])
		command should be (anInstanceOf[SortModulesCommandPermissions])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[SortModulesCommandValidation])
		command should be (anInstanceOf[Describable[Unit]])
	}

}
