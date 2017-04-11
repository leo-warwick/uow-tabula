package uk.ac.warwick.tabula.data

import org.hibernate.criterion.{Order, Projections, Restrictions}
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.TaskBenchmarking
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
	def countPendingApprovals(universityId: String): Int
	def get(id: String): Option[AbstractMeetingRecord]
	def purge(meeting: AbstractMeetingRecord): Unit
	def migrate(from: StudentRelationship, to: StudentRelationship): Unit
	def unconfirmedScheduledCount(relationships: Seq[StudentRelationship]): Map[StudentRelationship, Int]
}

@Repository
class MeetingRecordDaoImpl extends MeetingRecordDao with Daoisms with TaskBenchmarking {

	def saveOrUpdate(meeting: MeetingRecord): Unit = session.saveOrUpdate(meeting)

	def saveOrUpdate(scheduledMeeting: ScheduledMeetingRecord): Unit = session.saveOrUpdate(scheduledMeeting)

	def saveOrUpdate(meeting: AbstractMeetingRecord): Unit = session.saveOrUpdate(meeting)

	def saveOrUpdate(approval: MeetingRecordApproval): Unit = session.saveOrUpdate(approval)

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

	def countPendingApprovals(universityId: String): Int = {
		session.newCriteria[MeetingRecordApproval]
			.createAlias("meetingRecord", "meetingRecord")
			.add(is("approver.universityId", universityId))
			.add(is("state", MeetingApprovalState.Pending))
			.add(is("meetingRecord.deleted", false))
			.count.intValue()
	}

	def get(id: String): Option[AbstractMeetingRecord] = getById[AbstractMeetingRecord](id)

	def purge(meeting: AbstractMeetingRecord): Unit = {
		session.delete(meeting)
		session.flush()
	}

	def migrate(from: StudentRelationship, to: StudentRelationship): Unit = {
		session.newQuery("""
			update AbstractMeetingRecord
			set relationship = :to
	 		where relationship = :from
		""")
			.setParameter("from", from)
			.setParameter("to", to)
			.executeUpdate()
	}

	def unconfirmedScheduledCount(relationships: Seq[StudentRelationship]): Map[StudentRelationship, Int] = benchmarkTask("unconfirmedScheduledCount") {
		safeInSeqWithProjection[ScheduledMeetingRecord, Array[java.lang.Object]](
			() => {
				session.newCriteria[ScheduledMeetingRecord]
					.add(Restrictions.lt("meetingDate", DateTime.now))
			},
			Projections.projectionList()
				.add(Projections.groupProperty("relationship"))
				.add(Projections.count("relationship")),
			"relationship",
			relationships
		).map { objArray =>
			objArray(0).asInstanceOf[StudentRelationship] -> objArray(1).asInstanceOf[Long].toInt
		}.toMap
	}
}

trait MeetingRecordDaoComponent {
	val meetingRecordDao: MeetingRecordDao
}
trait AutowiringMeetingRecordDaoComponent extends MeetingRecordDaoComponent{
	val meetingRecordDao: MeetingRecordDao = Wire.auto[MeetingRecordDao]
}
