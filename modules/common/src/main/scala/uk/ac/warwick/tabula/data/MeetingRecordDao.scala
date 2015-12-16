package uk.ac.warwick.tabula.data

import org.hibernate.criterion.{Order, Restrictions}
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

trait MeetingRecordDao {
	def saveOrUpdate(meeting: MeetingRecord)
	def saveOrUpdate(scheduledMeeting: ScheduledMeetingRecord)
	def saveOrUpdate(meeting: AbstractMeetingRecord)
	def saveOrUpdate(approval: MeetingRecordApproval)
	def listScheduled(rel: Set[StudentRelationship], currentUser: Option[Member]): Seq[ScheduledMeetingRecord]
	def list(rel: Set[StudentRelationship], currentUser: Option[Member]): Seq[MeetingRecord]
	def list(rel: StudentRelationship): Seq[MeetingRecord]
	def listScheduled(rel: StudentRelationship): Seq[ScheduledMeetingRecord]
	def get(id: String): Option[AbstractMeetingRecord]
	def purge(meeting: AbstractMeetingRecord): Unit
	def migrate(from: StudentRelationship, to: StudentRelationship): Unit
}

@Repository
class MeetingRecordDaoImpl extends MeetingRecordDao with Daoisms {

	def saveOrUpdate(meeting: MeetingRecord) = session.saveOrUpdate(meeting)

	def saveOrUpdate(scheduledMeeting: ScheduledMeetingRecord) = session.saveOrUpdate(scheduledMeeting)

	def saveOrUpdate(meeting: AbstractMeetingRecord) = session.saveOrUpdate(meeting)

	def saveOrUpdate(approval: MeetingRecordApproval) = session.saveOrUpdate(approval)

	def list(rel: Set[StudentRelationship], currentUser: Option[Member]): Seq[MeetingRecord] = {
		if (rel.isEmpty)
			Seq()
		else
			addMeetingRecordListRestrictionsAndList(() => session.newCriteria[MeetingRecord], rel, currentUser)
	}

	def listScheduled(rel: Set[StudentRelationship], currentUser: Option[Member]): Seq[ScheduledMeetingRecord] = {
		if (rel.isEmpty)
			Seq()
		else
			addMeetingRecordListRestrictionsAndList(() => session.newCriteria[ScheduledMeetingRecord], rel, currentUser)
	}

	private def addMeetingRecordListRestrictionsAndList[A <: AbstractMeetingRecord](criteriaFactory: () => ScalaCriteria[A], rel: Set[StudentRelationship], currentUser: Option[Member]): Seq[A] = {
		// only pick records where deleted = 0 or the current user id is the creator id
		// - so that no-one can see records created and deleted by someone else
		val c = () => {
			val criteria = criteriaFactory.apply()

			currentUser match {
				case None | Some(_: RuntimeMember) => criteria.add(is("deleted", false))
				case Some(cu) => criteria.add(Restrictions.disjunction()
					.add(is("deleted", false))
					.add(is("creator", cu))
				)
			}

			criteria.addOrder(Order.desc("meetingDate")).addOrder(Order.desc("lastUpdatedDate"))
		}

		val meetings = safeInSeq(c, "relationship", rel.toSeq)
		meetings.sortBy(m => (m.meetingDate, m.lastUpdatedDate))(Ordering[(DateTime,DateTime)].reverse)
	}

	def list(rel: StudentRelationship): Seq[MeetingRecord] = {
		session.newCriteria[MeetingRecord]
			.add(Restrictions.eq("relationship", rel))
			.add(is("deleted", false))
			.addOrder(Order.desc("meetingDate"))
			.addOrder(Order.desc("lastUpdatedDate"))
			.seq
	}

	def listScheduled(rel: StudentRelationship): Seq[ScheduledMeetingRecord] = {
		session.newCriteria[ScheduledMeetingRecord]
			.add(Restrictions.eq("relationship", rel))
			.add(is("deleted", false))
			.addOrder(Order.desc("meetingDate"))
			.addOrder(Order.desc("lastUpdatedDate"))
			.seq
	}


	def get(id: String) = session.getById[AbstractMeetingRecord](id)

	def purge(meeting: AbstractMeetingRecord): Unit = {
		session.delete(meeting)
		session.flush()
	}

	def migrate(from: StudentRelationship, to: StudentRelationship) = {
		session.newQuery("""
			update AbstractMeetingRecord
			set relationship = :to
	 		where relationship = :from
		""")
			.setParameter("from", from)
			.setParameter("to", to)
			.executeUpdate()
	}
}

trait MeetingRecordDaoComponent {
	val meetingRecordDao: MeetingRecordDao
}
trait AutowiringMeetingRecordDaoComponent extends MeetingRecordDaoComponent{
	val meetingRecordDao = Wire.auto[MeetingRecordDao]
}
