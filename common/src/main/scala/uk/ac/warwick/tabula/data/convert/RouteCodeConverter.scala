package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.services.CourseAndRouteService

class RouteCodeConverter extends TwoWayConverter[String, Route] {

	@Autowired var service: CourseAndRouteService = _

	override def convertRight(code: String): Route =
		service.getRouteByCode(sanitise(code)).getOrElse {
			service.getRouteById(code).orNull
		}

	override def convertLeft(route: Route): String = (Option(route) map { _.code }).orNull

	def sanitise(code: String): String = {
		if (code == null) throw new IllegalArgumentException
		else code.toLowerCase
	}

}
