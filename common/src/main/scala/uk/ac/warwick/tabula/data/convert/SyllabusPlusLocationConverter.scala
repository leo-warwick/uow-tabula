package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.SyllabusPlusLocationService
import uk.ac.warwick.tabula.system.TwoWayConverter

class SyllabusPlusLocationConverter extends TwoWayConverter[String, SyllabusPlusLocation] {
	@Autowired var service: SyllabusPlusLocationService = _

	override def convertRight(id: String): SyllabusPlusLocation = service.getById(id).orNull

	override def convertLeft(location: SyllabusPlusLocation): String = Option(location).map(_.id).orNull
}