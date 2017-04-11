package uk.ac.warwick.tabula.data.model.groups

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.util.termdates.TermFactoryImpl
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.{TermServiceImpl, TermService}

class WeekRangeTest extends TestBase {

	implicit val termService = new TermServiceImpl

	@Test
	def fromString {
		WeekRange.fromString("3") should be (WeekRange(3))
		WeekRange.fromString("3-3") should be (WeekRange(3))
		WeekRange.fromString("3-10") should be (WeekRange(3,10))
		WeekRange.fromString("3  -  10") should be (WeekRange(3,10))
	}

	@Test
	def combine {
		WeekRange.combine(Seq()) should be (Seq())
		WeekRange.combine(Seq(1)) should be (Seq(WeekRange(1)))
		WeekRange.combine(Seq(1,2)) should be (Seq(WeekRange(1, 2)))
		WeekRange.combine(Seq(2,1)) should be (Seq(WeekRange(1, 2)))
		WeekRange.combine(Seq(1,2,3)) should be (Seq(WeekRange(1, 3)))
		WeekRange.combine(Seq(3,6,9,11,4,5,1)) should be (Seq(WeekRange(1), WeekRange(3, 6), WeekRange(9), WeekRange(11)))
	}

	@Test
	def termWeekRanges {
		WeekRange.termWeekRanges(AcademicYear.parse("11/12")) should be (Seq(WeekRange(1, 10), WeekRange(15, 24), WeekRange(30, 39)))
		WeekRange.termWeekRanges(AcademicYear.parse("12/13")) should be (Seq(WeekRange(1, 10), WeekRange(15, 24), WeekRange(30, 39)))
		WeekRange.termWeekRanges(AcademicYear.parse("13/14")) should be (Seq(WeekRange(1, 10), WeekRange(15, 24), WeekRange(30, 39)))
		WeekRange.termWeekRanges(AcademicYear.parse("14/15")) should be (Seq(WeekRange(1, 10), WeekRange(15, 24), WeekRange(30, 39)))
		WeekRange.termWeekRanges(AcademicYear.parse("15/16")) should be (Seq(WeekRange(1, 10), WeekRange(15, 24), WeekRange(30, 39)))
	}

}