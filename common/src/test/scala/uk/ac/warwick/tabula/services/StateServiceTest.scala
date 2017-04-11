package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.MarkerFeedback
import uk.ac.warwick.tabula.Mockito
import org.hibernate.Session
import uk.ac.warwick.tabula.data.model.MarkingState._

class StateServiceTest extends TestBase with Mockito {

	val mockSession: Session = mock[Session]
	val service = new StateServiceImpl {
		override def session: Session = mockSession
	}

	@Test
	def nullState {
		val markerFeedback = new MarkerFeedback
		service.updateState(markerFeedback, MarkingCompleted)
		markerFeedback.state should be (MarkingCompleted)
	}

}