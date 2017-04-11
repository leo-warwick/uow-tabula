package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.groups.{WeekRange, DayOfWeek}
import uk.ac.warwick.tabula.services.{TermService, TermServiceComponent, TermServiceImpl}

class WholeWeekFormatterTest extends TestBase {

	implicit val termService = new TermServiceImpl

	private trait TestTermServiceComponent extends TermServiceComponent {
		override def termService: TermService = WholeWeekFormatterTest.this.termService
	}

	@Test def termNumbering() = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
		val formatter = new WholeWeekFormatter(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)) with TestTermServiceComponent

		formatter.format(Seq(WeekRange(1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = false) should be("Term 1, week 1")
		formatter.format(Seq(WeekRange(7)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = false) should be("Term 1, week 7")
		formatter.format(Seq(WeekRange(14)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = false) should be("Christmas vacation, w/c Mon 2<sup>nd</sup> Jan 2012")
		formatter.format(Seq(WeekRange(25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = false) should be("Easter vacation, w/c Mon 19<sup>th</sup> Mar 2012")

		formatter.format(Seq(WeekRange(1, 1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = false) should be("Term 1, week 1")
		formatter.format(Seq(WeekRange(1, 2)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = false) should be("Term 1, weeks 1-2")
		formatter.format(Seq(WeekRange(1, 14)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = false) should be(
			"Term 1, weeks 1-10; Christmas vacation, w/c Mon 12<sup>th</sup> Dec 2011 - w/c Mon 2<sup>nd</sup> Jan 2012"
		)

		formatter.format(Seq(WeekRange(1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = true) should be("1")
		formatter.format(Seq(WeekRange(7)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = true) should be("7")
		formatter.format(Seq(WeekRange(14)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = true) should be("02/01")
		formatter.format(Seq(WeekRange(25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = true) should be("19/03")
	}



	@Test def cumulativeTermNumbering() = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
		val formatter = new WholeWeekFormatter(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)) with TestTermServiceComponent

		formatter.format(Seq(WeekRange(4)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = false) should be("Term 1, week 4")
		formatter.format(Seq(WeekRange(10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = false) should be("Term 1, week 10")
		formatter.format(Seq(WeekRange(13)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = false) should be("Christmas vacation, w/c Mon 26<sup>th</sup> Dec 2011")
		formatter.format(Seq(WeekRange(15)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = false) should be("Term 2, week 11")
		formatter.format(Seq(WeekRange(25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = false) should be("Easter vacation, w/c Mon 19<sup>th</sup> Mar 2012")

		formatter.format(Seq(WeekRange(1, 1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = false) should be("Term 1, week 1")
		formatter.format(Seq(WeekRange(1, 2)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = false) should be("Term 1, weeks 1-2")
		formatter.format(Seq(WeekRange(1, 14)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = false) should be(
			"Term 1, weeks 1-10; Christmas vacation, w/c Mon 12<sup>th</sup> Dec 2011 - w/c Mon 2<sup>nd</sup> Jan 2012"
		)

		formatter.format(Seq(WeekRange(4)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = true) should be("4")
		formatter.format(Seq(WeekRange(10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = true) should be("10")
		formatter.format(Seq(WeekRange(13)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = true) should be("26/12")
		formatter.format(Seq(WeekRange(15)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = true) should be("11")
		formatter.format(Seq(WeekRange(25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = true) should be("19/03")
	}


	@Test def academicWeekNumbering() = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
		val formatter = new WholeWeekFormatter(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)) with TestTermServiceComponent

		formatter.format(Seq(WeekRange(1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Term 1, week 1")
		formatter.format(Seq(WeekRange(7)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Term 1, week 7")
		formatter.format(Seq(WeekRange(16)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Term 2, week 16")
		formatter.format(Seq(WeekRange(52)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Summer vacation, week 52")
		formatter.format(Seq(WeekRange(14)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Christmas vacation, week 14")
		formatter.format(Seq(WeekRange(25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Easter vacation, week 25")

		formatter.format(Seq(WeekRange(1, 1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Term 1, week 1")
		formatter.format(Seq(WeekRange(1, 2)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Term 1, weeks 1-2")
		formatter.format(Seq(WeekRange(1, 14)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Term 1, weeks 1-10; Christmas vacation, weeks 11-14")

		formatter.format(Seq(WeekRange(1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = true) should be("1")
		formatter.format(Seq(WeekRange(7)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = true) should be("7")
		formatter.format(Seq(WeekRange(16)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = true) should be("16")
		formatter.format(Seq(WeekRange(52)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = true) should be("52")
		formatter.format(Seq(WeekRange(14)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = true) should be("14")
		formatter.format(Seq(WeekRange(25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = true) should be("25")
	}

	@Test def noWeekNumbers() = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
		val formatter = new WholeWeekFormatter(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)) with TestTermServiceComponent

		formatter.format(Seq(WeekRange(1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = false) should be("w/c Mon 3<sup>rd</sup> Oct 2011")
		formatter.format(Seq(WeekRange(5)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = false) should be("w/c Mon 31<sup>st</sup> Oct 2011")
		formatter.format(Seq(WeekRange(16)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = false) should be("w/c Mon 16<sup>th</sup> Jan 2012")
		formatter.format(Seq(WeekRange(52)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = false) should be("w/c Mon 24<sup>th</sup> Sep 2012")
		formatter.format(Seq(WeekRange(25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = false) should be("w/c Mon 19<sup>th</sup> Mar 2012")

		formatter.format(Seq(WeekRange(1, 1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = false) should be("w/c Mon 3<sup>rd</sup> Oct 2011")
		formatter.format(Seq(WeekRange(1, 2)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = false) should be("w/c Mon 3<sup>rd</sup> - w/c Mon 10<sup>th</sup> Oct 2011")
		formatter.format(Seq(WeekRange(1, 14)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = false) should be(
			"w/c Mon 3<sup>rd</sup> Oct 2011 - w/c Mon 2<sup>nd</sup> Jan 2012"
		)

		formatter.format(Seq(WeekRange(1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = true) should be("03/10")
		formatter.format(Seq(WeekRange(5)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = true) should be("31/10")
		formatter.format(Seq(WeekRange(16)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = true) should be("16/01")
		formatter.format(Seq(WeekRange(52)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = true) should be("24/09")
		formatter.format(Seq(WeekRange(25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = true) should be("19/03")

	}


}
