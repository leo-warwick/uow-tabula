package uk.ac.warwick.tabula.web.controllers.coursework

import uk.ac.warwick.tabula.data.model.{AssignmentFeedback, Feedback}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.web.controllers.coursework.admin.OldFeedbackSummaryController

class OldFeedbackControllersTest extends TestBase with Mockito {
	val controller = new OldFeedbackSummaryController
	val command: Appliable[Option[Feedback]] = mock[Appliable[Option[Feedback]]]
	val feedback = new AssignmentFeedback
	command.apply() returns Some(feedback)

	@Test
	def display() {
		controller.showForm(command) should be (Mav("${cm1.prefix}/admin/assignments/feedback/read_only",
			"embedded" -> true,
			"renderLayout" -> "none",
			"feedback" -> Some(feedback)))
	}
}