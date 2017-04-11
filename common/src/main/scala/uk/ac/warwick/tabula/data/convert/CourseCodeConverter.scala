package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.Course
import uk.ac.warwick.tabula.services.CourseAndRouteService
import uk.ac.warwick.tabula.system.TwoWayConverter

class CourseCodeConverter extends TwoWayConverter[String, Course] {

	@Autowired var service: CourseAndRouteService = _

	override def convertRight(code: String): Course =
		service.getCourseByCode(sanitise(code)).orNull

	override def convertLeft(course: Course): String = (Option(course) map { _.code }).orNull

	def sanitise(code: String): String = {
		if (code == null) throw new IllegalArgumentException
		else code
	}

}
