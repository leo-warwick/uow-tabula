package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.data.MeetingRecordDao
import uk.ac.warwick.tabula.system.TwoWayConverter


class MeetingRecordIdConverter extends TwoWayConverter[String, MeetingRecord] {

	@Autowired var dao: MeetingRecordDao = _

	override def convertRight(id: String): MeetingRecord = (Option(id) flatMap {
		meetingId => dao.get(meetingId).map {
			case (meeting: MeetingRecord) => meeting
			case _ => null
		}
	}).orNull

	override def convertLeft(meetingRecord: MeetingRecord): String = (Option(meetingRecord) map {_.id}).orNull

}