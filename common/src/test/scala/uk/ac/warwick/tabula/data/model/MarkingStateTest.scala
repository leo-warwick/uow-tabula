package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.MarkingState._

class MarkingStateTest extends TestBase with Mockito {

	// test all valid state transitions
	@Test def stateTransitionTest() {
		MarkingCompleted.canTransitionTo(InProgress) should be (false)
		MarkingCompleted.canTransitionTo(Rejected) should be (true)
		ReleasedForMarking.canTransitionTo(InProgress) should be (true)
		ReleasedForMarking.canTransitionTo(MarkingCompleted) should be (true)
		Rejected.canTransitionTo(ReleasedForMarking) should be (true)
		Rejected.canTransitionTo(MarkingCompleted) should be (true)
		InProgress.canTransitionTo(MarkingCompleted) should be (true)
	}

}
