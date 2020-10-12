package uk.ac.warwick.tabula.data.model.groups

import java.sql.Types

import org.hibernate.`type`.StandardBasicTypes
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.AbstractBasicUserType
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.util.termdates.AcademicYearPeriod.PeriodType

case class WeekRange(minWeek: WeekRange.Week, maxWeek: WeekRange.Week) {
  if (maxWeek < minWeek) throw new IllegalArgumentException("maxWeek must be >= minWeek")

  def isSingleWeek: Boolean = maxWeek == minWeek

  def toWeeks: Seq[WeekRange.Week] = minWeek to maxWeek

  override def toString: String =
    if (!isSingleWeek) "%d-%d".format(minWeek, maxWeek)
    else minWeek.toString
}

object WeekRange {
  type Week = Int

  def apply(singleWeek: Week): WeekRange = WeekRange(singleWeek, singleWeek)

  def fromString(rep: String): WeekRange = rep.replaceAll("\\s*", "") match {
    case r"""(-?\d+)${singleWeek}""" => WeekRange(singleWeek.toInt)
    case r"""(-?\d+)${min}-(-?\d+)${max}""" => WeekRange(min.toInt, max.toInt)
    case _ => throw new IllegalArgumentException("Couldn't convert string representation %s to WeekRange" format rep)
  }

  /**
    * Combine weeks together into week ranges. For example:
    * 3, 6, 9, 11, 4, 5, 1 -> 1, 3-6, 9, 11
    */
  def combine(weeks: Seq[Week]): Seq[WeekRange] =
  /*
   * Fold right starts at the right-most element of the sequence, and then folds the next element to the left
   * on top of it. We go through the sequence backwards, effectively, seeing whether the previous week is one
   * before the week we saw last.
   */
    weeks.sorted.foldRight(List[WeekRange]()) { case (week, acc) =>
      acc.headOption match {
        case Some(range) if range.minWeek == week + 1 => WeekRange(week, range.maxWeek) :: acc.tail
        case _ => WeekRange(week) :: acc
      }
    }

  def termWeekRanges(year: AcademicYear): Seq[WeekRange] = {
    // We are confident that November 1st is always in the Autumn of the year
    val autumnTerm = year.termOrVacation(PeriodType.autumnTerm)
    val springTerm = year.termOrVacation(PeriodType.springTerm)
    val summerTerm = year.termOrVacation(PeriodType.summerTerm)

    Seq(autumnTerm, springTerm, summerTerm).map(_.weekRange)
  }

  implicit val defaultOrdering: Ordering[WeekRange] = Ordering.by[WeekRange, Week](_.minWeek)

  object NumberingSystem {
    val Term = "term" // 1-10, 1-10, 1-10
    val Cumulative = "cumulative" // 1-10, 11-20, 21-30
    val Academic = "academic" // 1-52
    val None = "none" // Dates only

    val Default = Term
  }

}

class WeekRangeListUserType extends AbstractBasicUserType[Seq[WeekRange], String] {

  val separator = ","
  val basicType = StandardBasicTypes.STRING

  override def sqlTypes = Array(Types.VARCHAR)

  val nullValue = null
  val nullObject = Nil

  override def convertToObject(string: String): Seq[WeekRange] = string.split(separator).map { rep => WeekRange.fromString(rep) }.toSeq

  override def convertToValue(list: Seq[WeekRange]): String = if (list.isEmpty) null else list.map(_.toString).mkString(separator)

}
