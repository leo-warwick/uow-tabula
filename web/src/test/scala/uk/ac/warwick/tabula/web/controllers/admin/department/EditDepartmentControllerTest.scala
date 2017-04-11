package uk.ac.warwick.tabula.web.controllers.admin.department

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.admin.department.EditDepartmentCommandState
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{ItemNotFoundException, Fixtures, Mockito, TestBase}

class EditDepartmentControllerTest extends TestBase with Mockito {

	val controller = new EditDepartmentController

	@Test def createsCommand {
		val department = Fixtures.department("in")

		val command = controller.command(department)
		command.department should be (department)

		command should be (anInstanceOf[Appliable[Department]])
	}

	@Test(expected = classOf[ItemNotFoundException]) def requiresDepartment {
		controller.command(null)
	}

	@Test def form {
		controller.showForm().viewName should be("admin/department/edit/form")
	}

	@Test def submit {
		val department = Fixtures.department("in-ug")
		val command = mock[Appliable[Department] with EditDepartmentCommandState]
		command.apply() returns (department)

		val errors = new BindException(command, "command")

		controller.submit(command, errors).viewName should be (s"redirect:${Routes.admin.department(department)}")

		verify(command, times(1)).apply()
	}

	@Test def validation {
		val command = mock[Appliable[Department] with EditDepartmentCommandState]
		val errors = new BindException(command, "command")
		errors.reject("error")

		controller.submit(command, errors).viewName should be("admin/department/edit/form")

		verify(command, times(0)).apply()
	}

}