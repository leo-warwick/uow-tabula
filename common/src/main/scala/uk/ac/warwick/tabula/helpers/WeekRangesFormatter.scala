package uk.ac.warwick.tabula.helpers

import freemarker.core.{HTMLOutputFormat, TemplateHTMLOutputModel}
import freemarker.template.TemplateMethodModelEx
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, SmallGroupEvent, WeekRange}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.web.views.BaseTemplateMethodModelEx
import uk.ac.warwick.util.termdates.AcademicYearPeriod.PeriodType

import scala.jdk.CollectionConverters._
import scala.language.implicitConversions

/** Format week ranges, using a formatting preference for term week numbers, cumulative week numbers or academic week numbers.
  *
  * WeekRange objects are always _stored_ with academic week numbers. These may span multiple terms, holidays etc.
  */
object WeekRangesFormatter {

  val separator = "; "

  private val formatterMap = new WeekRangesFormatterCache

  /** The reason we need the academic year and day of the week here is that it might affect
    * which term a date falls under. Often, the Spring term starts on a Wednesday after New
    * Year's Day, so Monday of that week is in the vacation, but Thursday is week 1 of term 2.
    */
  def format(ranges: Seq[WeekRange], dayOfWeek: DayOfWeek, year: AcademicYear, numberingSystem: String): String =
    formatterMap.retrieve(year).format(ranges, dayOfWeek, numberingSystem)

  class WeekRangesFormatterCache {
    private val map = JConcurrentMap[AcademicYear, WeekRangesFormatter]()

    def retrieve(year: AcademicYear): WeekRangesFormatter = map.getOrElseUpdate(year, new WeekRangesFormatter(year))
  }

}


/** Companion class for Freemarker.
  */
class WeekRangesFormatterTag extends BaseTemplateMethodModelEx with KnowsUserNumberingSystem with AutowiringUserSettingsServiceComponent {

  import WeekRangesFormatter.format

  /** Pass through all the arguments, or just a SmallGroupEvent if you're lazy */
  override def execMethod(args: Seq[_]): TemplateHTMLOutputModel = {
    val user = RequestInfo.fromThread.map(_.user).getOrElse(NoCurrentUser())

    HTMLOutputFormat.INSTANCE.fromMarkup {
      args match {
        case Seq(ranges: Seq[_], dayOfWeek: DayOfWeek, year: AcademicYear, dept: Department) =>
          format(ranges.asInstanceOf[Seq[WeekRange]], dayOfWeek, year, numberingSystem(user, Some(dept)))

        case Seq(ranges: JList[_], dayOfWeek: DayOfWeek, year: AcademicYear, dept: Department) =>
          format(ranges.asScala.toSeq.asInstanceOf[Seq[WeekRange]], dayOfWeek, year, numberingSystem(user, Some(dept)))

        case Seq(event: SmallGroupEvent) =>
          format(event.weekRanges, event.day, event.group.groupSet.academicYear, numberingSystem(user, Some(event.group.groupSet.module.adminDepartment)))

        case _ => throw new IllegalArgumentException("Bad args: " + args)
      }
    }
  }
}

class WeekRangesFormatter(year: AcademicYear) extends WeekRanges(year: AcademicYear) {

  import WeekRangesFormatter._

  // Pimp Term to have a clever toString output
  implicit class PimpedTerm(termOrVacation: AcademicPeriod) {
    def print(weekRange: WeekRange, dayOfWeek: DayOfWeek, numberingSystem: String): String = {
      // TODO we've already done this calculation once in groupWeekRangesByTerm, do we really need to do it again?
      val startDate = weekNumberToDate(weekRange.minWeek).withDayOfWeek(dayOfWeek.jodaDayOfWeek)
      val endDate = weekNumberToDate(weekRange.maxWeek).withDayOfWeek(dayOfWeek.jodaDayOfWeek)

      termOrVacation match {
        case vac: Vacation =>
          // Date range
          if (startDate.equals(endDate))
            "%s, %s".format(vac.periodType.toString, IntervalFormatter.formatDate(startDate))
          else
            "%s, %s".format(vac.periodType.toString, IntervalFormatter.formatDate(startDate, endDate))
        case term: Term =>
          // Convert week numbers to the correct style
          val termNumber = term.periodType match {
            case PeriodType.autumnTerm => 1
            case PeriodType.springTerm => 2
            case PeriodType.summerTerm => 3
            case _ => throw new IllegalArgumentException
          }

          def weekNumber(date: LocalDate) =
            numberingSystem match {
              case WeekRange.NumberingSystem.Term => term.weekForDate(date).termWeekNumber
              case WeekRange.NumberingSystem.Cumulative => term.weekForDate(date).cumulativeWeekNumber
            }

          if (weekRange.isSingleWeek) "Term %d, week %d".format(termNumber, weekNumber(startDate))
          else "Term %d, weeks %d-%d".format(termNumber, weekNumber(startDate), weekNumber(endDate))
      }
    }
  }

  def format(ranges: Seq[WeekRange], dayOfWeek: DayOfWeek, numberingSystem: String): String = numberingSystem match {
    case WeekRange.NumberingSystem.Academic =>
      // Special early exit for the "academic" week numbering system - because that's what we
      // store, we can simply return that.
      val prefix =
        if (ranges.size == 1 && ranges.head.isSingleWeek) "Week"
        else "Weeks"

      prefix + " " + ranges.mkString(separator)
    case WeekRange.NumberingSystem.None =>
      ranges.map { weekRange =>
        val startDate = weekNumberToDate(weekRange.minWeek).withDayOfWeek(dayOfWeek.jodaDayOfWeek)
        val endDate = weekNumberToDate(weekRange.maxWeek).withDayOfWeek(dayOfWeek.jodaDayOfWeek)

        IntervalFormatter.formatDate(start = startDate, end = endDate)
      }.mkString(separator)
    case _ =>
      /*
       * The first thing we need to do is split the WeekRanges by term.
       *
       * If we have a weekRange that is 1-24, we might split that into three week ranges:
       *  1-10, 11-15, 16-24
       *
       * This is because we display it as three separate ranges.
       *
       * Then we use our PimpedTerm to print the week numbers based on the numbering system.
       */
      groupWeekRangesByTerm(ranges).map {
        case (weekRange, term) =>
          term.print(weekRange, dayOfWeek, numberingSystem)
      }.mkString(separator)
  }

}

class WeekRanges(year: AcademicYear) {
  lazy val weeksForYear: Map[Int, AcademicWeek] = year.weeks

  def weekNumberToDate(weekNumber: Int): LocalDate = weeksForYear(weekNumber).firstDay

  def groupWeekRangesByTerm(ranges: Seq[WeekRange]): Seq[(WeekRange, AcademicPeriod)] = {
    ranges.flatMap { range =>
      if (range.isSingleWeek) Seq((range, year.termOrVacationForDate(weekNumberToDate(range.minWeek))))
      else {
        val startDate = weekNumberToDate(range.minWeek)
        val endDate = weekNumberToDate(range.maxWeek)

        year.termsAndVacations.filterNot { p => p.lastDay.isBefore(startDate) || p.firstDay.isAfter(endDate) }.map { term =>
          val minWeek =
            if (startDate.isBefore(term.firstDay)) term.firstWeek
            else term.weekForDate(startDate)

          val maxWeek =
            if (endDate.isAfter(term.lastDay)) term.lastWeek
            else term.weekForDate(endDate)

          (WeekRange(minWeek.weekNumber, maxWeek.weekNumber), term)
        }
      }
    }
  }
}

trait KnowsUserNumberingSystem {
  self: UserSettingsServiceComponent =>

  def numberingSystem(user: CurrentUser, department: => Option[Department]): String = {
    userSettingsService.getByUserId(user.apparentId)
      .flatMap { settings => Option(settings.weekNumberingSystem) }
      .getOrElse(department.map(_.weekNumberingSystem).getOrElse(WeekRange.NumberingSystem.Default))
  }
}

/**
  * Outputs a list of weeks, with the start and end times for each week, and a description which is formatted using
  * the user's preferred numbering system (if set) or the department's.
  *
  * The list is output as JSON, to allow client-side code to access it for formatting weeks in javascript
  */
trait WeekRangesDumper extends KnowsUserNumberingSystem {
  self: ClockComponent with UserSettingsServiceComponent with ModuleAndDepartmentServiceComponent =>

  private def academicWeeksBetween(startDate: LocalDate, endDate: LocalDate): Seq[(AcademicYear, AcademicWeek)] = {
    val startYear = AcademicYear.forDate(startDate)
    val endYear = AcademicYear.forDate(endDate)

    if (startYear == endYear) {
      startYear.weeks.values.toSeq.sorted.filterNot { w => w.lastDay.isBefore(startDate) || w.firstDay.isAfter(endDate) }.map { w => (startYear, w) }
    } else {
      val startWeeks = startYear.weeks.values.toSeq.sorted.filterNot { w => w.lastDay.isBefore(startDate) }.map { w => (startYear, w) }
      val inbetweenWeeks = (startYear.startYear + 1 until endYear.startYear - 1).flatMap { y =>
        val inbetweenYear = AcademicYear(y)
        inbetweenYear.weeks.values.toSeq.sorted.map { w => (inbetweenYear, w) }
      }
      val endWeeks = endYear.weeks.values.toSeq.sorted.filterNot { w => w.firstDay.isAfter(startDate) }.map { w => (endYear, w) }

      startWeeks ++ inbetweenWeeks ++ endWeeks
    }
  }

  def getWeekRangesAsJSON(formatWeekName: (AcademicYear, Int, String) => String): String = {

    // set a fairly large window over which to get week dates since we don't know exactly
    // how they might be used
    val startDate = clock.now.minusYears(2).toLocalDate
    val endDate = clock.now.plusYears(2).toLocalDate
    val user = RequestInfo.fromThread.map(_.user).getOrElse(NoCurrentUser())

    // don't fetch the department if we don't have to, and if we _do_ have to, don't fetch it more than once.
    lazy val department = moduleAndDepartmentService.getDepartmentByCode(user.departmentCode)

    val termsAndWeeks = academicWeeksBetween(startDate, endDate)
    val system = numberingSystem(user, department)

    val weekDescriptions = termsAndWeeks map {
      case (year, week) =>
        val longDescription = formatWeekName(year, week.weekNumber, system)

        val shortDescription = system match {
          case WeekRange.NumberingSystem.Academic => week.weekNumber.toString
          case WeekRange.NumberingSystem.None => ""
          case WeekRange.NumberingSystem.Term =>
            week.period match {
              case _: Vacation => ""
              case _: Term => week.termWeekNumber
            }
          case WeekRange.NumberingSystem.Cumulative =>
            week.period match {
              case _: Vacation => ""
              case _: Term => week.cumulativeWeekNumber
            }
        }

        // weekRangeFormatter always includes vacations, but we don't want them here, so if the
        // description doesn't look like "Term X Week Y", throw it away and use a standard IntervalFormat;
        // TAB-1906 UNLESS we're in academic week number town
        val description = if (system == WeekRange.NumberingSystem.Academic || longDescription.startsWith("Term")) {
          longDescription
        } else {
          IntervalFormatter.formatDateRange(week.dateRange, includeDays = false)
        }

        (week.dateRange.lowerEndpoint().toDateTimeAtStartOfDay.getMillis, week.dateRange.upperEndpoint().plusDays(1).toDateTimeAtStartOfDay.getMillis, description, shortDescription)
    }

    // could use Jackson to map these objects but it doesn't seem worth it
    "[" + weekDescriptions.map {
      case (start, end, desc, shortDescription) => s"{'start':$start,'end':$end,'desc':'$desc','shortDescription':'$shortDescription'}"
    }.mkString(",") + "]"

  }
}

/**
  * Freemarker companion for the WeekRangesDumper
  */
class WeekRangesDumperTag extends TemplateMethodModelEx with WeekRangesDumper with SystemClockComponent
  with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent {

  def formatWeekName(year: AcademicYear, weekNumber: Int, numberingSystem: String): String = {
    // use a WeekRangesFormatter to format the week name, obv.
    val formatter = new WeekRangesFormatter(year)
    formatter.format(Seq(WeekRange(weekNumber)), DayOfWeek.Monday, numberingSystem)
  }

  def exec(unused: JList[_]): AnyRef = {
    getWeekRangesAsJSON(formatWeekName)
  }
}


class WholeWeekFormatter(year: AcademicYear) extends WeekRanges(year: AcademicYear) {

  implicit class PimpedTerm(termOrVacation: AcademicPeriod) {
    def print(weekRange: WeekRange, dayOfWeek: DayOfWeek, numberingSystem: String, short: Boolean): String = {
      val startDate = weekNumberToDate(weekRange.minWeek)
      val endDate = weekNumberToDate(weekRange.maxWeek)

      termOrVacation match {
        case vac: Vacation if numberingSystem == WeekRange.NumberingSystem.Academic =>
          if (short) {
            if (weekRange.isSingleWeek) weekRange.minWeek.toString
            else "%d-%d".format(weekRange.minWeek, weekRange.maxWeek)
          } else {
            if (weekRange.isSingleWeek) "%s, week %d".format(vac.periodType.toString, weekRange.minWeek)
            else "%s, weeks %d-%d".format(vac.periodType.toString, weekRange.minWeek, weekRange.maxWeek)
          }
        case vac: Vacation =>
          if (weekRange.isSingleWeek) {
            if (short) startDate.toString("dd/MM")
            else "%s, w/c %s".format(
              vac.periodType.toString,
              IntervalFormatter.formatDate(start = startDate)
            )
          } else {
            // Date range
            if (short)
              "%s, %s-%s".format(
                vac.periodType.toString,
                startDate.toString("dd/MM"),
                endDate.toString("dd/MM")
              )
            else
              "%s, %s".format(
                vac.periodType.toString,
                IntervalFormatter.formatDate(
                  start = startDate,
                  end = endDate,
                ).replaceAll("\\b(Mon|Tue|Wed|Thu|Fri|Sat|Sun)\\b", "w/c $1")
              )
          }
        case term: Term =>
          // Convert week numbers to the correct style
          val termNumber = term.periodType match {
            case PeriodType.autumnTerm => 1
            case PeriodType.springTerm => 2
            case PeriodType.summerTerm => 3
            case _ => throw new IllegalArgumentException
          }

          def weekNumber(date: LocalDate) =
            numberingSystem match {
              case WeekRange.NumberingSystem.Term => term.weekForDate(date).termWeekNumber
              case WeekRange.NumberingSystem.Cumulative => term.weekForDate(date).cumulativeWeekNumber
              case _ => term.weekForDate(date).weekNumber
            }

          if (short) {
            if (weekRange.isSingleWeek) weekNumber(startDate).toString
            else "Term %d, %d-%d".format(termNumber, weekNumber(startDate), weekNumber(endDate))
          } else {
            if (weekRange.isSingleWeek) "Term %d, week %d".format(termNumber, weekNumber(startDate))
            else "Term %d, weeks %d-%d".format(termNumber, weekNumber(startDate), weekNumber(endDate))
          }
      }
    }
  }

  def format(ranges: Seq[WeekRange], dayOfWeek: DayOfWeek, numberingSystem: String, short: Boolean): String = {
    import WeekRangesFormatter._

    numberingSystem match {
      case WeekRange.NumberingSystem.None =>
        ranges.map { weekRange =>
          val startDate = weekNumberToDate(weekRange.minWeek)
          val endDate = weekNumberToDate(weekRange.maxWeek)

          if (short) {
            if (weekRange.isSingleWeek) startDate.toString("dd/MM")
            else startDate.toString("dd/MM") + " - " + endDate.toString("dd/MM")
          } else {
            IntervalFormatter.formatDate(startDate, endDate).replaceAll("\\b(Mon|Tue|Wed|Thu|Fri|Sat|Sun)\\b", "w/c $1")
          }
        }.mkString(separator)
      case _ =>
        groupWeekRangesByTerm(ranges).map {
          case (weekRange, term) =>
            term.print(weekRange = weekRange, dayOfWeek = dayOfWeek, numberingSystem = numberingSystem, short = short)
        }.mkString(separator)
    }
  }
}

object WholeWeekFormatter {

  val separator = "; "

  private val formatterMap = new WholeWeekFormatterCache

  /** The reason we need the academic year and day of the week here is that it might affect
    * which term a date falls under. Often, the Spring term starts on a Wednesday after New
    * Year's Day, so Monday of that week is in the vacation, but Thursday is week 1 of term 2.
    */
  def format(ranges: Seq[WeekRange], dayOfWeek: DayOfWeek, year: AcademicYear, numberingSystem: String, short: Boolean): String =
    formatterMap.retrieve(year).format(ranges = ranges, dayOfWeek = dayOfWeek, numberingSystem = numberingSystem, short = short)

  class WholeWeekFormatterCache {
    private val map = JConcurrentMap[AcademicYear, WholeWeekFormatter]()

    def retrieve(year: AcademicYear): WholeWeekFormatter = map.getOrElseUpdate(year, new WholeWeekFormatter(year))
  }

}

/* Freemarker companion for the WholeWeekFormatter */

class WholeWeekFormatterTag extends BaseTemplateMethodModelEx with KnowsUserNumberingSystem with AutowiringUserSettingsServiceComponent {

  import WholeWeekFormatter.format

  /** Pass through all the arguments  */
  override def execMethod(args: Seq[_]): String = {
    val user = RequestInfo.fromThread.map(_.user).getOrElse(NoCurrentUser())

    args match {
      case Seq(startWeekNumber: JInteger, endWeekNumber: JInteger, academicYear: AcademicYear, dept: Department, short: JBoolean) =>
        format(Seq(WeekRange(startWeekNumber, endWeekNumber)), DayOfWeek.Thursday, academicYear, numberingSystem(user, Some(dept)), short)

      case Seq(startWeekNumber: JInteger, endWeekNumber: JInteger, academicYear: AcademicYear, short: JBoolean) =>
        format(Seq(WeekRange(startWeekNumber, endWeekNumber)), DayOfWeek.Thursday, academicYear, WeekRange.NumberingSystem.None, short)

      case _ => throw new IllegalArgumentException("Bad args: " + args)
    }
  }
}



