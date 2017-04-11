package uk.ac.warwick.tabula.web.controllers.coursework

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.coursework.feedback.GenericFeedbackCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Department, Module}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.coursework.admin.OldGenericFeedbackController
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class OldGenericFeedbackControllerTest extends TestBase with Mockito {

	trait Fixture {
		val department: Department = Fixtures.department("hz", "Heron studies")
		val module = new Module
		module.code = "hn101"
		module.adminDepartment = department
		val assignment = new Assignment
		assignment.module = module

		val command: GenericFeedbackCommand = mock[GenericFeedbackCommand]
	}

	@Test def controllerShowsForm() {
		new Fixture {
			val controller = new OldGenericFeedbackController
			controller.urlPrefix = "coursework"
			val mav: Mav = controller.showForm(assignment, command, null)
			mav.map("command") should be(command)
			mav.viewName should be ("coursework/admin/assignments/feedback/generic_feedback")
		}
	}

	@Test def controllerAppliesCommand() {
		new Fixture {
			val controller = new OldGenericFeedbackController { override val ajax = true }
			val errors: Errors = mock[Errors]

			val mav: Mav = controller.submit(assignment, command, errors)

			verify(command, times(1)).apply()

			mav.viewName should be ("ajax_success")
		}
	}

}
