package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod
import uk.ac.warwick.tabula.system.TwoWayConverter

class SmallGroupAllocationMethodConverter extends TwoWayConverter[String, SmallGroupAllocationMethod] {

	override def convertRight(code: String): SmallGroupAllocationMethod = SmallGroupAllocationMethod.fromDatabase(code)
	override def convertLeft(allocationMethod: SmallGroupAllocationMethod): String = Option(allocationMethod).map { _.dbValue }.orNull

}
