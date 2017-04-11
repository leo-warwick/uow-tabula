package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.data.model.MeetingFormat
import uk.ac.warwick.tabula.system.TwoWayConverter

class MeetingFormatConverter extends TwoWayConverter[String, MeetingFormat] {

	override def convertRight(code: String): MeetingFormat = MeetingFormat.fromCode(code)
	override def convertLeft(format: MeetingFormat): String = (Option(format) map { _.code }).orNull
}