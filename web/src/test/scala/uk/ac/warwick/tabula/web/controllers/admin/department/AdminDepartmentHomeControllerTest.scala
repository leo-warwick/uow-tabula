package uk.ac.warwick.tabula.web.controllers.admin.department

import uk.ac.warwick.tabula.{FunctionalContextTesting, Fixtures, CurrentUser, TestBase, Mockito}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.{Route, Module}
import uk.ac.warwick.tabula.commands.admin.department.{AdminDepartmentHomeCommandTest, AdminDepartmentHomeCommandState}

class AdminDepartmentHomeControllerTest extends TestBase with Mockito with FunctionalContextTesting {

	val controller = new AdminDepartmentHomeController

	@Test def createsCommand { inContext[AdminDepartmentHomeCommandTest.MinimalCommandContext] {
		val department = Fixtures.department("in", "IT Services")
		val user = mock[CurrentUser]

		val command = controller.command(department, user)

		command should be (anInstanceOf[Appliable[(Seq[Module], Seq[Route])]])
		command should be (anInstanceOf[AdminDepartmentHomeCommandState])

		command.asInstanceOf[AdminDepartmentHomeCommandState].department should be (department)
		command.asInstanceOf[AdminDepartmentHomeCommandState].user should be (user)
	}}

	@Test def adminPage {
		val department = Fixtures.department("in", "IT Services")
		val modules = Seq(Fixtures.module("in101"), Fixtures.module("in102"), Fixtures.module("in103"))
		val routes = Seq(Fixtures.route("i100"), Fixtures.route("i200"), Fixtures.route("i300"))

		val command = mock[Appliable[(Seq[Module], Seq[Route])]]
		command.apply() returns ((modules, routes))

		val mav = controller.adminDepartment(command, department)
		mav.viewName should be ("admin/department")
		mav.toModel should be (Map(
			"department" -> department,
			"modules" -> modules,
			"departmentRoutes" -> routes
		))
	}

}