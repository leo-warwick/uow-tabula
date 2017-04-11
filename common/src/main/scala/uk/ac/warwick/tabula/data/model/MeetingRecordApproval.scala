package uk.ac.warwick.tabula.data.model

import java.sql.Types
import javax.persistence._

import org.hibernate.`type`.StandardBasicTypes
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@Entity
class MeetingRecordApproval extends GeneratedId with ToEntityReference with PermissionsTarget {
	type Entity = MeetingRecordApproval

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "meetingrecord_id")
	var meetingRecord: MeetingRecord = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="approver_id")
	var approver: Member = _

	@Column(name="approval_state")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.MeetingApprovalStateUserType")
	var state: MeetingApprovalState = _

	@Column(name="creation_date")
	var creationDate: DateTime = DateTime.now

	var lastUpdatedDate: DateTime = creationDate

	var comments: String = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="approved_by")
	var approvedBy: Member = _

	override def toEntityReference: EntityReference[MeetingRecordApproval] = new MeetingRecordApprovalEntityReference().put(this)

	override def permissionsParents: Stream[PermissionsTarget] = Stream(meetingRecord)
}

sealed abstract class MeetingApprovalState(val code: String, val description: String) {
	override def toString: String = description
}

object MeetingApprovalState {
	case object Pending extends MeetingApprovalState("pending", "Pending approval")
	case object Approved extends MeetingApprovalState("approved", "Approved")
	case object Rejected extends MeetingApprovalState("rejected", "Rejected")

	// lame manual collection. Keep in sync with the case objects above
	val states = Set(Pending, Approved, Rejected)

	def fromCode(code: String): MeetingApprovalState =
		if (code == null) null
		else states.find{_.code == code} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}

	def fromDescription(description: String): MeetingApprovalState =
		if (description == null) null
		else states.find{_.description == description} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}
}

class MeetingApprovalStateUserType extends AbstractBasicUserType[MeetingApprovalState, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String): MeetingApprovalState = MeetingApprovalState.fromCode(string)
	override def convertToValue(state: MeetingApprovalState): String = state.code
}
