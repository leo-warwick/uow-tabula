package uk.ac.warwick.tabula.web.controllers.admin.routes

import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.admin.AdminBreadcrumbs
import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, Mockito, TestBase}
import uk.ac.warwick.tabula.commands.admin.routes.SortRoutesCommandState
import uk.ac.warwick.tabula.data.model.{Department, Route}
import uk.ac.warwick.tabula.commands.{Appliable, GroupsObjects}
import org.springframework.validation.BindException

class SortRoutesControllerTest extends TestBase with Mockito {

	val controller = new SortRoutesController

	@Test def createsCommand() {
		val department = Fixtures.department("in")

		val command = controller.command(department)

		command should be (anInstanceOf[Appliable[Unit]])
	}

	@Test(expected = classOf[ItemNotFoundException]) def requiresDepartment() {
		controller.command(null)
	}

	class CountingCommand(val department: Department) extends Appliable[Unit] with GroupsObjects[Route, Department] with SortRoutesCommandState {
		var populateCount = 0
		var sortCount = 0
		var applyCount = 0

		def populate() { populateCount += 1 }
		def sort() { sortCount += 1 }
		def apply() { applyCount += 1 }
	}

	trait Fixture {
		val department: Department = Fixtures.department("in")
		val subDepartment: Department = Fixtures.department("in-ug")
		department.children.add(subDepartment)
		subDepartment.parent = department
	}

	@Test def formOnParent() { new Fixture {
		val command = new CountingCommand(department)
		val mav: Mav = controller.showForm(command)
		mav.viewName should be ("admin/routes/arrange/form")
		mav.toModel should be (Map("breadcrumbs" -> Seq(AdminBreadcrumbs.Department(department))))

		command.populateCount should be (1)
		command.sortCount should be (1)
		command.applyCount should be (0)
	}}

	@Test def formOnChild() { new Fixture {
		val command = new CountingCommand(subDepartment)
		val mav: Mav = controller.showForm(command)
		mav.viewName should be (s"redirect:${Routes.admin.department.sortRoutes(department)}")
		mav.toModel should be ('empty)

		command.populateCount should be (1)
		command.sortCount should be (1)
		command.applyCount should be (0)
	}}

	@Test def formOnLoneDepartment() { new Fixture {
		val command = new CountingCommand(Fixtures.department("xx"))
		val mav: Mav = controller.showForm(command)
		mav.viewName should be ("admin/routes/arrange/form")
		mav.toModel should be (Map("breadcrumbs" -> Seq(AdminBreadcrumbs.Department(command.department))))

		command.populateCount should be (1)
		command.sortCount should be (1)
		command.applyCount should be (0)
	}}

	@Test def submitParent() { new Fixture {
		val command = new CountingCommand(department)
		val errors = new BindException(command, "command")

		val mav: Mav = controller.submit(command, errors)
		mav.viewName should be ("admin/routes/arrange/form")
		mav.toModel("saved").toString should be ("true")

		command.populateCount should be (0)
		command.sortCount should be (1)
		command.applyCount should be (1)
	}}

	@Test def submitValidationErrors() { new Fixture {
		val command = new CountingCommand(department)
		val errors = new BindException(command, "command")
		errors.reject("fail")

		val mav: Mav = controller.submit(command, errors)
		mav.viewName should be ("admin/routes/arrange/form")
		mav.toModel should be (Map("breadcrumbs" -> Seq(AdminBreadcrumbs.Department(department))))

		command.populateCount should be (0)
		command.sortCount should be (1)
		command.applyCount should be (0)
	}}

}
