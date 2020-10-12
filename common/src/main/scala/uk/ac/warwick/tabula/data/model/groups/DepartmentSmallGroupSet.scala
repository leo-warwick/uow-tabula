package uk.ac.warwick.tabula.data.model.groups

import javax.persistence.CascadeType._
import javax.persistence._
import javax.validation.constraints.NotNull
import org.hibernate.annotations.{BatchSize, Filter, FilterDef, Proxy, Type}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{SmallGroupMembershipHelpers, SmallGroupService, UserGroupCacheManager}
import uk.ac.warwick.tabula.{AcademicYear, ToString}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

object DepartmentSmallGroupSet {
  final val NotDeletedFilter = "notDeleted"

  // For sorting a collection by set name. Either pass to the sort function,
  // or expose as an implicit val.
  val NameOrdering: Ordering[DepartmentSmallGroupSet] = Ordering.by { set: DepartmentSmallGroupSet => (set.name, set.id) }

  // Companion object is one of the places searched for an implicit Ordering, so
  // this will be the default when ordering a list of small group sets.
  implicit val defaultOrdering = NameOrdering
}

/**
  * Represents a set of small groups with a Department. These are then linked to by an actual
  * SmallGroupSet rather than doing allocations manually.
  */
@FilterDef(name = DepartmentSmallGroupSet.NotDeletedFilter, defaultCondition = "deleted = false")
@Filter(name = DepartmentSmallGroupSet.NotDeletedFilter)
@Entity
@Proxy
@Access(AccessType.FIELD)
class DepartmentSmallGroupSet
  extends GeneratedId
    with CanBeDeleted
    with ToString
    with PermissionsTarget
    with Serializable
    with ToEntityReference {
  type Entity = DepartmentSmallGroupSet

  // FIXME this isn't really optional, but testing is a pain unless it's made so
  @transient var smallGroupService: Option[SmallGroupService with SmallGroupMembershipHelpers] = Wire.option[SmallGroupService with SmallGroupMembershipHelpers]

  def this(_department: Department) = {
    this()
    this.department = _department
  }

  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(nullable = false)
  var academicYear: AcademicYear = AcademicYear.now()

  @NotNull
  var name: String = _

  var archived: JBoolean = false

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  var department: Department = _

  @OneToMany(fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @JoinColumn(name = "set_id")
  @BatchSize(size = 200)
  var groups: JList[DepartmentSmallGroup] = JArrayList()

  @OneToMany(mappedBy = "linkedDepartmentSmallGroupSet", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
  @BatchSize(size = 200)
  var linkedSets: JSet[SmallGroupSet] = JHashSet()

  // only students manually added or excluded. use allStudents to get all students in the group set
  @OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
  @JoinColumn(name = "membersgroup_id")
  private var _membersGroup = UserGroup.ofUniversityIds

  def members: UnspecifiedTypeUserGroup = {
    smallGroupService match {
      case Some(smallGroupService) => {
        new UserGroupCacheManager(_membersGroup, smallGroupService.departmentGroupSetManualMembersHelper)
      }
      case _ => _membersGroup
    }
  }

  def members_=(group: UserGroup): Unit = {
    _membersGroup = group
  }

  @Column(name = "member_query")
  var memberQuery: String = _

  def isStudentMember(user: User): Boolean = members.includesUser(user)

  def allStudents: Seq[User] = members.users.toSeq

  def allStudentIds: Seq[String] = if (members.universityIds) members.knownType.members.toSeq else allStudents.map(_.getWarwickId)

  def allStudentsCount: Int = members.size

  def unallocatedStudents: Seq[User] = {
    val allocatedStudents = groups.asScala.flatMap(_.students.users)

    allStudents diff allocatedStudents
  }

  def unallocatedStudentsCount: Int = {
    // TAB-2296 we can't rely just on counts here
    unallocatedStudents.size
  }

  def studentsNotInMembership: Seq[User] = {
    val allocatedStudents = groups.asScala.toSeq.flatMap(_.students.users)

    allocatedStudents diff allStudents
  }

  def studentsNotInMembershipCount: Int = {
    // TAB-2296 we can't rely just on counts here
    studentsNotInMembership.size
  }

  def hasAllocated: Boolean = groups.asScala.exists {
    !_.students.isEmpty
  }

  def permissionsParents: LazyList[PermissionsTarget] = Option(department).to(LazyList) #::: linkedSets.asScala.to(LazyList)

  def toStringProps = Seq(
    "id" -> id,
    "name" -> name,
    "department" -> department)

  def duplicateTo(transient: Boolean, department: Department = department, academicYear: AcademicYear = academicYear, copyMembership: Boolean = true): DepartmentSmallGroupSet = {
    val newSet = new DepartmentSmallGroupSet()
    if (!transient) newSet.id = id
    newSet.academicYear = academicYear
    newSet.archived = archived
    newSet.memberQuery = memberQuery
    newSet.groups = groups.asScala.map(_.duplicateTo(newSet, transient = transient, copyMembership = copyMembership)).asJava
    if (copyMembership) newSet._membersGroup = _membersGroup.duplicate()
    newSet.department = department
    newSet.name = name
    newSet
  }

}
