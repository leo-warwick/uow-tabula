package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.tabula.data.model.{StudentCourseYearDetails}
import uk.ac.warwick.tabula.data.StudentCourseYearDetailsDao
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.spring.Wire

class StudentCourseYearDetailsIdConverter extends TwoWayConverter[String, StudentCourseYearDetails]  {
	var dao: StudentCourseYearDetailsDao = Wire.auto[StudentCourseYearDetailsDao]

	// print
	override def convertLeft(scyd: StudentCourseYearDetails): String = Option(scyd).map { _.id }.orNull

	// parse
	override def convertRight(id: String): StudentCourseYearDetails = dao.getStudentCourseYearDetails(id).orNull
}
