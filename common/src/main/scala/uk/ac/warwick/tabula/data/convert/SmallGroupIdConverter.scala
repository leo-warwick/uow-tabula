package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.groups.SmallGroup

class SmallGroupIdConverter extends TwoWayConverter[String, SmallGroup] {

	@Autowired var service: SmallGroupService = _

	override def convertRight(id: String): SmallGroup = (Option(id) flatMap { service.getSmallGroupById(_) }).orNull
	override def convertLeft(group: SmallGroup): String = (Option(group) map {_.id}).orNull

}
