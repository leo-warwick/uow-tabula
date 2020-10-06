package uk.ac.warwick.tabula.services

import org.joda.time.{DateTime, LocalDate}
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringMeetingRecordDaoComponent, MeetingRecordDaoComponent}

trait MeetingRecordServiceComponent {
  def meetingRecordService: MeetingRecordService
}

trait AutowiringMeetingRecordServiceComponent extends MeetingRecordServiceComponent {
  var meetingRecordService: MeetingRecordService = Wire[MeetingRecordService]
}

trait MeetingRecordService {

  def saveOrUpdate(meeting: MeetingRecord): Unit

  def saveOrUpdate(scheduledMeeting: ScheduledMeetingRecord): Unit

  def saveOrUpdate(meeting: AbstractMeetingRecord): Unit

  def saveOrUpdate(approval: MeetingRecordApproval): Unit

  def list(rel: Set[StudentRelationship], currentMember: Option[Member]): Seq[MeetingRecord]

  def listScheduled(rel: Set[StudentRelationship], currentMember: Option[Member]): Seq[ScheduledMeetingRecord]

  def listAll(rel: Set[StudentRelationship], currentMember: Option[Member]): Seq[AbstractMeetingRecord]

  def list(rel: StudentRelationship): Seq[MeetingRecord]

  def listAll(rel: StudentRelationship): Seq[AbstractMeetingRecord]

  def countAll(rel: StudentRelationship): Int

  def countPendingApprovals(universityId: String): Int

  def get(id: String): Option[AbstractMeetingRecord]

  def purge(meeting: AbstractMeetingRecord): Unit

  def purge(approval: MeetingRecordApproval): Unit

  def getAcademicYear(meeting: AbstractMeetingRecord): Option[AcademicYear]

  def getAcademicYear(id: String): Option[AcademicYear]

  def migrate(from: StudentRelationship, to: StudentRelationship): Unit

  def unconfirmedScheduledCount(relationships: Seq[StudentRelationship]): Map[StudentRelationship, Int]

  def listBetweenDates(student: StudentMember, startInclusive: DateTime, endExclusive: DateTime): Seq[MeetingRecord]

  def listAllOnOrAfter(localDate: LocalDate): Seq[MeetingRecord]
}


abstract class AbstractMeetingRecordService extends MeetingRecordService {
  self: MeetingRecordDaoComponent =>

  def saveOrUpdate(meeting: MeetingRecord): Unit = meetingRecordDao.saveOrUpdate(meeting)

  def saveOrUpdate(scheduledMeeting: ScheduledMeetingRecord): Unit = meetingRecordDao.saveOrUpdate(scheduledMeeting)

  def saveOrUpdate(meeting: AbstractMeetingRecord): Unit = meetingRecordDao.saveOrUpdate(meeting)

  def saveOrUpdate(approval: MeetingRecordApproval): Unit = meetingRecordDao.saveOrUpdate(approval)

  def list(rel: Set[StudentRelationship], currentMember: Option[Member]): Seq[MeetingRecord] = meetingRecordDao.list(rel, currentMember)

  def listScheduled(rel: Set[StudentRelationship], currentMember: Option[Member]): Seq[ScheduledMeetingRecord] =
    meetingRecordDao.listScheduled(rel, currentMember)

  def list(rel: StudentRelationship): Seq[MeetingRecord] = meetingRecordDao.list(rel)

  def listAll(rel: Set[StudentRelationship], currentMember: Option[Member]): Seq[AbstractMeetingRecord] = transactional(readOnly = true) {
    (meetingRecordDao.list(rel, currentMember) ++ meetingRecordDao.listScheduled(rel, currentMember)).sorted
  }

  def listAll(rel: StudentRelationship): Seq[AbstractMeetingRecord] = {
    (meetingRecordDao.list(rel) ++ meetingRecordDao.listScheduled(rel)).sorted
  }

  def countAll(rel: StudentRelationship): Int =
    meetingRecordDao.count(rel) + meetingRecordDao.countScheduled(rel)

  def countPendingApprovals(universityId: String): Int = meetingRecordDao.countPendingApprovals(universityId)

  def get(id: String): Option[AbstractMeetingRecord] = Option(id) match {
    case Some(uid) if uid.nonEmpty => meetingRecordDao.get(uid)
    case _ => None
  }

  def purge(meeting: AbstractMeetingRecord): Unit = meetingRecordDao.purge(meeting)

  def purge(approval: MeetingRecordApproval): Unit = meetingRecordDao.purge(approval)

  def getAcademicYear(meeting: AbstractMeetingRecord): Option[AcademicYear] = Some(AcademicYear.forDate(meeting.meetingDate))

  def getAcademicYear(id: String): Option[AcademicYear] = Option(id).flatMap(get).flatMap(getAcademicYear)

  def migrate(from: StudentRelationship, to: StudentRelationship): Unit =
    meetingRecordDao.migrate(from, to)

  def unconfirmedScheduledCount(relationships: Seq[StudentRelationship]): Map[StudentRelationship, Int] =
    meetingRecordDao.unconfirmedScheduledCount(relationships)

  override def listBetweenDates(student: StudentMember, startInclusive: DateTime, endExclusive: DateTime): Seq[MeetingRecord] = meetingRecordDao.listBetweenDates(student, startInclusive, endExclusive)

  override def listAllOnOrAfter(localDate: LocalDate): Seq[MeetingRecord] = meetingRecordDao.listAllOnOrAfter(localDate)
}

@Service("meetingRecordService")
class MeetingRecordServiceImpl
  extends AbstractMeetingRecordService
    with AutowiringMeetingRecordDaoComponent
