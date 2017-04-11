package uk.ac.warwick.tabula

import uk.ac.warwick.util.termdates.{TermFactoryImpl, Term, TermFactory}
import org.joda.time.base.BaseDateTime
import uk.ac.warwick.util.termdates.Term.TermType
import org.joda.time.{DateTimeConstants, DateTime}

/**
 * Various bits of logic cherry-picked from TermFactory, VacationAwareTermFactory, WeekToDateConverter et al
 * to allow functional tests to work with academic dates. Note the case class names are prefixed with FunctionalTest
 * where they would otherwise clash with pre-existing classes elsewhere in the tabula codebase, to avoid picking the
 * wrong one in the IDE
 *
 */
class AcademicDateHelper(val delegate: TermFactory) extends AnyVal {

	def getAcademicYearContainingDate(date: BaseDateTime): FunctionalTestAcademicYear = {
		val firstWeekOfYear = getFirstTermOfYearContaining(date).getStartDate
		FunctionalTestAcademicYear(firstWeekOfYear.getYear)
	}

	def getFirstTermOfYearContaining(date:BaseDateTime):Term = {
		val termContainingIntervalStart = getTermFromDateIncludingVacations(date)
		def findAutumnTermForTerm(term: Term): Term = {
			term.getTermType match {
				case TermType.autumn => term
				case _ => findAutumnTermForTerm(delegate.getPreviousTerm(term))
			}
		}
		findAutumnTermForTerm(termContainingIntervalStart)
	}

	def getTermFromDateIncludingVacations(date: BaseDateTime): Term = {
		val term = delegate.getTermFromDate(date)
		if (date.isBefore(term.getStartDate)) FunctionalTestVacation(delegate.getPreviousTerm(term), term)
		else term
	}
}

case class FunctionalTestVacation(before: Term, after: Term) extends Term {
	// Starts the day after the previous term and ends the day before the new term
	def getStartDate: DateTime = before.getEndDate.plusDays(1)

	def getEndDate: DateTime = after.getStartDate.minusDays(1)

	def getTermType = null

	def getTermTypeAsString: String = before.getTermType match {
		case Term.TermType.autumn => "Christmas vacation"
		case Term.TermType.spring => "Easter vacation"
		case Term.TermType.summer => "Summer vacation"
	}

	def getWeekNumber(date: BaseDateTime) = throw new IllegalStateException("Can't get week numbers from a vacation")

	def getCumulativeWeekNumber(date: BaseDateTime) = throw new IllegalStateException("Can't get week numbers from a vacation")

	def getAcademicWeekNumber(date: BaseDateTime): Int = after.getAcademicWeekNumber(date)
}

case class FunctionalTestAcademicYear(startYear: Int) extends Ordered[FunctionalTestAcademicYear] {
	val endYear: Int = startYear + 1
	def compare(that:FunctionalTestAcademicYear): Int = {
		this.startYear - that.startYear
	}
	def toSyllabusPlusFormat:String = (startYear%100).toString + ((startYear+1)%100).toString
	override def toString = s"${startYear%100}/${endYear%100}"
}

object FunctionalTestAcademicYear{
	private lazy val termFactory = new TermFactoryImpl
	def current: FunctionalTestAcademicYear = new AcademicDateHelper(termFactory).getAcademicYearContainingDate(DateTime.now)
	def currentSITS: FunctionalTestAcademicYear =
		if (DateTime.now.getMonthOfYear >= DateTimeConstants.AUGUST) {
			FunctionalTestAcademicYear(DateTime.now.getYear)
		} else {
			FunctionalTestAcademicYear(DateTime.now.getYear - 1)
		}
}