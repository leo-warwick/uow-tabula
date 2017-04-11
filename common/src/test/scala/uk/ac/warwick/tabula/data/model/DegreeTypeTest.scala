package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.DegreeType._

class DegreeTypeTest extends TestBase {

	@Test def convertToObject() {
		val t = new DegreeTypeUserType
		t.convertToObject("UG") should be (Undergraduate)
		t.convertToObject("PG") should be (Postgraduate)
		t.convertToObject("IS") should be (InService)
		t.convertToObject("PGCE") should be (PGCE)
		an [IllegalArgumentException] should be thrownBy { t.convertToObject("Q") }
	}

	@Test def convertToValue() {
		val t = new DegreeTypeUserType
		t.convertToValue(Undergraduate) should be ("UG")
		t.convertToValue(Postgraduate) should be ("PG")
		t.convertToValue(InService) should be ("IS")
		t.convertToValue(PGCE) should be ("PGCE")
	}

	@Test def getFromSchemeCode(): Unit = {
		DegreeType.getFromSchemeCode("UW UG") should be (Undergraduate)
		DegreeType.getFromSchemeCode("UW PG") should be (Postgraduate)
		DegreeType.getFromSchemeCode("X") should be (null)
	}

}