package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.MarkingState._

class MarkingStateUserTypeTest extends TestBase {

	@Test def convertToObject() {
		val t = new MarkingStateUserType
		t.convertToObject("ReleasedForMarking") should be (ReleasedForMarking)
		t.convertToObject("InProgress") should be (InProgress)
		t.convertToObject("MarkingCompleted") should be (MarkingCompleted)
		t.convertToObject("Rejected") should be (Rejected)
		an [IllegalArgumentException] should be thrownBy { t.convertToObject("Q") }
	}

	@Test def convertToValue() {
		val t = new MarkingStateUserType
		t.convertToValue(ReleasedForMarking) should be ("ReleasedForMarking")
		t.convertToValue(InProgress) should be ("InProgress")
		t.convertToValue(MarkingCompleted) should be ("MarkingCompleted")
		t.convertToValue(Rejected) should be ("Rejected")
	}

}