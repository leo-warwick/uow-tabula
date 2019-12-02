package uk.ac.warwick.tabula

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.google.common.collect.{Range => GRange}
import org.joda.time.{DateTime, DateTimeConstants, LocalDate}
import uk.ac.warwick.tabula.AcademicPeriod._
import uk.ac.warwick.tabula.AcademicWeek._
import uk.ac.warwick.tabula.AcademicYear._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.Convertible
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.helpers.DateRange
import uk.ac.warwick.tabula.helpers.JodaConverters._
import uk.ac.warwick.util.termdates.{AcademicWeek => JAcademicWeek, AcademicYear => JAcademicYear, AcademicYearPeriod => JAcademicYearPeriod, ExtendedAcademicYear => JExtendedAcademicYear, Term => JTerm, Vacation => JVacation}

import scala.jdk.CollectionConverters._
import scala.language.implicitConversions

/**
  * Represents a particular academic year. Traditionally they are displayed as
  * "99/00" or "11/12" but we just store the first year as a 4-digit number.
  * toString() returns the traditional format.
  */
@JsonSerialize(using = classOf[ToStringSerializer])
case class AcademicYear(underlying: JAcademicYear) extends Ordered[AcademicYear] with Convertible[JInteger] {
  val startYear: Int = underlying.getStartYear
  val endYear: Int = startYear + 1

  def firstDay: LocalDate = underlying.getPeriods.asScala.head.getFirstDay.asJoda

  def lastDay: LocalDate = underlying.getPeriods.asScala.last.getLastDay.asJoda

  override def toString: String = underlying.toString

  // properties for binding to dropdown box
  def getStoreValue: Int = underlying.getValue

  def getLabel: String = underlying.getLabel

  def value: JInteger = underlying.getValue

  def previous: AcademicYear = this - 1

  def next: AcademicYear = this + 1

  def -(i: Int): AcademicYear = AcademicYear(underlying.getStartYear - i)

  def +(i: Int): AcademicYear = AcademicYear(underlying.getStartYear + i)

  /**
    * Returns a sequence of AcademicYears, in order, starting
    * the given number of years before this year, and ending
    * the given number of years after, inclusive. The length
    * will be 1 + yearsBefore + yearsAfter. If both are 0, then
    * it will have a single element containing this year.
    */
  def yearsSurrounding(yearsBefore: Int, yearsAfter: Int): Seq[AcademicYear] = {
    assert(yearsBefore >= 0)
    assert(yearsAfter >= 0)
    val length = 1 + yearsBefore + yearsAfter
    val first = this - yearsBefore
    Iterable.iterate(first, length)(_.next).toSeq
  }

  /**
    * An inclusive range of this year to endYear
    */
  def to(endYear: AcademicYear): Seq[AcademicYear] = {
    assert(endYear.startYear >= startYear)
    val length = 1 + (endYear.startYear - startYear)
    Iterable.iterate(this, length)(_.next).toSeq
  }

  override def equals(that: Any): Boolean = that match {
    case other: AcademicYear => startYear == other.startYear
    case _ => false
  }

  override def hashCode(): Int = startYear.hashCode()

  def compare(that: AcademicYear): Int = this.underlying.compareTo(that.underlying)

  def isSITSInFlux(date: LocalDate): Boolean = {
    val juneThisYear = new LocalDate(underlying.getStartYear + 1, DateTimeConstants.JUNE, 1)
    !date.isBefore(juneThisYear)
  }

  def placeholder: Boolean = underlying.isPlaceholder

  def termsAndVacations: Seq[AcademicPeriod] = underlying.getPeriods.asScala.toSeq.map { p => p: AcademicPeriod }

  def termOrVacationForDate(now: LocalDate): AcademicPeriod = underlying.getPeriod(now.asJava)

  def termOrVacation(periodType: JAcademicYearPeriod.PeriodType): AcademicPeriod = underlying.getPeriod(periodType)

  def weeks: Map[Int, AcademicWeek] = underlying.getAcademicWeeks.asScala.map { w => (w.getWeekNumber, w: AcademicWeek) }.toMap

  def weekForDate(now: LocalDate): AcademicWeek = underlying.getAcademicWeek(now.asJava)

  def extended: AcademicYear = underlying match {
    case _: JExtendedAcademicYear => this
    case _ => AcademicYear(JExtendedAcademicYear.starting(startYear))
  }

  // SITS week 1 is the week starting on or after 1st August
  def dateFromSITSWeek(week: Int): LocalDate = {
    val firstOfAugust = LocalDate.now().withYear(underlying.getStartYear).withMonthOfYear(DateTimeConstants.AUGUST).withDayOfMonth(1)
    val startOfWeekOne = if (firstOfAugust.getDayOfWeek == DateTimeConstants.MONDAY) {
      firstOfAugust
    } else {
      firstOfAugust.plusDays(8 - firstOfAugust.getDayOfWeek)
    }
    startOfWeekOne.plusWeeks(week - 1)
  }
}

object AcademicYear {
  implicit def warwickUtilsAcademicYearToAcademicYear(year: JAcademicYear): AcademicYear = AcademicYear(year)

  // An implicit for the UserType to create instances
  implicit val factory: JInteger => AcademicYear = (year: JInteger) => AcademicYear(year)

  def apply(startYear: Int): AcademicYear = JExtendedAcademicYear.starting(startYear)

  def starting(startYear: Int): AcademicYear = apply(startYear)

  def parse(string: String): AcademicYear = JExtendedAcademicYear.starting(JAcademicYear.parse(string).getStartYear)

  def forDate(now: LocalDate): AcademicYear = JExtendedAcademicYear.starting(JAcademicYear.forDate(now.asJava).getStartYear)

  def forDate(now: DateTime): AcademicYear = forDate(now.toLocalDate)

  def now(): AcademicYear = forDate(DateTime.now())

  def allCurrent(): Seq[AcademicYear] = allForDate(DateTime.now())

  def allForDate(now: LocalDate): Seq[AcademicYear] = {
    var years: Seq[AcademicYear] = Seq(forDate(now))

    while (years.head.previous.lastDay.isAfter(now))
      years = years.head.previous +: years

    years
  }

  def allForDate(now: DateTime): Seq[AcademicYear] = allForDate(now.toLocalDate)
}

case class AcademicWeek(underlying: JAcademicWeek) extends Ordered[AcademicWeek] {

  def year: AcademicYear = underlying.getYear

  def period: AcademicPeriod = underlying.getPeriod

  def weekNumber: Int = underlying.getWeekNumber

  def termWeekNumber: Int = underlying.getTermWeekNumber

  def cumulativeWeekNumber: Int = underlying.getCumulativeWeekNumber

  def firstDay: LocalDate = underlying.getDateRange.getStart.asJoda

  def lastDay: LocalDate = underlying.getDateRange.getEndInclusive.asJoda

  def dateRange: GRange[LocalDate] = DateRange(firstDay, lastDay)

  override def compare(that: AcademicWeek): Int = this.underlying.compareTo(that.underlying)
}

object AcademicWeek {
  implicit def warwickUtilsAcademicWeekToAcademicWeek(week: JAcademicWeek): AcademicWeek = AcademicWeek(week)
}

sealed trait AcademicPeriod extends Ordered[AcademicPeriod] {
  val underlying: JAcademicYearPeriod

  def year: AcademicYear = underlying.getYear

  def periodType: JAcademicYearPeriod.PeriodType = underlying.getType

  def firstDay: LocalDate = underlying.getFirstDay.asJoda

  def lastDay: LocalDate = underlying.getLastDay.asJoda

  def weeks: Seq[AcademicWeek] = underlying.getAcademicWeeks.asScala.toSeq.map { w => w: AcademicWeek }

  def firstWeek: AcademicWeek = underlying.getFirstWeek

  def lastWeek: AcademicWeek = underlying.getLastWeek

  def weekRange: WeekRange = WeekRange(firstWeek.weekNumber, lastWeek.weekNumber)

  def weekForDate(date: LocalDate): AcademicWeek = {
    if (date.isBefore(firstDay) || date.isAfter(lastDay)) throw new IllegalArgumentException
    year.weekForDate(date)
  }

  def isTerm: Boolean = underlying.isTerm

  def isVacation: Boolean = underlying.isVacation

  def dateRange: GRange[LocalDate] = DateRange(firstDay, lastDay)

  override def compare(that: AcademicPeriod): Int = this.underlying.compareTo(that.underlying)
}

object AcademicPeriod {
  val allPeriodTypes: Seq[JAcademicYearPeriod.PeriodType] = JAcademicYearPeriod.PeriodType.values().toSeq

  implicit def warwickUtilsAcademicYearPeriodToAcademicYearPeriod(period: JAcademicYearPeriod): AcademicPeriod = AcademicPeriod(period)

  def apply(period: JAcademicYearPeriod): AcademicPeriod = period match {
    case t: JTerm => Term(t)
    case v: JVacation => Vacation(v)
    case p => throw new IllegalArgumentException(s"Unexpected JAcademicYearPeriod: $p")
  }
}

case class Term(underlying: JTerm) extends AcademicPeriod

case class Vacation(underlying: JVacation) extends AcademicPeriod
