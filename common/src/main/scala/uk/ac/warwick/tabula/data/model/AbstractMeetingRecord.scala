package uk.ac.warwick.tabula.data.model

import java.sql.Types

import freemarker.core.TemplateHTMLOutputModel
import javax.persistence.CascadeType._
import javax.persistence._
import org.hibernate.`type`.StandardBasicTypes
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.permissions.{Permission, Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.system.permissions.RestrictionProvider
import uk.ac.warwick.tabula.timetables.{EventOccurrence, RelatedUrl, TimetableEvent, TimetableEventType}
import uk.ac.warwick.tabula.{AcademicYear, DateFormats, ToString}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

trait MeetingRecordAttachments {
  var attachments: JList[FileAttachment]

  def removeAttachment(attachment: FileAttachment): Boolean = {
    attachments.remove(attachment)
  }

  def removeAllAttachments(): Unit = attachments.clear()

}

object AbstractMeetingRecord {
  // do not remove - import needed for sorting DateTimes
  import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

  implicit val defaultOrdering: Ordering[AbstractMeetingRecord] = Ordering.by { meeting: AbstractMeetingRecord => (meeting.meetingDate, meeting.lastUpdatedDate) }.reverse
}

@Entity
@Proxy
@Table(name = "meetingrecord")
@DiscriminatorColumn(name = "discriminator", discriminatorType = DiscriminatorType.STRING)
abstract class AbstractMeetingRecord extends GeneratedId with PermissionsTarget with ToString with CanBeDeleted
  with ToEntityReference with MeetingRecordAttachments {

  type Entity = AbstractMeetingRecord

  def isScheduled: Boolean = this match {
    case _: ScheduledMeetingRecord => true
    case _ => false
  }

  @Column(name = "creation_date")
  var creationDate: DateTime = DateTime.now

  @Column(name = "last_updated_date")
  var lastUpdatedDate: DateTime = creationDate

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "relationship_id")
  @deprecated(message = "use relationships instead", since = "2018.9.1")
  var relationship: StudentRelationship = _

  @ManyToMany
  @JoinTable(name = "meetingrecordrelationship", joinColumns = Array(new JoinColumn(name = "meeting_record_id")), inverseJoinColumns = Array(new JoinColumn(name = "relationship_id")))
  @JoinColumn(name = "relationship_id")
  var _relationships: JList[StudentRelationship] = JArrayList()

  def relationships: Seq[StudentRelationship] = {
    if (relationship != null) {
      Seq(relationship)
    } else {
      _relationships.asScala.toSeq
    }
  }

  def relationships_=(r: Seq[StudentRelationship]): Unit = {
    relationship = null

    if (_relationships == null) {
      _relationships = JArrayList(r.asJava)
    } else {
      _relationships.clear()
      _relationships.addAll(r.asJava)
    }
  }

  def replaceParticipant(original: StudentRelationship, replacement: StudentRelationship): Unit = {
    if (relationship == original) {
      relationship = null
      _relationships = JArrayList(replacement)
    } else if (_relationships.contains(original)) {
      _relationships.remove(original)
      _relationships.add(replacement)
    }
  }

  @Column(name = "meeting_date")
  @DateTimeFormat(pattern = DateFormats.DateTimePickerPattern)
  var meetingDate: DateTime = _

  @Column(name = "meeting_end_date")
  @DateTimeFormat(pattern = DateFormats.DateTimePickerPattern)
  var meetingEndDate: DateTime = _


  @Column(name = "meeting_location")
  @Type(`type` = "uk.ac.warwick.tabula.data.model.LocationUserType")
  var meetingLocation: Location = _

  @Column(name = "meeting_format")
  @Type(`type` = "uk.ac.warwick.tabula.data.model.MeetingFormatUserType")
  var format: MeetingFormat = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "creator_id")
  var creator: Member = _

  def readPermissions(): Seq[Seq[Permission]] =
    relationshipTypes.map(relationshipType => Seq(Permissions.Profiles.MeetingRecord.ReadDetails(relationshipType)))

  @OneToMany(mappedBy = "meetingRecord", fetch = FetchType.LAZY, cascade = Array(ALL))
  @RestrictionProvider("readPermissions")
  @BatchSize(size = 200)
  var attachments: JList[FileAttachment] = JArrayList()

  def addAttachment(attachment: FileAttachment): Unit = {
    if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
    attachment.temporary = false
    attachment.meetingRecord = this
    attachments.add(attachment)
  }

  @RestrictionProvider("readPermissions")
  var title: String = _

  @RestrictionProvider("readPermissions")
  var description: String = _

  var missed: Boolean = false

  @Column(name = "missed_reason")
  var missedReason: String = _

  def escapedDescription: TemplateHTMLOutputModel = FormattedHtml(description)

  def this(creator: Member, relationship: StudentRelationship) = {
    this()
    this.creator = creator
    this.relationships = Seq(relationship)
  }

  def this(creator: Member, relationships: Seq[StudentRelationship]) = {
    this()
    this.creator = creator
    this.relationships = relationships
  }

  def student: StudentMember = relationships.flatMap(_.studentMember).headOption.getOrElse(throw new IllegalStateException("Meeting record student member not found"))

  def relationshipTypes: Seq[StudentRelationshipType] = relationships.map(_.relationshipType).distinct

  def agents: Seq[Member] = relationships.flatMap(_.agentMember).distinct

  def participants: Seq[Member] = student +: agents

  def allParticipantNames: String = memberNames(participants)

  def allAgentNames: String = memberNames(agents)

  def participantNamesExcept(user: User): String = memberNames(participants.filterNot(_.asSsoUser == user))

  protected def memberNames(members: Seq[Member]): String =
    members.sortBy(p => (p.lastName, p.firstName, p.universityId)).map(p => p.fullName.getOrElse(p.universityId)) match {
      case Nil => "nobody"
      case Seq(single) => single
      case init :+ last => s"${init.mkString(", ")} and $last"
    }

  def toEventOccurrence(context: TimetableEvent.Context): Option[EventOccurrence]

  protected def asEventOccurrence(context: TimetableEvent.Context): Option[EventOccurrence] = {
    Some(EventOccurrence(
      uid = id,
      name = title,
      title = title,
      description = description,
      eventType = TimetableEventType.Meeting,
      start = meetingDate.toLocalDateTime,
      end = meetingEndDate.toLocalDateTime,
      location = Option(meetingLocation).orElse {
        if (format == MeetingFormat.FaceToFace) None
        else Option(format).map(_.description).map(NamedLocation)
      },
      onlineDeliveryUrl = None,
      deliveryMethod = None,
      parent = TimetableEvent.Parent(relationships.map(_.relationshipType).distinct),
      comments = None,
      staff = context match {
        case TimetableEvent.Context.Staff => Seq(student.asSsoUser)
        case TimetableEvent.Context.Student => agents.map(_.asSsoUser)
      },
      relatedUrl = Some(RelatedUrl(
        urlString = Routes.Profile.relationshipType(
          relationships.head.studentCourseDetails,
          AcademicYear.forDate(meetingDate.toDateTime),
          relationships.head.relationshipType
        ),
        title = Some("Meeting records")
      )),
      attendance = None
    ))
  }

  def permissionsParents: LazyList[StudentCourseDetails] = relationships.map(_.studentCourseDetails).to(LazyList)

  def toStringProps = Seq(
    "creator" -> creator,
    "creationDate" -> creationDate,
    "meetingDate" -> meetingDate,
    "relationships" -> relationships
  )

  override def humanReadableId: String = s"Meeting between $allParticipantNames on $meetingDate"
}

sealed abstract class MeetingFormat(val code: String, val description: String) {
  def getCode: String = code

  def getDescription: String = description

  override def toString: String = description
}

object MeetingFormat {

  case object FaceToFace extends MeetingFormat("f2f", "Face-to-face meeting")

  case object VideoConference extends MeetingFormat("video", "Video conference")

  case object PhoneCall extends MeetingFormat("phone", "Telephone call")

  case object Email extends MeetingFormat("email", "Email conversation")

  // lame manual collection. Keep in sync with the case objects above
  val members = Set(FaceToFace, VideoConference, PhoneCall, Email)

  def fromCode(code: String): MeetingFormat =
    if (code == null) null
    else members.find {
      _.code == code
    } match {
      case Some(caseObject) => caseObject
      case None => throw new IllegalArgumentException()
    }

  @Deprecated // use only in MonitoringPoint, AttendanceMonitoringPoint to catch legacy db data
  def fromCodeOrDescription(value: String): MeetingFormat =
    if (value == null) null
    else members.find { m => m.description == value || m.code == value } match {
      case Some(caseObject) => caseObject
      case None => throw new IllegalArgumentException()
    }
}

class MeetingFormatUserType extends AbstractBasicUserType[MeetingFormat, String] {

  val basicType = StandardBasicTypes.STRING

  override def sqlTypes = Array(Types.VARCHAR)

  val nullValue = null
  val nullObject = null

  override def convertToObject(string: String): MeetingFormat = MeetingFormat.fromCode(string)

  override def convertToValue(format: MeetingFormat): String = format.code
}
