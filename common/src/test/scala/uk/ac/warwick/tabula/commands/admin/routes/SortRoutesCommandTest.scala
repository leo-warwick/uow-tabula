package uk.ac.warwick.tabula.commands.admin.routes

import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.{Fixtures, TestBase, Mockito}
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.commands.{Describable, SelfValidating, Appliable, DescriptionImpl, GroupsObjects}
import uk.ac.warwick.tabula.data.model.{Route, Department}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions

class SortRoutesCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends SortRoutesCommandState with ModuleAndDepartmentServiceComponent {
		val moduleAndDepartmentService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]
	}

	trait SortRoutesWorld {
		val department: Department = Fixtures.department("in", "IT Services")
		val ugDepartment: Department = Fixtures.department("in-ug", "ITS Undergraduates")
		val pgDepartment: Department = Fixtures.department("in-pg", "ITS Postgraduates")

		department.children.add(ugDepartment)
		department.children.add(pgDepartment)

		ugDepartment.parent = department
		pgDepartment.parent = department

		val route1: Route = Fixtures.route("in101")
		val route2: Route = Fixtures.route("in102")
		val route3: Route = Fixtures.route("in103")
		val route4: Route = Fixtures.route("in104")
		val route5: Route = Fixtures.route("in105")
		val route6: Route = Fixtures.route("in106")
		val route7: Route = Fixtures.route("in107")

		department.routes.add(route1)
		department.routes.add(route2)

		// Mix up the order here to make sure sort() works
		ugDepartment.routes.add(route4)
		ugDepartment.routes.add(route5)
		ugDepartment.routes.add(route3)

		pgDepartment.routes.add(route6)
		pgDepartment.routes.add(route7)
	}

	trait Fixture extends SortRoutesWorld {
		val command = new SortRoutesCommandInternal(department) with CommandTestSupport
	}

	@Test def init { new Fixture {
		command.department should be (department)
		command.departments.toSet should be (Set(department, ugDepartment, pgDepartment))
	}}

	@Test def populateAndSort { new Fixture {
		command.mapping.asScala should be ('empty)

		command.populate()
		command.mapping.asScala.mapValues(_.asScala.toSeq) should be (Map(
			department -> Seq(route1, route2),
			ugDepartment -> Seq(route4, route5, route3), // wrong order until .sort() is called
			pgDepartment -> Seq(route6, route7)
		))

		command.sort()
		command.mapping.asScala.mapValues(_.asScala.toSeq) should be (Map(
			department -> Seq(route1, route2),
			ugDepartment -> Seq(route3, route4, route5),
			pgDepartment -> Seq(route6, route7)
		))
	}}

	@Test def mappingByCode { new Fixture {
		command.mappingByCode should be ('empty)

		command.populate()
		command.sort()

		// Mapping by code is also sorted
		command.mappingByCode.mapValues(_.asScala.toSeq) should be (Map(
			"in" -> Seq(route1, route2),
			"in-ug" -> Seq(route3, route4, route5),
			"in-pg" -> Seq(route6, route7)
		))
	}}

	@Test def apply { new Fixture {
		command.populate()
		command.sort()

		// Move route1 into in-ug and route2 into in-pg
		command.mapping.get(ugDepartment).add(route1)
		command.mapping.get(pgDepartment).add(route2)
		command.mapping.get(department).clear()

		command.applyInternal()

		route1.adminDepartment should be (ugDepartment)
		route2.adminDepartment should be (pgDepartment)

		department.routes.asScala should be ('empty)
		ugDepartment.routes.asScala.toSet should be (Set(route1, route3, route4, route5))
		pgDepartment.routes.asScala.toSet should be (Set(route2, route6, route7))

		verify(command.moduleAndDepartmentService, times(1)).saveOrUpdate(department)
		verify(command.moduleAndDepartmentService, times(1)).saveOrUpdate(ugDepartment)
		verify(command.moduleAndDepartmentService, times(1)).saveOrUpdate(pgDepartment)
	}}

	trait ValidationFixture extends SortRoutesWorld {
		val d: Department = department
		val command = new SortRoutesCommandValidation with CommandTestSupport with SortRoutesCommandGrouping {
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

	@Test def rejectUnrelatedDepartments { new ValidationFixture {
		command.mapping.put(Fixtures.department("other"), JArrayList())

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.getErrorCount should be (1)
		errors.getGlobalError.getCodes should contain ("sortRoutes.departments.invalid")
	}}

	@Test def rejectUnrelatedRoutes { new ValidationFixture {
		command.mapping.get(department).add(Fixtures.route("cs205"))

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.getErrorCount should be (1)
		errors.getGlobalError.getCodes should contain ("sortRoutes.routes.invalid")
	}}

	@Test def rejectOrphanRoutes { new ValidationFixture {
		command.mapping.get(department).remove(route1)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.getErrorCount should be (1)
		errors.getGlobalError.getCodes should contain ("sortRoutes.routes.invalid")
	}}

	@Test
	def permissionsRequireCreateRouteOnDepartment {
		val dept = Fixtures.department("in")

		val command = new SortRoutesCommandPermissions with CommandTestSupport {
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

		val command = new SortRoutesCommandDescription with CommandTestSupport {
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
		val command = SortRoutesCommand(department)

		command should be (anInstanceOf[Appliable[Unit]])
		command should be (anInstanceOf[GroupsObjects[Route, Department]])
		command should be (anInstanceOf[SortRoutesCommandState])
		command should be (anInstanceOf[SortRoutesCommandPermissions])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[SortRoutesCommandValidation])
		command should be (anInstanceOf[Describable[Unit]])
	}

}
