package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.data.StudentCourseYearDetailsDao

class StudentCourseYearDetailsIdConverterTest extends TestBase with Mockito {
	val scyd = new StudentCourseYearDetails
	scyd.id = "foo"

	val converter = new StudentCourseYearDetailsIdConverter
	val dao: StudentCourseYearDetailsDao = smartMock[StudentCourseYearDetailsDao]
	converter.dao = dao

	dao.getStudentCourseYearDetails("foo") returns Some(scyd)
	dao.getStudentCourseYearDetails("bar") returns None

	@Test def validInput {

		converter.convertRight("foo") should be (scyd)
	}

	@Test def invalidInput {

		converter.convertRight("bar") should be (null)
	}

	@Test def formatting {
		val scyd = new StudentCourseYearDetails
		scyd.id = "foo"

		converter.convertLeft(scyd) should be ("foo")
		converter.convertLeft(null) should be (null)
	}

}