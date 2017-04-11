package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.data.StudentCourseDetailsDao
import uk.ac.warwick.spring.Wire

class StudentCourseDetailsConverter extends TwoWayConverter[String, StudentCourseDetails] {

	var service: StudentCourseDetailsDao = Wire.auto[StudentCourseDetailsDao]

	// parse
	override def convertRight(scjCode: String): StudentCourseDetails = {
		val scjCodeDecoded = scjCode.replace("_","/")
		service.getByScjCode(scjCodeDecoded).orElse(service.getByScjCodeStaleOrFresh(scjCodeDecoded)).orNull
	}

	// print
	override def convertLeft(studentCourseDetails: StudentCourseDetails): String = (Option(studentCourseDetails) map {_.scjCode}).orNull

}
