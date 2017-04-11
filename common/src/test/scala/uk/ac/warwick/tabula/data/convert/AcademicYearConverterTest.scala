package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.AcademicYear

//scalastyle:off magic.number
class AcademicYearConverterTest extends TestBase {

	val converter = new AcademicYearConverter

	@Test def validInput {
		converter.convertRight("2012") should be (new AcademicYear(2012))
	}

	@Test def invalidInput {
		converter.convertRight("20X6") should be (null)
		converter.convertRight("") should be (null)
		converter.convertRight(null) should be (null)
	}

	@Test def formatting {
		converter.convertLeft(new AcademicYear(2012)) should be ("2012")
		converter.convertLeft(null) should be (null)
	}

}