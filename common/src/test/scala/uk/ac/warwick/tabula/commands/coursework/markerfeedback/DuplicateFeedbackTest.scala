package uk.ac.warwick.tabula.commands.coursework.markerfeedback

import uk.ac.warwick.tabula.TestBase

class DuplicateFeedbackTest extends TestBase with MarkingWorkflowWorld{
	@Test
	def duplicateDetected() {
		assignment.findFeedback("cusxad").isDefined should be (true)
		assignment.findFullFeedback("cusxad").isDefined should be (false)
	}
}
