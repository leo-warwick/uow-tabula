package uk.ac.warwick.tabula.data.model.groups

import javax.persistence.CascadeType._
import javax.persistence._
import javax.validation.constraints.NotNull
import org.hibernate.annotations.{BatchSize, Filter, FilterDef, Proxy, Type}
import org.hibernate.validator.constraints.URL
import org.joda.time.{LocalDate, LocalTime}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.{Linked, StudentSignUp}
import uk.ac.warwick.tabula.helpers.RequestLevelCache
import uk.ac.warwick.tabula.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.{AcademicYear, ToString}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

object SmallGroupSet {
  final val NotDeletedFilter = "notDeleted"

  object Settings {
    val StudentsCanSeeTutorNames = "StudentsCanSeeTutorNames"
    val StudentsCanSeeOtherMembers = "StudentsCanSeeOtherMembers"
    val DefaultMaxGroupSizeEnabled = "DefaultMaxGroupSizeEnabled"
    val DefaultMaxGroupSize = "DefaultMaxGroupSize"
  }

  // For sorting a collection by set name. Either pass to the sort function,
  // or expose as an implicit val.
  val NameOrdering: Ordering[SmallGroupSet] = Ordering.by { set: SmallGroupSet => (set.name, set.id) }

  // Companion object is one of the places searched for an implicit Ordering, so
  // this will be the default when ordering a list of small group sets.
  implicit val defaultOrdering = NameOrdering
}

/**
  * Represents a set of small groups, within an instance of a module.
  */
@FilterDef(name = SmallGroupSet.NotDeletedFilter, defaultCondition = "deleted = false")
@Filter(name = SmallGroupSet.NotDeletedFilter)
@Entity
@Proxy
@Access(AccessType.FIELD)
class SmallGroupSet
  extends GeneratedId
    with CanBeDeleted
    with ToString
    with PermissionsTarget
    with HasSettings
    with Serializable
    with PostLoadBehaviour
    with ToEntityReference
    with TaskBenchmarking
    with HasManualMembership {
  type Entity = SmallGroupSet

  import SmallGroupSet.Settings

  @transient var permissionsService: PermissionsService = Wire[PermissionsService]
  @transient var membershipService: AssessmentMembershipService = Wire[AssessmentMembershipService]

  // FIXME this isn't really optional, but testing is a pain unless it's made so
  @transient var smallGroupService: Option[SmallGroupService with SmallGroupMembershipHelpers] = Wire.option[SmallGroupService with SmallGroupMembershipHelpers]

  def this(_module: Module) = {
    this()
    this.module = _module
  }

  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(nullable = false)
  var academicYear: AcademicYear = AcademicYear.now()

  @NotNull
  var name: String = _

  var archived: JBoolean = false

  @Column(name = "released_to_students")
  private var _releasedToStudents: JBoolean = false

  def releasedToStudents: Boolean = {
    allocationMethod match {
      case StudentSignUp => true
      case _ =>
        _releasedToStudents
    }
  }

  def releasedToStudents_=(releasedToStudents: Boolean): Unit = {
    _releasedToStudents = releasedToStudents
  }

  def canBeDeleted: Boolean = {
    allocationMethod match {
      case StudentSignUp => !releasedToTutors
      case _ =>
        !_releasedToStudents && !releasedToTutors
    }
  }

  @Column(name = "released_to_tutors")
  var releasedToTutors: JBoolean = false

  @Column(name = "email_students")
  var emailStudentsOnChange: JBoolean = true
  @Column(name = "email_tutors")
  var emailTutorsOnChange: JBoolean = true

  def visibleToStudents: Boolean = releasedToStudents || allocationMethod == SmallGroupAllocationMethod.StudentSignUp

  def fullyReleased: Boolean = releasedToStudents && releasedToTutors

  @Column(name = "group_format")
  @Type(`type` = "uk.ac.warwick.tabula.data.model.groups.SmallGroupFormatUserType")
  @NotNull
  var format: SmallGroupFormat = _

  @Column(name = "allocation_method")
  @Type(`type` = "uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethodUserType")
  var allocationMethod: SmallGroupAllocationMethod = SmallGroupAllocationMethod.Manual

  @Column(name = "membership_style")
  @Type(`type` = "uk.ac.warwick.tabula.data.model.groups.SmallGroupMembershipStyleUserType")
  var membershipStyle: SmallGroupMembershipStyle = SmallGroupMembershipStyle.Default

  @Column(name = "member_query")
  var memberQuery: String = _

  @Column(name = "self_group_switching")
  var allowSelfGroupSwitching: Boolean = true

  // TODO consider changing this to be a string, and setting it to the name of the SmallGroupSetSelfSignUpState
  // to allow for more states than just "open" and "closed"
  @Column(name = "open_for_signups")
  var openForSignups: Boolean = false

  def openState: SmallGroupSetSelfSignUpState = if (openForSignups) SmallGroupSetSelfSignUpState.Open else SmallGroupSetSelfSignUpState.Closed

  def openState_=(value: SmallGroupSetSelfSignUpState): Unit = value match {
    case SmallGroupSetSelfSignUpState.Open => openForSignups = true
    case SmallGroupSetSelfSignUpState.Closed => openForSignups = false
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "module_id")
  var module: Module = _

  @OneToMany(fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @JoinColumn(name = "set_id")
  @BatchSize(size = 200)
  @OrderBy("name")
  var groups: JList[SmallGroup] = JArrayList()

  // A linked departmental small group set; if this is linked, memberships aren't kept here.
  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "linked_dept_group_set_id")
  var linkedDepartmentSmallGroupSet: DepartmentSmallGroupSet = _

  // only students manually added or excluded. use allStudents to get all students in the group set
  @OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
  @JoinColumn(name = "membersgroup_id")
  private var _membersGroup: UserGroup = UserGroup.ofUniversityIds

  def members: UnspecifiedTypeUserGroup = {
    linkedDepartmentSmallGroupSet match {
      case ldsgs: DepartmentSmallGroupSet => ldsgs.members
      case _ =>
        smallGroupService match {
          case Some(service) =>
            new UserGroupCacheManager(_membersGroup, service.groupSetManualMembersHelper)
          case _ => _membersGroup
        }
    }
  }

  def members_=(group: UserGroup): Unit = {
    _membersGroup = group
  }

  // Cannot link directly to upstream assessment groups data model in sits is silly ...
  @OneToMany(mappedBy = "smallGroupSet", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @BatchSize(size = 200)
  var assessmentGroups: JList[AssessmentGroup] = JArrayList()

  @Column(name = "collect_attendance")
  var collectAttendance: Boolean = true

  // --- Default properties for creating/applying to SGEs ---
  @Type(`type` = "uk.ac.warwick.tabula.data.model.groups.WeekRangeListUserType")
  @Column(name = "default_weekranges")
  var defaultWeekRanges: Seq[WeekRange] = Nil

  @Type(`type` = "uk.ac.warwick.tabula.data.model.groups.DayOfWeekUserType")
  @Column(name = "default_day")
  var defaultDay: DayOfWeek = _

  @Column(name = "default_starttime")
  var defaultStartTime: LocalTime = _
  @Column(name = "default_endtime")
  var defaultEndTime: LocalTime = _

  @OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
  @JoinColumn(name = "default_tutorsgroup_id")
  private var _defaultTutors: UserGroup = UserGroup.ofUsercodes

  def defaultTutors: UnspecifiedTypeUserGroup = _defaultTutors // No need to wrap this in a cache
  def defaultTutors_=(group: UserGroup): Unit = {
    _defaultTutors = group
  }

  @Type(`type` = "uk.ac.warwick.tabula.data.model.LocationUserType")
  @Column(name = "default_location")
  var defaultLocation: Location = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.groups.EventDeliveryMethodUserType")
  var defaultDeliveryMethod: EventDeliveryMethod = _

  @URL
  var defaultOnlineDeliveryUrl: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.groups.OnlinePlatformUserType")
  var defaultOnlinePlatform: OnlinePlatform = _

  // --- End of default properties for creating/applying to SGEs ---

  def showAttendanceReports: Boolean = !archived && !deleted && collectAttendance

  // converts the assessmentGroups to UpstreamAssessmentGroupInfo
  def upstreamAssessmentGroupInfos: Seq[UpstreamAssessmentGroupInfo] = RequestLevelCache.cachedBy("SmallGroupSet.upstreamAssessmentGroupInfos", id) {
    membershipService.getUpstreamAssessmentGroupInfo(assessmentGroups.asScala.toSeq, academicYear)
  }

  // Gets a breakdown of the membership for this small group set.
  def membershipInfo: AssessmentMembershipInfo = RequestLevelCache.cachedBy("SmallGroupSet.membershipInfo", id) {
    membershipService.determineMembership(upstreamAssessmentGroupInfos, Some(members), resitOnly = false)
  }

  def isStudentMember(user: User): Boolean = RequestLevelCache.cachedBy("SmallGroupSet.isStudentMember", s"$id-${user.getUserId}") {
    val memberOfAnyGroup: Boolean =
      smallGroupService match {
        case Some(service) =>
          service.studentGroupHelper.findBy(user).exists(_.groupSet.id == id)

        case _ => groups.asScala.exists(_.students.includesUser(user))
      }

    memberOfAnyGroup || Option(linkedDepartmentSmallGroupSet).map(_.isStudentMember(user)).getOrElse {
      membershipService.isStudentCurrentMember(user, upstreamAssessmentGroupInfos, Option(members), resitOnly = false)
    }
  }

  def allStudents: Seq[User] = RequestLevelCache.cachedBy("SmallGroupSet.allStudents", id) {
    Option(linkedDepartmentSmallGroupSet).map(_.allStudents).getOrElse {
      membershipService.determineMembershipUsers(upstreamAssessmentGroupInfos, Some(members), resitOnly = false)
    }
  }

  def allStudentIds: Seq[String] = RequestLevelCache.cachedBy("SmallGroupSet.allStudentIds", id) {
    Option(linkedDepartmentSmallGroupSet).map(_.allStudentIds).getOrElse {
      membershipService.determineMembershipIds(upstreamAssessmentGroupInfos, Some(members))
    }
  }

  def allStudentsCount: Int = benchmarkTask(s"$id allStudentsCount") {
    RequestLevelCache.cachedBy("SmallGroupSet.allStudentsCount", id) {
      Option(linkedDepartmentSmallGroupSet).map(_.allStudentsCount).getOrElse {
        membershipService.countCurrentMembershipWithUniversityIdGroup(upstreamAssessmentGroupInfos, Some(members))
      }
    }
  }

  def unallocatedStudents: Seq[User] = RequestLevelCache.cachedBy("SmallGroupSet.unallocatedStudents", id) {
    Option(linkedDepartmentSmallGroupSet).map(_.unallocatedStudents).getOrElse {
      val allocatedStudents = groups.asScala.flatMap(_.students.users)

      allStudents diff allocatedStudents
    }
  }

  def unallocatedStudentsCount: Int = benchmarkTask(s"$id unallocatedStudentsCount") {
    RequestLevelCache.cachedBy("SmallGroupSet.unallocatedStudentsCount", id) {
      Option(linkedDepartmentSmallGroupSet).map(_.unallocatedStudentsCount).getOrElse {
        if (groups.asScala.forall {
          _.students.universityIds
        } && members.universityIds) {
          // Efficiency
          val allocatedStudentIds = groups.asScala.flatMap(_.students.knownType.members)

          (allStudentIds diff allocatedStudentIds).size
        } else {
          // TAB-2296 we can't rely just on counts here
          unallocatedStudents.size
        }
      }
    }
  }

  def studentsNotInMembership: Seq[User] = {
    Option(linkedDepartmentSmallGroupSet).map(_.studentsNotInMembership).getOrElse {
      val allocatedStudents = groups.asScala.toSeq.flatMap(_.students.users)

      allocatedStudents diff allStudents
    }
  }

  def studentsNotInMembershipCount: Int = {
    Option(linkedDepartmentSmallGroupSet).map(_.studentsNotInMembershipCount).getOrElse {
      if (groups.asScala.forall(_.students.universityIds) && members.universityIds) {
        // Efficiency
        val allocatedStudentIds = groups.asScala.flatMap(_.students.knownType.members)

        (allocatedStudentIds diff allStudentIds).size
      } else {
        // TAB-2296 we can't rely just on counts here
        studentsNotInMembership.size
      }
    }
  }

  def nameWithoutModulePrefix: String = {
    val moduleCodePrefix = module.code.toUpperCase + " "
    if (name.startsWith(moduleCodePrefix)) {
      name.replaceFirst(moduleCodePrefix, "")
    } else name
  }

  def linked: Boolean = allocationMethod == SmallGroupAllocationMethod.Linked

  def hasAllocated: Boolean = groups.asScala exists {
    !_.students.isEmpty
  }

  def permissionsParents: LazyList[Module] = Option(module).to(LazyList)

  override def humanReadableId: String = name

  def studentsCanSeeTutorName: Boolean = getBooleanSetting(Settings.StudentsCanSeeTutorNames).getOrElse(false)

  def studentsCanSeeTutorName_=(canSee: Boolean): Unit = settings += (Settings.StudentsCanSeeTutorNames -> canSee)

  def studentsCanSeeOtherMembers: Boolean = getBooleanSetting(Settings.StudentsCanSeeOtherMembers).getOrElse(false)

  def studentsCanSeeOtherMembers_=(canSee: Boolean): Unit = settings += (Settings.StudentsCanSeeOtherMembers -> canSee)

  def toStringProps = Seq(
    "id" -> id,
    "name" -> name,
    "module" -> module)

  def duplicateTo(transient: Boolean, module: Module = module, assessmentGroups: JList[AssessmentGroup] = JArrayList(), academicYear: AcademicYear = academicYear, copyGroups: Boolean = true, copyEvents: Boolean = true, copyMembership: Boolean = true): SmallGroupSet = {
    val newSet = new SmallGroupSet()
    if (!transient) newSet.id = id
    newSet.academicYear = academicYear
    newSet.allocationMethod = allocationMethod
    newSet.membershipStyle = membershipStyle
    newSet.allowSelfGroupSwitching = allowSelfGroupSwitching
    newSet.archived = archived
    newSet.assessmentGroups = assessmentGroups
    newSet.collectAttendance = collectAttendance
    newSet.format = format
    if (copyGroups) newSet.groups = groups.asScala.map(_.duplicateTo(newSet, transient = transient, copyEvents = copyEvents, copyMembership = copyMembership)).asJava
    if (copyMembership) newSet._membersGroup = _membersGroup.duplicate()
    newSet.membershipService = membershipService
    newSet.module = module
    newSet.name = name
    newSet.permissionsService = permissionsService
    newSet.releasedToStudents = releasedToStudents
    newSet.releasedToTutors = releasedToTutors
    newSet.openForSignups = openForSignups
    newSet.defaultWeekRanges = defaultWeekRanges
    newSet.defaultDay = defaultDay
    newSet.defaultStartTime = defaultStartTime
    newSet.defaultEndTime = defaultEndTime
    if (_defaultTutors != null) newSet._defaultTutors = _defaultTutors.duplicate()
    newSet.defaultLocation = defaultLocation
    newSet.linkedDepartmentSmallGroupSet = linkedDepartmentSmallGroupSet
    newSet.settings = Map() ++ settings
    newSet
  }

  def postLoad(): Unit = {
    ensureSettings
  }

  def department: Department = module.adminDepartment

  //Generate members for SGT at start if  1)linked by SITS query 2)sits not in flux 3)Not using linked allocation
  def generateLinkedSitsQueryMembers: Boolean = membershipStyle == SmallGroupMembershipStyle.Default && memberQuery.isEmptyOrWhitespace && !academicYear.isSITSInFlux(LocalDate.now) && allocationMethod != Linked
}

