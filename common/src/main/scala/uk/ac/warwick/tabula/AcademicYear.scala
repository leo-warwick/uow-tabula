package uk.ac.warwick.tabula

import scala.language.implicitConversions
import org.joda.time.{LocalDate, DateTimeConstants, DateTime}
import org.joda.time.DateTimeConstants._
import org.joda.time.base.BaseDateTime
import uk.ac.warwick.util.termdates.{TermNotFoundException, Term}
import uk.ac.warwick.util.termdates.Term.TermType
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.TermService
import uk.ac.warwick.tabula.data.model.Convertible


/**
 * Represents a particular academic year. Traditionally they are displayed as
 * "99/00" or "11/12" but we just store the first year as a 4-digit number.
 * toString() returns the traditional format.
 */
case class AcademicYear(startYear: Int) extends Ordered[AcademicYear] with Convertible[JInteger] {
	val endYear: Int = startYear + 1
	if (endYear > 9999 || startYear < 1000) throw new IllegalArgumentException()

	override def toString: String = "%s/%s".format(toDigits(startYear), toDigits(endYear))
	private def toDigits(year: Int) = year.toString.substring(2)

	// properties for binding to dropdown box
	def getStoreValue: Int = startYear
	def getLabel: String = toString
	def value: JInteger = startYear
	def previous = new AcademicYear(startYear - 1)
	def next = new AcademicYear(startYear + 1)

	def - (i: Int) = new AcademicYear(startYear - i)
	def + (i: Int) = new AcademicYear(startYear + i)

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
		Iterable.iterate(first, length) { y => y.next }.toSeq
	}

	/**
	 * Returns a date guaranteed* to be some time in the first term of the specified year,
	 * suitable for passing to TermFactory.getAcademicWeeksForYear
	 *
	 *  *Restrictions apply. Always read the small print. We are confident
	 *   that November 1st is always in term 1 of the year
	 */
	def dateInTermOne: DateTime =	new LocalDate(startYear, DateTimeConstants.NOVEMBER, 1).toDateTimeAtStartOfDay

	def compare(that:AcademicYear): Int = {
			this.startYear - that.startYear
	}

	def getYear(date: LocalDate): Int = {
		if (date.getMonthOfYear < 10) endYear else startYear
	}

	def isSITSInFlux(date: DateTime): Boolean = {
		val juneThisYear = new LocalDate(this.endYear, DateTimeConstants.JUNE, 1).toDateTimeAtStartOfDay
		juneThisYear.isBefore(date)
	}

}

object AcademicYear {

	private val SitsPattern = """(\d{2})/(\d{2})""".r
	/**
	 * We're only dealing with current years, not DOBs or anything, so can afford
	 * to make the century break large. I don't think there is even any module data in SITS
	 * from before 2004, so could even do without this check.
	 *
	 * Anyway, this will only break near the year 2090.
	 */
	private val CenturyBreak = 90

	// An implicit for the UserType to create instances
	implicit val factory: (_root_.uk.ac.warwick.tabula.JavaImports.JInteger) => AcademicYear = (year: JInteger) => AcademicYear(year)

	def parse(string: String): AcademicYear = string match {
		case SitsPattern(year1, year2) => AcademicYear(parseTwoDigits(year1))
		case _ => throw new IllegalArgumentException("Did not match YY/YY: " + string)
	}

	/**
	 * Academic years in SITS are only 2 digits so we need to be able to parse them.
	 * Assume that it's almost always a 20XX date.
	 */
	private def parseTwoDigits(twoDigitYear: String) = twoDigitYear.toInt match {
		case y if y > CenturyBreak => 1900 + y
		case y => 2000 + y
	}


	/**
	 * n.b. this does *not* tell you what academic year the date "now" lies within.
	 *
	 * e.g. Sept. 1st 2012 (Academic week 48, year 2011-12) will return year 2012-13.
	 *
	 * This function returns the year based on when SITS rolls over,
	 * not when the academic year starts/stops
	 */
	def guessSITSAcademicYearByDate(now: BaseDateTime): AcademicYear = {
		if (now.getMonthOfYear >= AUGUST) {
			new AcademicYear(now.getYear)
		} else {
			new AcademicYear(now.getYear - 1)
		}
	}

	/**
	 * This will tell you which academic year you're currently in, assuming that the year starts on day 1 of week 1 in term 1
	 *
	 */
	def findAcademicYearContainingDate(date: BaseDateTime)(implicit termService: TermService): AcademicYear = {
		try {
			val termContainingIntervalStart = termService.getTermFromDateIncludingVacations(date)
			def findAutumnTermForTerm(term: Term): Term = {
				term.getTermType match {
					case TermType.autumn => term
					case _ => findAutumnTermForTerm(termService.getPreviousTerm(term))
				}
			}
			val firstWeekOfYear = findAutumnTermForTerm(termContainingIntervalStart).getStartDate
			AcademicYear(firstWeekOfYear.getYear)
		} catch {
			case tnf: TermNotFoundException =>
				// Fall back to guessing behaviour
				guessSITSAcademicYearByDate(date)
		}
	}

}