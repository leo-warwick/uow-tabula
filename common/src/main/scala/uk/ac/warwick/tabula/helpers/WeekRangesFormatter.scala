package uk.ac.warwick.tabula.helpers

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.{CurrentUser, RequestInfo}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.util.termdates.Term
import org.joda.time.{DateTime, Interval}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.JavaImports._
import freemarker.template.{TemplateMethodModelEx, TemplateModel}
import freemarker.template.utility.DeepUnwrap
import uk.ac.warwick.tabula.services._


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
	def format(ranges: Seq[WeekRange], dayOfWeek: DayOfWeek, year: AcademicYear, numberingSystem: String)(implicit termService: TermService): String =
		formatterMap.retrieve(year) format (ranges, dayOfWeek, numberingSystem)

	class WeekRangesFormatterCache {
		private val map = JConcurrentMap[AcademicYear, WeekRangesFormatter]()
		def retrieve(year: AcademicYear)(implicit termService: TermService): WeekRangesFormatter = map.getOrElseUpdate(year, new WeekRangesFormatter(year))
	}
}


/** Companion class for Freemarker.
	*/
class WeekRangesFormatterTag extends TemplateMethodModelEx with KnowsUserNumberingSystem with AutowiringUserSettingsServiceComponent with AutowiringTermServiceComponent {

	import WeekRangesFormatter.format

	/** Pass through all the arguments, or just a SmallGroupEvent if you're lazy */
	override def exec(list: JList[_]): String = {
		val user = RequestInfo.fromThread.get.user

		val args = list.asScala.toSeq.map { model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel]) }
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

class WeekRangesFormatter(year: AcademicYear)(implicit termService: TermService) extends WeekRanges(year: AcademicYear) {

	import WeekRangesFormatter._

	// Pimp Term to have a clever toString output
	implicit class PimpedTerm(termOrVacation: Term) {
		def print(weekRange: WeekRange, dayOfWeek: DayOfWeek, numberingSystem: String): String = {
			// TODO we've already done this calculation once in groupWeekRangesByTerm, do we really need to do it again?
			val startDate = weekNumberToDate(weekRange.minWeek, dayOfWeek)
			val endDate = weekNumberToDate(weekRange.maxWeek, dayOfWeek)

			termOrVacation match {
				case vac: Vacation =>
					// Date range
					if (startDate.equals(endDate))
						"%s, %s" format (vac.getTermTypeAsString, IntervalFormatter.format(startDate, includeTime = false))
					else
						"%s, %s" format (vac.getTermTypeAsString, IntervalFormatter.format(startDate, endDate, includeTime = false))
				case term =>
					// Convert week numbers to the correct style
					val termNumber = term.getTermType match {
						case Term.TermType.autumn => 1
						case Term.TermType.spring => 2
						case Term.TermType.summer => 3
					}

					def weekNumber(date: DateTime) =
						numberingSystem match {
							case WeekRange.NumberingSystem.Term => term.getWeekNumber(date)
							case WeekRange.NumberingSystem.Cumulative => term.getCumulativeWeekNumber(date)
						}

					if (weekRange.isSingleWeek) "Term %d, week %d" format (termNumber, weekNumber(startDate))
					else "Term %d, weeks %d-%d" format (termNumber, weekNumber(startDate), weekNumber(endDate))
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
				val startDate = weekNumberToDate(weekRange.minWeek, dayOfWeek)
				val endDate = weekNumberToDate(weekRange.maxWeek, dayOfWeek)

				IntervalFormatter.format(startDate, endDate, includeTime = false)
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
			groupWeekRangesByTerm(ranges, dayOfWeek).map {
				case (weekRange, term) =>
					term.print(weekRange, dayOfWeek, numberingSystem)
			}.mkString(separator)
	}

}


object WeekRangeSelectFormatter {

	val separator = "; "

	private val formatterMap = new WeekRangeSelectFormatterCache

	/** The reason we need the academic year and day of the week here is that it might affect
		* which term a date falls under. Often, the Spring term starts on a Wednesday after New
		* Year's Day, so Monday of that week is in the vacation, but Thursday is week 1 of term 2.
		*/
	def format(ranges: Seq[WeekRange], dayOfWeek: DayOfWeek, year: AcademicYear, numberingSystem: String)(implicit termService: TermService): Seq[EventWeek] =
		formatterMap.retrieve(year) format (ranges, dayOfWeek, numberingSystem)

	class WeekRangeSelectFormatterCache {
		private val map = JConcurrentMap[AcademicYear, WeekRangeSelectFormatter]()
		def retrieve(year: AcademicYear)(implicit termService: TermService): WeekRangeSelectFormatter = map.getOrElseUpdate(year, new WeekRangeSelectFormatter(year))
	}
}


class WeekRanges(year:AcademicYear)(implicit termService: TermService) {

	// We are confident that November 1st is always in term 1 of the year
	lazy val weeksForYear: Map[JInteger, Interval] =
		termService.getAcademicWeeksForYear(year.dateInTermOne).toMap

	def weekNumberToDate(weekNumber: Int, dayOfWeek: DayOfWeek): DateTime =
		weeksForYear(weekNumber).getStart.withDayOfWeek(dayOfWeek.jodaDayOfWeek)

	def groupWeekRangesByTerm(ranges: Seq[WeekRange], dayOfWeek: DayOfWeek): Seq[(WeekRange, Term)] = {
		ranges flatMap { range =>
			if (range.isSingleWeek) Seq((range, termService.getTermFromDateIncludingVacations(weekNumberToDate(range.minWeek, dayOfWeek))))
			else {
				val startDate = weekNumberToDate(range.minWeek, dayOfWeek)
				val endDate = weekNumberToDate(range.maxWeek, dayOfWeek)

				termService.getTermsBetween(startDate, endDate) map { term =>
					val minWeek =
						if (startDate.isBefore(term.getStartDate)) term.getAcademicWeekNumber(term.getStartDate)
						else term.getAcademicWeekNumber(startDate)

					val maxWeek =
						if (endDate.isAfter(term.getEndDate)) term.getAcademicWeekNumber(term.getEndDate)
						else term.getAcademicWeekNumber(endDate)

					(WeekRange(minWeek, maxWeek), term)
				}
			}
		}
	}
}



class WeekRangeSelectFormatter(year: AcademicYear)(implicit termService: TermService) extends WeekRanges(year: AcademicYear) {

	def format(ranges: Seq[WeekRange], dayOfWeek: DayOfWeek, numberingSystem: String): Seq[EventWeek] = {
		val allTermWeekRanges = WeekRange.termWeekRanges(year)
		val currentTerm = getTermNumber(DateTime.now)
		val eventRanges = ranges flatMap (_.toWeeks)

		val currentTermRanges = allTermWeekRanges.toList(currentTerm).toWeeks
		val weeks = currentTermRanges.intersect(eventRanges)

		numberingSystem match {
			case WeekRange.NumberingSystem.Term => weeks.map( x => EventWeek( x - (currentTermRanges.head - 1 ), x ) )
			case WeekRange.NumberingSystem.Cumulative => weeks.map({x =>
				val date = weekNumberToDate(x, dayOfWeek)
				EventWeek(termService.getTermFromDate(date).getCumulativeWeekNumber(date), x)
			})
			case WeekRange.NumberingSystem.Academic => eventRanges.map(x => EventWeek(x, x))
			case _ => weeks.map( x => EventWeek(x, x) )
		}
	}

	def getTermNumber(now: DateTime): Int = {
		termService.getTermFromDate(DateTime.now).getTermType match {
			case Term.TermType.autumn => 0
			case Term.TermType.spring => 1
			case Term.TermType.summer => 2
		}
	}
}

case class EventWeek(weekToDisplay: Int, weekToStore: Int)

class WeekRangeSelectFormatterTag extends TemplateMethodModelEx
	with KnowsUserNumberingSystem with AutowiringUserSettingsServiceComponent with AutowiringTermServiceComponent {
	import WeekRangeSelectFormatter.format

	/** Pass through all the arguments, or just a SmallGroupEvent if you're lazy */
	override def exec(list: JList[_]): Seq[EventWeek] = {
		val user = RequestInfo.fromThread.get.user

		val args = list.asScala.toSeq.map { model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel]) }
		args match {
			case Seq(event: SmallGroupEvent) =>
				format(event.weekRanges, event.day, event.group.groupSet.academicYear, numberingSystem(user, Some(event.group.groupSet.module.adminDepartment)))

			case _ => throw new IllegalArgumentException("Bad args: " + args)
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
	self: ClockComponent with UserSettingsServiceComponent with ModuleAndDepartmentServiceComponent with TermServiceComponent =>

	def getWeekRangesAsJSON(formatWeekName: (AcademicYear, Int, String) => String): String = {

		// set a fairly large window over which to get week dates since we don't know exactly
		// how they might be used
		val startDate = clock.now.minusYears(2)
		val endDate = clock.now.plusYears(2)
		val user = RequestInfo.fromThread.get.user

		// don't fetch the department if we don't have to, and if we _do_ have to, don't fetch it more than once.
		lazy val department = moduleAndDepartmentService.getDepartmentByCode(user.departmentCode)

		val termsAndWeeks = termService.getAcademicWeeksBetween(startDate, endDate)
		val system = numberingSystem(user, department)

		val weekDescriptions = termsAndWeeks map {
			case (year, weekNumber, weekInterval) =>
				val longDescription = formatWeekName(year, weekNumber, system)

				val baseDate = weekInterval.getStart.withDayOfWeek(DayOfWeek.Thursday.jodaDayOfWeek)
				val shortDescription = system match {
					case WeekRange.NumberingSystem.Academic => weekNumber.toString
					case WeekRange.NumberingSystem.None => ""
					case WeekRange.NumberingSystem.Term =>
						termService.getTermFromDateIncludingVacations(baseDate) match {
							case vac: Vacation => ""
							case t: Term => t.getWeekNumber(baseDate)
						}
					case WeekRange.NumberingSystem.Cumulative =>
						termService.getTermFromDateIncludingVacations(baseDate) match {
							case vac: Vacation => ""
							case t: Term => t.getCumulativeWeekNumber(baseDate)
						}
				}

				// weekRangeFormatter always includes vacations, but we don't want them here, so if the
				// description doesn't look like "Term X Week Y", throw it away and use a standard IntervalFormat;
				// TAB-1906 UNLESS we're in academic week number town
				val description = if (system == WeekRange.NumberingSystem.Academic || longDescription.startsWith("Term")) {
					longDescription
				} else {
					IntervalFormatter.format(weekInterval.getStart, weekInterval.getEnd, includeTime = false, includeDays = false)
				}

				(weekInterval.getStart.getMillis, weekInterval.getEnd.getMillis, description, shortDescription)
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
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent with AutowiringTermServiceComponent {

	def formatWeekName(year: AcademicYear, weekNumber: Int, numberingSystem: String): String = {
		// use a WeekRangesFormatter to format the week name, obv.
		val formatter = new WeekRangesFormatter(year)
		formatter.format(Seq(WeekRange(weekNumber)), DayOfWeek.Monday, numberingSystem)
	}

	def exec(unused: JList[_]): AnyRef = {
		getWeekRangesAsJSON(formatWeekName)
	}
}


class WholeWeekFormatter(year: AcademicYear)(implicit termService: TermService) extends WeekRanges(year: AcademicYear) {

	implicit class PimpedTerm(termOrVacation: Term) {
		def print(weekRange: WeekRange, dayOfWeek: DayOfWeek, numberingSystem: String, short: Boolean): String = {
			val startDate = weekNumberToDate(weekRange.minWeek, dayOfWeek)
			val endDate = weekNumberToDate(weekRange.maxWeek, dayOfWeek)
			val Monday = DayOfWeek.Monday.getAsInt

			termOrVacation match {
				case vac: Vacation if numberingSystem == WeekRange.NumberingSystem.Academic =>
					if (short) {
						if (weekRange.isSingleWeek) weekRange.minWeek.toString
						else "%d-%d" format (weekRange.minWeek, weekRange.maxWeek)
					} else {
						if (weekRange.isSingleWeek) "%s, week %d" format (vac.getTermTypeAsString, weekRange.minWeek)
						else "%s, weeks %d-%d" format (vac.getTermTypeAsString, weekRange.minWeek, weekRange.maxWeek)
					}
				case vac: Vacation =>
					if (weekRange.isSingleWeek) {
						if (short) startDate.withDayOfWeek(Monday).toString("dd/MM")
						else "%s, w/c %s" format (
							vac.getTermTypeAsString,
							IntervalFormatter.format(startDate.withDayOfWeek(Monday), includeTime = false)
						)
					} else {
						// Date range
						if (short)
							"%s, %s-%s" format (
								vac.getTermTypeAsString,
								startDate.withDayOfWeek(Monday).toString("dd/MM"),
								endDate.withDayOfWeek(Monday).toString("dd/MM")
							)
						else
							"%s, %s" format (
								vac.getTermTypeAsString,
								IntervalFormatter.format(startDate.withDayOfWeek(Monday), endDate.withDayOfWeek(Monday), includeTime = false).replaceAll("Mon", "w/c Mon")
							)
					}
				case term =>
					// Convert week numbers to the correct style
					val termNumber = term.getTermType match {
						case Term.TermType.autumn => 1
						case Term.TermType.spring => 2
						case Term.TermType.summer => 3
					}

					def weekNumber(date: DateTime) =
						numberingSystem match {
							case WeekRange.NumberingSystem.Term => term.getWeekNumber(date)
							case WeekRange.NumberingSystem.Cumulative => term.getCumulativeWeekNumber(date)
							case _ => term.getAcademicWeekNumber(date)
						}

					if (short) {
						if (weekRange.isSingleWeek) weekNumber(startDate).toString
						else "Term %d, %d-%d" format (termNumber, weekNumber(startDate), weekNumber(endDate))
					} else {
						if (weekRange.isSingleWeek) "Term %d, week %d" format (termNumber, weekNumber(startDate))
						else "Term %d, weeks %d-%d" format (termNumber, weekNumber(startDate), weekNumber(endDate))
					}
			}
		}
	}


	def format(ranges: Seq[WeekRange], dayOfWeek: DayOfWeek, numberingSystem: String, short: Boolean): String = {
		import WeekRangesFormatter._

		numberingSystem match {
			case WeekRange.NumberingSystem.None =>
				ranges.map { weekRange =>
					val Monday = DayOfWeek.Monday
					val startDate = weekNumberToDate(weekRange.minWeek, Monday)
					val endDate = weekNumberToDate(weekRange.maxWeek, Monday)

					if (short) {
						if (weekRange.isSingleWeek) startDate.toString("dd/MM")
						else startDate.toString("dd/MM") + " - " + endDate.toString("dd/MM")
					} else {
						IntervalFormatter.format(startDate, endDate, includeTime = false).replaceAll("Mon", "w/c Mon")
					}
				}.mkString(separator)
			case _ =>
				groupWeekRangesByTerm(ranges, dayOfWeek).map {
					case (weekRange, term) =>
						term.print(weekRange, dayOfWeek, numberingSystem, short)
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
	def format(ranges: Seq[WeekRange], dayOfWeek: DayOfWeek, year: AcademicYear, numberingSystem: String, short: Boolean)(implicit termService: TermService): String =
		formatterMap.retrieve(year) format (ranges, dayOfWeek, numberingSystem, short)

	class WholeWeekFormatterCache {
		private val map = JConcurrentMap[AcademicYear, WholeWeekFormatter]()
		def retrieve(year: AcademicYear)(implicit termService: TermService): WholeWeekFormatter = map.getOrElseUpdate(year, new WholeWeekFormatter(year))
	}
}

/* Freemarker companion for the WholeWeekFormatter */

class WholeWeekFormatterTag extends TemplateMethodModelEx with KnowsUserNumberingSystem with AutowiringUserSettingsServiceComponent with AutowiringTermServiceComponent {
	import WholeWeekFormatter.format

	/** Pass through all the arguments  */
	override def exec(list: JList[_]): String = {
		val user = RequestInfo.fromThread.get.user

		val args = list.asScala.toSeq.map { model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel]) }
		args match {
			case Seq(startWeekNumber: JInteger, endWeekNumber: JInteger, academicYear: AcademicYear, dept: Department, short: JBoolean) =>
				format(Seq(WeekRange(startWeekNumber, endWeekNumber)), DayOfWeek.Thursday, academicYear, numberingSystem(user, Some(dept)), short)

			case Seq(startWeekNumber: JInteger, endWeekNumber: JInteger, academicYear: AcademicYear, short: JBoolean) =>
				format(Seq(WeekRange(startWeekNumber, endWeekNumber)), DayOfWeek.Thursday, academicYear, WeekRange.NumberingSystem.None, short)

			case _ => throw new IllegalArgumentException("Bad args: " + args)
		}
	}
}



