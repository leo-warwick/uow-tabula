package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import org.joda.time.{LocalDate, DateTime}

class JodaLocalDateConverterTest extends TestBase{
	val converter = new JodaLocalDateConverter

	@Test def validInput {
		converter.convertRight("10-Mar-2012") should be (new LocalDate(2012,3,10))
	}

	@Test def invalidInput {
		converter.convertRight("5th April 1996") should be (null)
		converter.convertRight("") should be (null)
		converter.convertRight(null) should be (null)
	}

	@Test def formatting {
		converter.convertLeft(new LocalDate(2013,12,31)) should be ("31-Dec-2013")
		converter.convertLeft(null) should be (null)
	}

}
