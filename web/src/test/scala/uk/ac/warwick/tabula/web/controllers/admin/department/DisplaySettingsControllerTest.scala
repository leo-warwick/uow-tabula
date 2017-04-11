package uk.ac.warwick.tabula.web.controllers.admin.department

import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{ItemNotFoundException, Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.commands.{PopulateOnForm, Appliable}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.services.permissions.PermissionsServiceComponent
import org.springframework.validation.BindException

class DisplaySettingsControllerTest extends TestBase with Mockito {

	val controller = new DisplaySettingsController
	controller.relationshipService = mock[RelationshipService]

	@Test def createsCommand {
		val department = Fixtures.department("in")

		val command = controller.displaySettingsCommand(department)

		command should be (anInstanceOf[Appliable[Department]])
		command should be (anInstanceOf[PopulateOnForm])
	}

	@Test(expected = classOf[ItemNotFoundException]) def requiresDepartment {
		controller.displaySettingsCommand(null)
	}

	@Test def form {
		val department = Fixtures.department("in")

		var populateCalledCount = 0
		val command = new Appliable[Department] with PopulateOnForm with PermissionsServiceComponent {
			val permissionsService = null
			def populate() {
				populateCalledCount += 1
			}
			def apply(): Null = {
				fail("Should not be called")
				null
			}
		}

		val mav = controller.initialView(department, command)
		mav.viewName should be ("admin/display-settings")
		mav.toModel("department") should be (department)
		mav.toModel("returnTo") should be ("")

		populateCalledCount should be (1)
	}

	@Test def submit {
		val department = Fixtures.department("in")

		var populateCalledCount = 0
		var applyCalledCount = 0
		val command = new Appliable[Department] with PopulateOnForm with PermissionsServiceComponent {
			val permissionsService = null
			def populate() {
				populateCalledCount += 1
			}
			def apply(): Department = {
				applyCalledCount += 1
				department
			}
		}

		val errors = new BindException(command, "command")

		val mav = controller.saveSettings(command, errors, department)
		mav.viewName should be (s"redirect:${Routes.admin.department(department)}")
		mav.toModel should be ('empty)

		populateCalledCount should be (0)
		applyCalledCount should be (1)
	}

	@Test def submitValidationErrors {
		val department = Fixtures.department("in")

		var populateCalledCount = 0
		var applyCalledCount = 0
		val command = new Appliable[Department] with PopulateOnForm with PermissionsServiceComponent {
			val permissionsService = null
			def populate() {
				populateCalledCount += 1
			}
			def apply(): Department = {
				applyCalledCount += 1
				department
			}
		}

		val errors = new BindException(command, "command")
		errors.reject("fail")

		val mav = controller.saveSettings(command, errors, department)
		mav.viewName should be ("admin/display-settings")
		mav.toModel("department") should be (department)
		mav.toModel("returnTo") should be ("")

		populateCalledCount should be (0)
		applyCalledCount should be (0)
	}

}
