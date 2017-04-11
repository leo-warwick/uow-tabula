package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.Department

class ClassConverterTest extends TestBase {

	val converter = new ClassConverter

	@Test def validInput {
		converter.convertRight("uk.ac.warwick.tabula.data.model.Department") should be (classOf[Department])
	}

	@Test def invalidInput {
		converter.convertRight("20X6") should be (null)
		converter.convertRight("") should be (null)
	}

	@Test def formatting {
		converter.convertLeft(classOf[Department]) should be ("uk.ac.warwick.tabula.data.model.Department")
		converter.convertLeft(null) should be (null)
	}

}