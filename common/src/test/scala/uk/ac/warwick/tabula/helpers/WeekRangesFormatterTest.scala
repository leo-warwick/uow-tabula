package uk.ac.warwick.tabula.helpers

import org.joda.time.DateTime
import uk.ac.warwick.tabula.{AcademicYear, TestBase}
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}

class WeekRangesFormatterTest extends TestBase {

  @Test def termNumbering = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
    val formatter = new WeekRangesFormatter(AcademicYear.now())

    formatter.format(Seq(WeekRange(1, 10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be("Term 1, weeks 1-10")
    formatter.format(Seq(WeekRange(1, 5), WeekRange(7, 10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be("Term 1, weeks 1-5; Term 1, weeks 7-10")
    formatter.format(Seq(WeekRange(2, 16)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be("Term 1, weeks 2-10; Christmas vacation, Tue 13<sup>th</sup> Dec 2011 - Tue 3<sup>rd</sup> Jan 2012; Term 2, weeks 1-2")
    formatter.format(Seq(WeekRange(-8, 44)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be("Pre-term vacation, Tue 2<sup>nd</sup> Aug - Tue 27<sup>th</sup> Sep 2011; Term 1, weeks 1-10; Christmas vacation, Tue 13<sup>th</sup> Dec 2011 - Tue 3<sup>rd</sup> Jan 2012; Term 2, weeks 1-10; Easter vacation, Tue 20<sup>th</sup> Mar - Tue 17<sup>th</sup> Apr 2012; Term 3, weeks 1-10; Summer vacation, Tue 3<sup>rd</sup> - Tue 31<sup>st</sup> Jul 2012")
    formatter.format(Seq(WeekRange(11, 15)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be("Christmas vacation, Tue 13<sup>th</sup> Dec 2011 - Tue 3<sup>rd</sup> Jan 2012; Term 2, week 1")
    formatter.format(Seq(WeekRange(16, 25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be("Term 2, weeks 2-10; Easter vacation, Tue 20<sup>th</sup> Mar 2012")
  }

  @Test def cumulativeTermNumbering = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
    val formatter = new WeekRangesFormatter(AcademicYear.now())

    formatter.format(Seq(WeekRange(1, 10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be("Term 1, weeks 1-10")
    formatter.format(Seq(WeekRange(1, 5), WeekRange(7, 10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be("Term 1, weeks 1-5; Term 1, weeks 7-10")
    formatter.format(Seq(WeekRange(2, 16)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be("Term 1, weeks 2-10; Christmas vacation, Tue 13<sup>th</sup> Dec 2011 - Tue 3<sup>rd</sup> Jan 2012; Term 2, weeks 11-12")
    formatter.format(Seq(WeekRange(-8, 44)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be("Pre-term vacation, Tue 2<sup>nd</sup> Aug - Tue 27<sup>th</sup> Sep 2011; Term 1, weeks 1-10; Christmas vacation, Tue 13<sup>th</sup> Dec 2011 - Tue 3<sup>rd</sup> Jan 2012; Term 2, weeks 11-20; Easter vacation, Tue 20<sup>th</sup> Mar - Tue 17<sup>th</sup> Apr 2012; Term 3, weeks 21-30; Summer vacation, Tue 3<sup>rd</sup> - Tue 31<sup>st</sup> Jul 2012")
    formatter.format(Seq(WeekRange(11, 15)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be("Christmas vacation, Tue 13<sup>th</sup> Dec 2011 - Tue 3<sup>rd</sup> Jan 2012; Term 2, week 11")
    formatter.format(Seq(WeekRange(16, 25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be("Term 2, weeks 12-20; Easter vacation, Tue 20<sup>th</sup> Mar 2012")
  }

  @Test def academicWeekNumbering = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
    val formatter = new WeekRangesFormatter(AcademicYear.now())

    formatter.format(Seq(WeekRange(1, 10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic) should be("Weeks 1-10")
    formatter.format(Seq(WeekRange(1, 5), WeekRange(7, 10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic) should be("Weeks 1-5; 7-10")
    formatter.format(Seq(WeekRange(2, 16)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic) should be("Weeks 2-16")
    formatter.format(Seq(WeekRange(1, 52)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic) should be("Weeks 1-52")
    formatter.format(Seq(WeekRange(11, 15)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic) should be("Weeks 11-15")
    formatter.format(Seq(WeekRange(16, 25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic) should be("Weeks 16-25")
  }

  @Test def noWeekNumbers = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
    val formatter = new WeekRangesFormatter(AcademicYear.now())

    formatter.format(Seq(WeekRange(1, 10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None) should be("Tue 4<sup>th</sup> Oct - Tue 6<sup>th</sup> Dec 2011")
    formatter.format(Seq(WeekRange(1, 5), WeekRange(7, 10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None) should be("Tue 4<sup>th</sup> Oct - Tue 1<sup>st</sup> Nov 2011; Tue 15<sup>th</sup> Nov - Tue 6<sup>th</sup> Dec 2011")
    formatter.format(Seq(WeekRange(2, 16)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None) should be("Tue 11<sup>th</sup> Oct 2011 - Tue 17<sup>th</sup> Jan 2012")
    formatter.format(Seq(WeekRange(-8, 44)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None) should be("Tue 2<sup>nd</sup> Aug 2011 - Tue 31<sup>st</sup> Jul 2012")
    formatter.format(Seq(WeekRange(11, 15)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None) should be("Tue 13<sup>th</sup> Dec 2011 - Tue 10<sup>th</sup> Jan 2012")
    formatter.format(Seq(WeekRange(16, 25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None) should be("Tue 17<sup>th</sup> Jan - Tue 20<sup>th</sup> Mar 2012")
  }
}
