package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroup}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence

class SmallGroupEventOccurrenceIdConverter extends TwoWayConverter[String, SmallGroupEventOccurrence] {

	@Autowired var service: SmallGroupService = _

	override def convertRight(id: String): SmallGroupEventOccurrence = (Option(id) flatMap { service.getSmallGroupEventOccurrenceById }).orNull
	override def convertLeft(group: SmallGroupEventOccurrence): String = (Option(group) map {_.id}).orNull

}
