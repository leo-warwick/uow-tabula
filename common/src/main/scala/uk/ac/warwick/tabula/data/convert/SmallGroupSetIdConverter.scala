package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet

class SmallGroupSetIdConverter extends TwoWayConverter[String, SmallGroupSet] {

	@Autowired var service: SmallGroupService = _

	override def convertRight(id: String): SmallGroupSet = (Option(id) flatMap { service.getSmallGroupSetById(_) }).orNull
	override def convertLeft(set: SmallGroupSet): String = (Option(set) map {_.id}).orNull

}