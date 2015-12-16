package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.MarkerFeedback
import uk.ac.warwick.tabula.data.model.MarkingState._

class StateServiceTest extends TestBase with Mockito {

	val service = new StateServiceImpl
	service.feedbackService = mock[FeedbackService]

	@Test
	def nullState {
		val markerFeedback = new MarkerFeedback
		service.updateState(markerFeedback, MarkingCompleted)
		markerFeedback.state should be (MarkingCompleted)
	}

}