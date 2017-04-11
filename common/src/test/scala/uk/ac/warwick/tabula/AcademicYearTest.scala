package uk.ac.warwick.tabula
import org.junit.Test
import org.joda.time.DateTime

//scalastyle:off magic.number
class AcademicYearTest extends TestBase {
	@Test def year {
		AcademicYear.guessSITSAcademicYearByDate(dateTime(2010,11)).startYear should be(2010)
		AcademicYear.guessSITSAcademicYearByDate(dateTime(2010,5)).startYear should be(2009)
	}

	@Test def strings {
		AcademicYear(2011).toString should be ("11/12")
		AcademicYear(1999).toString should be ("99/00")

		(AcademicYear(2012) + 5) should be (AcademicYear(2017))
		(AcademicYear(2012) - 10) should be (AcademicYear(2002))
	}

	@Test def range {
		AcademicYear(2001).yearsSurrounding(2, 4) should be (Seq(
				AcademicYear(1999),
				AcademicYear(2000),
				AcademicYear(2001),
				AcademicYear(2002),
				AcademicYear(2003),
				AcademicYear(2004),
				AcademicYear(2005)
		))
	}

	@Test def parse {
		AcademicYear.parse("05/06") should be (AcademicYear(2005))
		AcademicYear.parse("99/00") should be (AcademicYear(1999))
		intercept[IllegalArgumentException] {
			AcademicYear.parse("05") should be (AcademicYear(1999))
		}
	}

	@Test(expected=classOf[IllegalArgumentException]) def tooHigh {
		AcademicYear(9999)
	}

	@Test(expected=classOf[IllegalArgumentException]) def tooLow {
		AcademicYear(999)
	}

}
