package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase

class StudentCourseYearDetailsTest extends TestBase with Mockito {

	@Test def comparison{
		val student = new StudentMember("0205225")

		val scd = new StudentCourseDetails(student, "0205225/1")

		val scyd_2012_1 = new StudentCourseYearDetails(scd, 1, AcademicYear(2012))

		val scyd_2012_2 = new StudentCourseYearDetails(scd, 2,AcademicYear(2012))

		val scyd_2012_3 = new StudentCourseYearDetails(scd, 3,AcademicYear(2012))

		val scyd_2013_1 = new StudentCourseYearDetails(scd, 4,AcademicYear(2013))

		val scyd_2013_2 = new StudentCourseYearDetails(scd, 5,AcademicYear(2013))

		val scyd_2013_3 = new StudentCourseYearDetails(scd, 6,AcademicYear(2013))

		// latest academic year should come back first
		scd.addStudentCourseYearDetails(scyd_2013_1)
		scd.addStudentCourseYearDetails(scyd_2012_2)

		scd.freshStudentCourseYearDetails.max should be (scyd_2013_1)

		// latest sequence number should come back first

		scd.removeStudentCourseYearDetails(scyd_2013_1)
		scd.removeStudentCourseYearDetails(scyd_2012_2)

		scd.addStudentCourseYearDetails(scyd_2012_2)
		scd.addStudentCourseYearDetails(scyd_2012_1)

		scd.freshStudentCourseYearDetails.max should be (scyd_2012_2)

		// year should win over sequence number
		scd.removeStudentCourseYearDetails(scyd_2012_2)
		scd.removeStudentCourseYearDetails(scyd_2012_1)

		scd.addStudentCourseYearDetails(scyd_2013_1)
		scd.addStudentCourseYearDetails(scyd_2012_2)

		scd.freshStudentCourseYearDetails.max should be (scyd_2013_1)
	}
}