package uk.ac.warwick.tabula.data.model

import javax.persistence.CascadeType._
import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy}
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.MeetingRecordApprovalType.{AllApprovals, OneApproval}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent}

import scala.jdk.CollectionConverters._

object MeetingRecord {
  val DefaultMeetingTimeOfDay = 12 // Should be used for legacy meetings (where isRealTime is false)
  val MeetingTooOldThresholdYears = 5
  val MaxTitleLength = 140
  val MaxLocationLength = 255
}

@Entity
@Proxy
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("standard")
class MeetingRecord extends AbstractMeetingRecord {

  def this(creator: Member, relationships: Seq[StudentRelationship]) = {
    this()
    this.creator = creator
    this.relationships = relationships
  }

  @transient
  var securityService: SecurityService = Wire[SecurityService]

  @Column(name = "real_time")
  var isRealTime: Boolean = true

  def toEventOccurrence(context: TimetableEvent.Context): Option[EventOccurrence] = {
    if (isRealTime) {
      this.asEventOccurrence(context)
    } else {
      None
    }
  }

  // Workflow definitions

  @OneToMany(mappedBy = "meetingRecord", fetch = FetchType.LAZY, cascade = Array(ALL))
  @BatchSize(size = 200)
  var approvals: JList[MeetingRecordApproval] = JArrayList()

  // true if the specified user needs to perform a workflow action on this meeting record
  def pendingActionBy(user: CurrentUser): Boolean = pendingApprovalBy(user) || pendingRevisionBy(user)

  // The meeting record is approved when all required approvals are Approved
  def isApproved: Boolean = approvals.asScala.filter(_.required).forall(_.approved)

  // for attendance purposes the meeting is approved if it was created by the agent, or is otherwise approved
  def isAttendanceApproved: Boolean = ((creator != null && relationships.map(_.agent).contains(creator.universityId)) || isApproved) && !missed

  def isPendingApproval: Boolean = approvals.asScala.exists(_.pending)

  def pendingApprovals: Seq[MeetingRecordApproval] = approvals.asScala.toSeq.filter(_.pending)

  def pendingApprovalBy(user: CurrentUser): Boolean =
    approvals.asScala.exists(approval =>
      approval.pending &&
        user.universityId != creator.universityId &&
        (pendingApprovals.exists(_.approver.universityId == user.apparentUser.getWarwickId) ||
          securityService.can(user, Permissions.Profiles.MeetingRecord.Approve, approval))
    )

  def pendingApprovers: List[Member] = pendingApprovals.map(_.approver).toList

  def isRejected: Boolean = approvals.asScala.exists(_.rejected)

  def rejectedApprovals: Seq[MeetingRecordApproval] = approvals.asScala.toSeq.filter(_.rejected)

  def rejectedBy(member: Member): Boolean = rejectedApprovals.exists(_.approver == member)

  // people who have had a draft version rejected
  def pendingRevisionBy(user: CurrentUser): Boolean = isRejected && user.universityId == creator.universityId

  def willBeApprovedFollowingApprovalBy(user: CurrentUser): Boolean = approvalType match {
    case OneApproval => pendingApprovers.map(_.universityId).contains(user.universityId)
    case AllApprovals => pendingApprovers.map(_.universityId).forall(_ == user.universityId)
  }

  import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

  def approvedDate: Option[DateTime] = if (isPendingApproval) None else mostRecentApproval.map(_.lastUpdatedDate)

  def approvedBy: Option[Member] = if (isPendingApproval) None else mostRecentApproval.flatMap(a => Option(a.approvedBy))

  def mostRecentApproval: Option[MeetingRecordApproval] = approvals.asScala.filter(_.approved).sortBy(_.lastUpdatedDate).lastOption

  def pendingApprovalsDescription: String = pendingApprovers.sortBy(p => p.lastName -> p.firstName).flatMap(_.fullName) match {
    case Nil => "nobody"
    case Seq(single) => single
    case init :+ last =>
      val andOr = approvalType match {
        case OneApproval => "or"
        case AllApprovals => "and"
      }
      s"${init.mkString(", ")} $andOr $last"
  }

  def departmentToCheck: Department = student.homeDepartment.subDepartmentsContaining(student).lastOption.getOrElse(student.homeDepartment)

  def approvalType: MeetingRecordApprovalType = departmentToCheck.meetingRecordApprovalType

  // End of workflow definitions
}
