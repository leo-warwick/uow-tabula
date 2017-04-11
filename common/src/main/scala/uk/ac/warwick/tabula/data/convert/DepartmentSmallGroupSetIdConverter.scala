package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.system.TwoWayConverter

class DepartmentSmallGroupSetIdConverter extends TwoWayConverter[String, DepartmentSmallGroupSet] {

	@Autowired var service: SmallGroupService = _

	override def convertRight(id: String): DepartmentSmallGroupSet = (Option(id) flatMap { service.getDepartmentSmallGroupSetById(_) }).orNull
	override def convertLeft(set: DepartmentSmallGroupSet): String = (Option(set) map {_.id}).orNull

}