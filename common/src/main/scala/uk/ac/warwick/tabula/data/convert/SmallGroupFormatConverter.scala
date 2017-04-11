package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat
import uk.ac.warwick.tabula.system.TwoWayConverter

class SmallGroupFormatConverter extends TwoWayConverter[String, SmallGroupFormat] {

	override def convertRight(code: String): SmallGroupFormat = SmallGroupFormat.fromCode(code)
	override def convertLeft(format: SmallGroupFormat): String = Option(format).map { _.code }.orNull

}
