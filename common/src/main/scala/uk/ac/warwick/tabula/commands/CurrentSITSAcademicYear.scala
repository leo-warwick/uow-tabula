package uk.ac.warwick.tabula.commands

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear

trait CurrentSITSAcademicYear extends HasAcademicYear {

	var academicYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(new DateTime)

	def getAcademicYearString: String = Option(academicYear).map(_.toString).getOrElse("")

}
