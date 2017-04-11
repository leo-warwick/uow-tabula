package uk.ac.warwick.tabula.data.model.groups

import javax.persistence.CascadeType._
import javax.persistence._
import javax.validation.constraints.NotNull

import org.hibernate.annotations.{BatchSize, Filter, FilterDef, Type}
import org.joda.time.{DateTime, LocalTime}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.{AcademicYear, ToString}
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.mutable

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
@FilterDef(name = SmallGroupSet.NotDeletedFilter, defaultCondition = "deleted = 0")
@Filter(name = SmallGroupSet.NotDeletedFilter)
@Entity
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
		with TaskBenchmarking {
	type Entity = SmallGroupSet

	import SmallGroupSet.Settings

	@transient var permissionsService: PermissionsService = Wire[PermissionsService]
	@transient var membershipService: AssessmentMembershipService = Wire[AssessmentMembershipService]

	// FIXME this isn't really optional, but testing is a pain unless it's made so
	@transient var smallGroupService: Option[SmallGroupService with SmallGroupMembershipHelpers] = Wire.option[SmallGroupService with SmallGroupMembershipHelpers]

	def this(_module: Module) {
		this()
		this.module = _module
	}

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Column(nullable = false)
	var academicYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

	@NotNull
	var name: String = _

	var archived: JBoolean = false

  @Column(name="released_to_students")
	var releasedToStudents: JBoolean = false
  @Column(name="released_to_tutors")
  var releasedToTutors: JBoolean = false

	@Column(name="email_students")
	var emailStudentsOnChange: JBoolean = true
	@Column(name="email_tutors")
	var emailTutorsOnChange: JBoolean = true

  def visibleToStudents: Boolean = releasedToStudents || allocationMethod == SmallGroupAllocationMethod.StudentSignUp

  def fullyReleased: Boolean = releasedToStudents && releasedToTutors

	@Column(name="group_format")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.groups.SmallGroupFormatUserType")
	@NotNull
	var format: SmallGroupFormat = _

	@Column(name="allocation_method")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethodUserType")
	var allocationMethod: SmallGroupAllocationMethod = SmallGroupAllocationMethod.Manual

	@Column(name="self_group_switching")
	var allowSelfGroupSwitching: Boolean = true

	// TODO consider changing this to be a string, and setting it to the name of the SmallGroupSetSelfSignUpState
	// to allow for more states than just "open" and "closed"
	@Column(name="open_for_signups")
	var openForSignups: Boolean = false

  def openState: SmallGroupSetSelfSignUpState = if (openForSignups) SmallGroupSetSelfSignUpState.Open else SmallGroupSetSelfSignUpState.Closed

 	def openState_=(value:SmallGroupSetSelfSignUpState) : Unit = value match {
	 	case SmallGroupSetSelfSignUpState.Open => openForSignups = true
	 	case SmallGroupSetSelfSignUpState.Closed => openForSignups = false
  }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "module_id")
	var module: Module = _

	@OneToMany(fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval=true)
	@JoinColumn(name = "set_id")
	@BatchSize(size=200)
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
	def members_=(group: UserGroup) { _membersGroup = group }

	// Cannot link directly to upstream assessment groups data model in sits is silly ...
	@OneToMany(mappedBy = "smallGroupSet", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=200)
	var assessmentGroups: JList[AssessmentGroup] = JArrayList()

	@Column(name="collect_attendance")
	var collectAttendance: Boolean = true

	// Default properties for creating/applying to SGEs
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
	def defaultTutors_=(group: UserGroup) { _defaultTutors = group }

	@Type(`type` = "uk.ac.warwick.tabula.data.model.LocationUserType")
	@Column(name = "default_location")
	var defaultLocation: Location = _

	def showAttendanceReports: Boolean = !archived && !deleted && collectAttendance

	// converts the assessmentGroups to upstream assessment groups
	def upstreamAssessmentGroups: Seq[UpstreamAssessmentGroup] = assessmentGroups.asScala.flatMap { _.toUpstreamAssessmentGroup(academicYear) }

	// Gets a breakdown of the membership for this small group set.
	def membershipInfo: AssessmentMembershipInfo = membershipService.determineMembership(upstreamAssessmentGroups, Some(members))

	def isStudentMember(user: User): Boolean = {
		groups.asScala.exists(_.students.includesUser(user)) ||
		Option(linkedDepartmentSmallGroupSet).map { _.isStudentMember(user) }.getOrElse {
			membershipService.isStudentMember(user, upstreamAssessmentGroups, Option(members))
		}
	}

	def allStudents: Seq[User] =
		Option(linkedDepartmentSmallGroupSet).map { _.allStudents }.getOrElse {
			membershipService.determineMembershipUsers(upstreamAssessmentGroups, Some(members))
		}

	def allStudentIds: Seq[String] =
		Option(linkedDepartmentSmallGroupSet).map { _.allStudentIds }.getOrElse {
			membershipService.determineMembershipIds(upstreamAssessmentGroups, Some(members))
		}

	def allStudentsCount: Int = benchmarkTask(s"${this.id} allStudentsCount") {
		Option(linkedDepartmentSmallGroupSet).map { _.allStudentsCount }.getOrElse {
			membershipService.countMembershipWithUniversityIdGroup(upstreamAssessmentGroups, Some(members))
		}}

	def unallocatedStudents: Seq[User] = {
		Option(linkedDepartmentSmallGroupSet).map { _.unallocatedStudents }.getOrElse {
			val allocatedStudents = groups.asScala.flatMap { _.students.users }

			allStudents diff allocatedStudents
		}
	}

	def unallocatedStudentsCount: Int = benchmarkTask(s"${this.id} unallocatedStudentsCount") {
		Option(linkedDepartmentSmallGroupSet).map { _.unallocatedStudentsCount }.getOrElse {
			if (groups.asScala.forall { _.students.universityIds } && members.universityIds) {
				// Efficiency
				val allocatedStudentIds = groups.asScala.flatMap { _.students.knownType.members }

				(allStudentIds diff allocatedStudentIds).size
			} else {
				// TAB-2296 we can't rely just on counts here
				unallocatedStudents.size
			}
		}
	}

	def studentsNotInMembership: mutable.Buffer[User] = {
		Option(linkedDepartmentSmallGroupSet).map { _.studentsNotInMembership }.getOrElse {
			val allocatedStudents = groups.asScala.flatMap { _.students.users }

			allocatedStudents diff allStudents
		}
	}

	def studentsNotInMembershipCount: Int = {
		Option(linkedDepartmentSmallGroupSet).map { _.studentsNotInMembershipCount }.getOrElse {
			if (groups.asScala.forall { _.students.universityIds } && members.universityIds) {
				// Efficiency
				val allocatedStudentIds = groups.asScala.flatMap { _.students.knownType.members }

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

	def hasAllocated: Boolean = groups.asScala exists { !_.students.isEmpty }

	def permissionsParents: Stream[Module] = Option(module).toStream
	override def humanReadableId: String = name

	def studentsCanSeeTutorName: Boolean = getBooleanSetting(Settings.StudentsCanSeeTutorNames).getOrElse(false)
	def studentsCanSeeTutorName_=(canSee:Boolean): Unit = settings += (Settings.StudentsCanSeeTutorNames -> canSee)

	def studentsCanSeeOtherMembers: Boolean = getBooleanSetting(Settings.StudentsCanSeeOtherMembers).getOrElse(false)
	def studentsCanSeeOtherMembers_=(canSee:Boolean): Unit = settings += (Settings.StudentsCanSeeOtherMembers -> canSee)

	def toStringProps = Seq(
		"id" -> id,
		"name" -> name,
		"module" -> module)

  def duplicateTo(transient: Boolean, module: Module = module, assessmentGroups: JList[AssessmentGroup] = JArrayList(), academicYear: AcademicYear = academicYear, copyGroups: Boolean = true, copyEvents: Boolean = true, copyMembership: Boolean = true): SmallGroupSet = {
    val newSet = new SmallGroupSet()
		if (!transient) newSet.id = id
    newSet.academicYear = academicYear
    newSet.allocationMethod = allocationMethod
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

	def postLoad {
		ensureSettings
	}

	override def toEntityReference: SmallGroupSetEntityReference = new SmallGroupSetEntityReference().put(this)
}

