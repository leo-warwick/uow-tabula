package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.AddressType._

class AddressTypeTest extends TestBase {

	@Test def convertToObject() {
		val t = new AddressTypeUserType
		t.convertToObject("H") should be (Home)
		t.convertToObject("C") should be (TermTime)
		an [IllegalArgumentException] should be thrownBy { t.convertToObject("Q") }
	}

	@Test def convertToValue() {
		val t = new AddressTypeUserType
		t.convertToValue(Home) should be ("H")
		t.convertToValue(TermTime) should be ("C")
	}

}