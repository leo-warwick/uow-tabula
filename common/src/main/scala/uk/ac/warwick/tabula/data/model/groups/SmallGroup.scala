package uk.ac.warwick.tabula.data.model.groups

import javax.persistence.CascadeType._
import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.helpers.StringUtils
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.services.{SmallGroupMembershipHelpers, SmallGroupService, UserGroupCacheManager}
import uk.ac.warwick.tabula.{AcademicYear, ToString}

import scala.jdk.CollectionConverters._

object SmallGroup {
  final val DefaultGroupSize = 15

  object Settings {
    val MaxGroupSize = "MaxGroupSize"
  }

  // For sorting a collection by group name. Either pass to the sort function,
  // or expose as an implicit val.
  val NameOrdering = new Ordering[SmallGroup] {
    def compare(a: SmallGroup, b: SmallGroup): Int = {
      val nameCompare = StringUtils.AlphaNumericStringOrdering.compare(a.name, b.name)
      if (nameCompare != 0) nameCompare else a.id compare b.id
    }
  }

  // Companion object is one of the places searched for an implicit Ordering, so
  // this will be the default when ordering a list of small groups.
  implicit val defaultOrdering = NameOrdering
}

/**
  * Represents a single small teaching group within a group set.
  */
@Entity
@Proxy
@Access(AccessType.FIELD)
class SmallGroup
  extends GeneratedId
    with ToString
    with PermissionsTarget
    with HasSettings
    with Serializable
    with PostLoadBehaviour
    with ToEntityReference {
  type Entity = SmallGroup

  import uk.ac.warwick.tabula.data.model.groups.SmallGroup._

  @transient var permissionsService: PermissionsService = Wire[PermissionsService]

  // FIXME this isn't really optional, but testing is a pain unless it's made so
  @transient var smallGroupService: Option[SmallGroupService with SmallGroupMembershipHelpers] = Wire.option[SmallGroupService with SmallGroupMembershipHelpers]

  def this(_set: SmallGroupSet) = {
    this()
    this.groupSet = _set
  }

  @Column(name = "name")
  private var _name: String = _

  def name: String = Option(linkedDepartmentSmallGroup).map(_.name).getOrElse(_name)

  def name_=(name: String): Unit = {
    _name = name
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "set_id", insertable = false, updatable = false)
  var groupSet: SmallGroupSet = _

  @OneToMany(fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @JoinColumn(name = "group_id")
  @BatchSize(size = 200)
  private val _events: JList[SmallGroupEvent] = JArrayList()

  def events: Seq[SmallGroupEvent] = _events.asScala.toSeq.sorted

  private def events_=(e: Seq[SmallGroupEvent]): Unit = {
    _events.clear()
    _events.addAll(e.asJava)
  }

  def addEvent(event: SmallGroupEvent): Boolean = _events.add(event)

  def removeEvent(event: SmallGroupEvent): Boolean = _events.remove(event)

  // A linked departmental small group; if this is linked, allocations aren't kept here.
  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "linked_dept_group_id")
  var linkedDepartmentSmallGroup: DepartmentSmallGroup = _

  def permissionsParents: LazyList[SmallGroupSet] = Option(groupSet).to(LazyList)

  override def humanReadableId: String = name

  /**
    * Direct access to the underlying UserGroup. Most of the time you don't want to us this; unless you're setting
    * it to a new UserGroup, you should probably access "students" instead and work with Users rather than guessing what
    * the right sort of UserId to use is.
    */
  @OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
  @JoinColumn(name = "studentsgroup_id")
  private var _studentsGroup: UserGroup = UserGroup.ofUniversityIds

  def students: UnspecifiedTypeUserGroup = {
    linkedDepartmentSmallGroup match {
      case ldsg: DepartmentSmallGroup => ldsg.students
      case _ =>
        smallGroupService match {
          case Some(service) =>
            new UserGroupCacheManager(_studentsGroup, service.studentGroupHelper)
          case _ => _studentsGroup
        }
    }
  }

  def students_=(group: UserGroup): Unit = {
    _studentsGroup = group
  }

  def maxGroupSize = JInteger(getIntSetting(Settings.MaxGroupSize))

  def maxGroupSize_=(defaultSize: JInteger): Unit =
    defaultSize match {
      case null => removeMaxGroupSize()
      case _ => settings += (Settings.MaxGroupSize -> defaultSize)
    }

  def removeMaxGroupSize(): Unit = settings -= Settings.MaxGroupSize

  def isFull: Boolean = Option(maxGroupSize).exists(_ <= students.size)

  def toStringProps = Seq(
    "id" -> id,
    "name" -> name,
    "set" -> groupSet)


  def hasEquivalentEventsTo(other: SmallGroup): Boolean = {
    (this eq other) || {
      val eventsSC = events
      val otherEvents = other.events
      val allMyEventsExistOnOther = eventsSC.forall(ev => otherEvents.exists(oe => oe.isEquivalentTo(ev)))
      val allOthersEventsExistOnMe = otherEvents.forall(oe => eventsSC.exists(ev => ev.isEquivalentTo(oe)))
      allMyEventsExistOnOther && allOthersEventsExistOnMe
    }
  }

  def duplicateTo(groupSet: SmallGroupSet, transient: Boolean, copyEvents: Boolean = true, copyMembership: Boolean = true): SmallGroup = {
    val newGroup = new SmallGroup()
    if (!transient) newGroup.id = id
    if (copyEvents) newGroup.events = events.map(_.duplicateTo(newGroup, transient = transient))
    newGroup.groupSet = groupSet
    newGroup.name = name
    newGroup.linkedDepartmentSmallGroup = linkedDepartmentSmallGroup
    newGroup.permissionsService = permissionsService
    if (copyMembership && _studentsGroup != null) newGroup._studentsGroup = _studentsGroup.duplicate()
    newGroup.settings = Map() ++ (if (settings != null) settings else Map())
    newGroup
  }

  def postLoad(): Unit = {
    ensureSettings
  }

  def preDelete(): Unit = {
    events.foreach { event =>
      smallGroupService match {
        case Some(service) => service.getAllSmallGroupEventOccurrencesForEvent(event).filterNot(
          eventOccurrence => eventOccurrence.attendance.asScala.exists {
            attendance => attendance.state != AttendanceState.NotRecorded
          }).foreach(service.delete)
        case _ =>
      }
    }
  }

  def hasScheduledEvents: Boolean = events.exists(!_.isUnscheduled)

  def academicYear: AcademicYear = groupSet.academicYear

  def module: Module = groupSet.module

  def department: Department = groupSet.department
}
