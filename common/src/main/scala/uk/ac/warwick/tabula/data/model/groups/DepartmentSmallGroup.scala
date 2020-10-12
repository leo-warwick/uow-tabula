package uk.ac.warwick.tabula.data.model.groups

import javax.persistence.CascadeType._
import javax.persistence._
import javax.validation.constraints.NotNull
import org.hibernate.annotations.{BatchSize, Proxy}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{SmallGroupMembershipHelpers, SmallGroupService, UserGroupCacheManager}

import scala.jdk.CollectionConverters._

object DepartmentSmallGroup {

  // For sorting a collection by group name. Either pass to the sort function,
  // or expose as an implicit val.
  val NameOrdering: Ordering[DepartmentSmallGroup] = Ordering.by { group: DepartmentSmallGroup => (group.name, group.id) }

  // Companion object is one of the places searched for an implicit Ordering, so
  // this will be the default when ordering a list of small groups.
  implicit val defaultOrdering = NameOrdering
}

@Entity
@Proxy
@Access(AccessType.FIELD)
class DepartmentSmallGroup
  extends GeneratedId
    with ToString
    with PermissionsTarget
    with Serializable
    with ToEntityReference {
  type Entity = DepartmentSmallGroup

  // FIXME this isn't really optional, but testing is a pain unless it's made so
  @transient var smallGroupService: Option[SmallGroupService with SmallGroupMembershipHelpers] = Wire.option[SmallGroupService with SmallGroupMembershipHelpers]

  def this(_set: DepartmentSmallGroupSet) = {
    this()
    this.groupSet = _set
  }

  @NotNull
  var name: String = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "set_id", insertable = false, updatable = false)
  var groupSet: DepartmentSmallGroupSet = _

  @OneToMany(mappedBy = "linkedDepartmentSmallGroup", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
  @BatchSize(size = 200)
  var linkedGroups: JSet[SmallGroup] = JHashSet()

  def permissionsParents: LazyList[PermissionsTarget] = Option(groupSet).to(LazyList) #::: linkedGroups.asScala.to(LazyList)

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
    smallGroupService match {
      case Some(smallGroupService) => {
        new UserGroupCacheManager(_studentsGroup, smallGroupService.departmentStudentGroupHelper)
      }
      case _ => _studentsGroup
    }
  }

  def students_=(group: UserGroup): Unit = {
    _studentsGroup = group
  }

  def toStringProps = Seq(
    "id" -> id,
    "name" -> name,
    "set" -> groupSet)

  def duplicateTo(groupSet: DepartmentSmallGroupSet, transient: Boolean, copyMembership: Boolean = true): DepartmentSmallGroup = {
    val newGroup = new DepartmentSmallGroup()
    if (!transient) newGroup.id = id
    newGroup.groupSet = groupSet
    newGroup.name = name
    if (copyMembership) newGroup._studentsGroup = _studentsGroup.duplicate()
    newGroup
  }

}
