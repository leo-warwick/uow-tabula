package uk.ac.warwick.tabula.data.convert

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.TermService
import uk.ac.warwick.tabula.system.TwoWayConverter

/**
 * Stores academic year as the 4-digit starting year.
 */
class AcademicYearConverter extends TwoWayConverter[String, AcademicYear] {

	implicit var termService: TermService = Wire[TermService]

	override def convertRight(year: String): AcademicYear =
		if ("current" == year) AcademicYear.findAcademicYearContainingDate(DateTime.now)
		else if ("sits" == year) AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		else if (year.hasText)
			try { AcademicYear.parse(year) } catch {
				case e: IllegalArgumentException =>
					try { new AcademicYear(year.toInt) } catch { case e: NumberFormatException => null }
			}
		else null

	override def convertLeft(year: AcademicYear): String = Option(year) match {
		case Some(year) => year.startYear.toString
		case None => null
	}

}