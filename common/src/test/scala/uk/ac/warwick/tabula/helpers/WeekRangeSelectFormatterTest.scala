package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.services.{TermServiceComponent, TermServiceImpl, TermService}

class WeekRangesSelectFormatterTest extends TestBase {

	implicit val termService = new TermServiceImpl

	private trait TestTermServiceComponent extends TermServiceComponent {
		override def termService: TermService = WeekRangesSelectFormatterTest.this.termService
	}

	@Test def firstTermNumbering = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
		val select = new WeekRangeSelectFormatter(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)) with TestTermServiceComponent

		select.format(Seq(WeekRange(1, 10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be ( Seq(
			EventWeek(1,1), EventWeek(2,2), EventWeek(3,3), EventWeek(4,4), EventWeek(5,5), EventWeek(6,6), EventWeek(7,7), EventWeek(8,8), EventWeek(9,9), EventWeek(10,10)
		))

		select.format(Seq(WeekRange(1, 5), WeekRange(7, 10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be( Seq(
			EventWeek(1,1), EventWeek(2,2), EventWeek(3,3), EventWeek(4,4), EventWeek(5,5), EventWeek(7,7), EventWeek(8,8), EventWeek(9,9), EventWeek(10,10)
		))

		select.format(Seq(WeekRange(1, 3), WeekRange(15, 18)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be( Seq(
			EventWeek(1,1), EventWeek(2,2), EventWeek(3,3)
		))

	}

	@Test def secondTermNumbering = withFakeTime(new DateTime(2012, 2, 12, 13, 36, 44)) {
		val select = new WeekRangeSelectFormatter(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)) with TestTermServiceComponent

		select.format(Seq(WeekRange(1, 10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be ( Seq() )

		select.format(Seq(WeekRange(1, 3), WeekRange(15, 18)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be( Seq(
			EventWeek(1, 15), EventWeek(2, 16), EventWeek(3, 17), EventWeek(4, 18)
		))

	}


	@Test def firstCumulativeTermNumbering = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
		val select = new WeekRangeSelectFormatter(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)) with TestTermServiceComponent

		select.format(Seq(WeekRange(1, 10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be( Seq(
			EventWeek(1,1), EventWeek(2,2), EventWeek(3,3), EventWeek(4,4), EventWeek(5,5), EventWeek(6,6), EventWeek(7,7), EventWeek(8,8), EventWeek(9,9), EventWeek(10,10)
		))

		select.format(Seq(WeekRange(1, 2), WeekRange(5, 9)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be( Seq(
			EventWeek(1, 1), EventWeek(2, 2), EventWeek(5, 5), EventWeek(6, 6), EventWeek(7, 7), EventWeek(8, 8), EventWeek(9, 9)
		))

	}

	@Test def secondCumulativeTermNumbering = withFakeTime(new DateTime(2012, 1, 12, 13, 36, 44)) {
		val select = new WeekRangeSelectFormatter(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)) with TestTermServiceComponent

		select.format(Seq(WeekRange(15, 19)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be( Seq(
			EventWeek(11, 15), EventWeek(12, 16), EventWeek(13, 17), EventWeek(14, 18), EventWeek(15, 19)
		))

		select.format(Seq(WeekRange(16, 18), WeekRange(20, 21)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be( Seq(
			EventWeek(12, 16), EventWeek(13, 17), EventWeek(14, 18), EventWeek(16, 20), EventWeek(17, 21)
		))

	}

	@Test def thirdCumulativeTermNumbering = withFakeTime(new DateTime(2012, 5, 12, 13, 36, 44)) {
		val select = new WeekRangeSelectFormatter(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)) with TestTermServiceComponent

		select.format(Seq(WeekRange(29, 32)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be( Seq(
			EventWeek(21, 30), EventWeek(22, 31), EventWeek(23, 32)
		))


		select.format(Seq(WeekRange(1, 10), WeekRange(29, 32)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be( Seq(
			EventWeek(21, 30), EventWeek(22, 31), EventWeek(23, 32)
		))

	}



	@Test def academicWeekNumbering = withFakeTime(new DateTime(2012, 5, 12, 13, 36, 44)) {
		val select = new WeekRangeSelectFormatter(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)) with TestTermServiceComponent

		select.format(Seq(WeekRange(30, 32)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic) should be( Seq(
			EventWeek(30, 30), EventWeek(31, 31), EventWeek(32, 32)
		))

		select.format(Seq(WeekRange(41, 42)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic) should be( Seq(
			EventWeek(41, 41), EventWeek(42, 42)
		))

	}


}
