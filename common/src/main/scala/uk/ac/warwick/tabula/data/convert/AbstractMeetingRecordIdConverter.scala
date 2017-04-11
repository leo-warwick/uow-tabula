package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.AbstractMeetingRecord
import uk.ac.warwick.tabula.data.MeetingRecordDao
import uk.ac.warwick.tabula.system.TwoWayConverter


class AbstractMeetingRecordIdConverter extends TwoWayConverter[String, AbstractMeetingRecord] {

	@Autowired var dao: MeetingRecordDao = _

	override def convertRight(id: String): AbstractMeetingRecord = (Option(id) flatMap { dao.get }).orNull

	override def convertLeft(meetingRecord: AbstractMeetingRecord): String = (Option(meetingRecord) map {_.id}).orNull

}