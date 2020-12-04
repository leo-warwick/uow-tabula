package uk.ac.warwick.tabula.data.model.groups

import enumeratum.{Enum, EnumEntry}
import javax.persistence.CascadeType._
import javax.persistence._
import javax.validation.constraints.NotNull
import org.hibernate.annotations.{Proxy, Type}
import org.hibernate.validator.constraints.URL
import org.joda.time.{LocalDate, LocalDateTime, LocalTime}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.system.EnumTwoWayConverter
import uk.ac.warwick.tabula.{AcademicYear, ToString}

import scala.collection.immutable

object SmallGroupEvent {

  val RelatedUrlTitleMaxLength = 255

  // Companion object is one of the places searched for an implicit Ordering, so
  // this will be the default when ordering a list of small group events.
  implicit val defaultOrdering: Ordering[SmallGroupEvent] = new Ordering[SmallGroupEvent] {
    final val FirstInstanceOrdering: Ordering[SmallGroupEvent] = Ordering.by { event: SmallGroupEvent =>
      (Option(event.weekRanges).filter(_.nonEmpty).map {
        _.minBy {
          _.minWeek
        }.minWeek
      }, Option(event.day).map(_.jodaDayOfWeek), Option(event.startTime).map(_.getMillisOfDay), Option(event.endTime).map(_.getMillisOfDay))
    }

    def compare(a: SmallGroupEvent, b: SmallGroupEvent): Int = {
      val firstInstanceCompare = FirstInstanceOrdering.compare(a, b)
      if (firstInstanceCompare != 0) firstInstanceCompare
      else {
        val titleCompare = StringUtils.AlphaNumericStringOrdering.compare(a.title, b.title)
        if (titleCompare != 0) titleCompare else Ordering.by { event: SmallGroupEvent => Option(event.id) }.compare(a, b)
      }
    }
  }

}

@Entity
@Proxy
@Access(AccessType.FIELD)
class SmallGroupEvent extends GeneratedId with ToString with PermissionsTarget with Serializable {

  @transient var permissionsService: PermissionsService = Wire[PermissionsService]

  // FIXME this isn't really optional, but testing is a pain unless it's made so
  @transient var smallGroupService: Option[SmallGroupService with SmallGroupMembershipHelpers] = Wire.option[SmallGroupService with SmallGroupMembershipHelpers]

  def this(_group: SmallGroup) = {
    this()
    this.group = _group
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", insertable = false, updatable = false)
  var group: SmallGroup = _

  // Store Week Ranges as ACADEMIC week numbers - so week 1 is the first week of Autumn term, week 1 of spring term is 15 or 16, etc.
  @Type(`type` = "uk.ac.warwick.tabula.data.model.groups.WeekRangeListUserType")
  var weekRanges: Seq[WeekRange] = Nil

  @Type(`type` = "uk.ac.warwick.tabula.data.model.groups.DayOfWeekUserType")
  var day: DayOfWeek = _

  var startTime: LocalTime = _
  var endTime: LocalTime = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.LocationUserType")
  var location: Location = _

  var title: String = _

  override def humanReadableId: String = Option(title).getOrElse(
    Option(group.name).map(name => s"$name event").getOrElse(s"${group.groupSet.name} event")
  )

  @URL
  var relatedUrl: String = _
  var relatedUrlTitle: String = _

  @NotNull // defaults to hybrid
  @Type(`type` = "uk.ac.warwick.tabula.data.model.groups.EventDeliveryMethodUserType")
  var deliveryMethod: EventDeliveryMethod = _

  @URL
  var onlineDeliveryUrl: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.groups.OnlinePlatformUserType")
  @Column(name = "onlinePlatform")
  private var _onlinePlatform: OnlinePlatform = _
  def onlinePlatform: Option[OnlinePlatform] = Option(_onlinePlatform)
  def onlinePlatform_=(platform: Option[OnlinePlatform]): Unit = _onlinePlatform = platform.orNull

  def isUnscheduled: Boolean = day == null || (startTime == null && endTime == null)

  def scheduled: Boolean = day != null

  def isSingleEvent: Boolean = weekRanges.size == 1 && weekRanges.head.isSingleWeek

  @OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
  @JoinColumn(name = "tutorsgroup_id")
  private var _tutors: UserGroup = UserGroup.ofUsercodes

  def tutors: UnspecifiedTypeUserGroup = {
    smallGroupService match {
      case Some(sgs) =>
        new UserGroupCacheManager(_tutors, sgs.eventTutorsHelper)
      case _ => _tutors
    }
  }

  def tutors_=(group: UserGroup): Unit = {
    _tutors = group
  }

  def permissionsParents: LazyList[SmallGroup] = Option(group).to(LazyList)

  def toStringProps = Seq(
    "id" -> id,
    "weekRanges" -> weekRanges,
    "day" -> day,
    "startTime" -> startTime,
    "endTime" -> endTime)

  def isEquivalentTo(other: SmallGroupEvent): Boolean = {
    weekRanges == other.weekRanges &&
      day == other.day &&
      startTime == other.startTime &&
      endTime == other.endTime &&
      location == other.location &&
      tutors.hasSameMembersAs(other.tutors)
  }

  def duplicateTo(group: SmallGroup, transient: Boolean): SmallGroupEvent = {
    val newEvent = new SmallGroupEvent
    if (!transient) newEvent.id = id
    newEvent.day = day
    newEvent.endTime = endTime
    newEvent.group = group
    newEvent.location = location
    newEvent.permissionsService = permissionsService
    newEvent.startTime = startTime
    newEvent.title = title
    newEvent._tutors = _tutors.duplicate()
    newEvent.weekRanges = weekRanges
    newEvent.deliveryMethod = deliveryMethod
    newEvent.onlineDeliveryUrl = onlineDeliveryUrl
    newEvent.onlinePlatform = onlinePlatform
    newEvent
  }

  def allWeeks: Seq[WeekRange.Week] = weekRanges.flatMap(_.toWeeks)

  def dateForWeek(week: SmallGroupEventOccurrence.WeekNumber): Option[LocalDate] = {
    Option(day).flatMap(d => academicYear.weeks.get(week).map(_.firstDay.withDayOfWeek(d.jodaDayOfWeek)))
  }

  def startDateTimeForWeek(week: SmallGroupEventOccurrence.WeekNumber): Option[LocalDateTime] = dateForWeek(week).map(_.toLocalDateTime(startTime))

  def endDateTimeForWeek(week: SmallGroupEventOccurrence.WeekNumber): Option[LocalDateTime] = dateForWeek(week).map(_.toLocalDateTime(endTime))

  def academicYear: AcademicYear = group.academicYear

  def module: Module = group.module

  def groupSet: SmallGroupSet = group.groupSet

  def department: Department = group.department
}

sealed abstract class EventDeliveryMethod(var description: String) extends EnumEntry
object EventDeliveryMethod extends Enum[EventDeliveryMethod] {

  case object OnlineOnly extends EventDeliveryMethod("Online only")
  case object FaceToFaceOnly extends EventDeliveryMethod("Face to face only")
  case object Hybrid extends EventDeliveryMethod("Hybrid (online and face to face)")

  override val values: immutable.IndexedSeq[EventDeliveryMethod] = findValues
}
class EventDeliveryMethodUserType extends EnumUserType(EventDeliveryMethod)
class EventDeliveryMethodConverter extends EnumTwoWayConverter(EventDeliveryMethod)

sealed abstract class OnlinePlatform extends EnumEntry {
  def getEntryName: String = entryName
}
object OnlinePlatform extends Enum[OnlinePlatform] {

  case object Teams extends OnlinePlatform
  case object Moodle extends OnlinePlatform
  case object Blackboard extends OnlinePlatform
  case object SkillsForge extends OnlinePlatform
  case object Webinar extends OnlinePlatform
  case object QuestionMark extends OnlinePlatform

  override val values: immutable.IndexedSeq[OnlinePlatform] = findValues
}
class OnlinePlatformUserType extends EnumUserType(OnlinePlatform)
class OnlinePlatformConverter extends EnumTwoWayConverter(OnlinePlatform)
