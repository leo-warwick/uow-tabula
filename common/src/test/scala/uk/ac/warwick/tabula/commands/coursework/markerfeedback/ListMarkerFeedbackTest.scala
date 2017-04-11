package uk.ac.warwick.tabula.commands.coursework.markerfeedback

import uk.ac.warwick.tabula.commands.coursework.assignments.ListMarkerFeedbackCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.UserLookupComponent
import uk.ac.warwick.tabula.{MockUserLookup, Mockito, TestBase}

import scala.collection.JavaConversions._


class ListMarkerFeedbackTest extends TestBase with MarkingWorkflowWorld with Mockito {

	trait CommandTestSupport extends UserLookupComponent {
		val userLookup = new MockUserLookup
	}

	assignment.markingWorkflow.userLookup = mockUserLookup

	@Test
	def firstMarkerTest() {
		withUser("cuslaj") {
			val command =	new ListMarkerFeedbackCommand(assignment, assignment.module, currentUser.apparentUser, currentUser) with CommandTestSupport
			val markerFeedbackCollections = command.applyInternal()

			markerFeedbackCollections.head.feedbackItems.size should be (3)
		}
		withUser("cuscav") {
			val command =	new ListMarkerFeedbackCommand(assignment, assignment.module, currentUser.apparentUser, currentUser) with CommandTestSupport
			val markerFeedbackCollections = command.applyInternal()

			markerFeedbackCollections.head.feedbackItems.size should be (2)
		}
	}

	@Test
	def secondMarkerTest() {
		assignment.feedbacks.foreach{feedback =>
			val fmFeedback = new MarkerFeedback(feedback)
			feedback.firstMarkerFeedback = fmFeedback
			fmFeedback.state = MarkingState.MarkingCompleted
			val smFeedback = new MarkerFeedback(feedback)
			feedback.secondMarkerFeedback = smFeedback
			smFeedback.state = MarkingState.ReleasedForMarking
		}

		withUser("cuslat") {
			val command =	new ListMarkerFeedbackCommand(assignment, assignment.module, currentUser.apparentUser, currentUser) with CommandTestSupport
			val markerFeedbackCollections = command.applyInternal()

			markerFeedbackCollections.head.feedbackItems.size should be (3)
		}
		withUser("cuday") {
			val command =	new ListMarkerFeedbackCommand(assignment, assignment.module, currentUser.apparentUser, currentUser) with CommandTestSupport
			val markerFeedbackCollections = command.applyInternal()

			markerFeedbackCollections.head.feedbackItems.size should be (2)
		}
	}
}
