package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroup}

class SmallGroupEventIdConverter extends TwoWayConverter[String, SmallGroupEvent] {

	@Autowired var service: SmallGroupService = _

	override def convertRight(id: String): SmallGroupEvent = (Option(id) flatMap { service.getSmallGroupEventById(_) }).orNull
	override def convertLeft(group: SmallGroupEvent): String = (Option(group) map {_.id}).orNull

}
