package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}
import javax.persistence._
import org.hibernate.annotations.Proxy
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.User

import scala.collection.immutable
import scala.jdk.CollectionConverters._

/**
  * Wherever a group of users is referenced in the app, it will be
  * stored as a UserGroup.
  *
  * A UserGroup can either be a totally internal list of users, or it
  * can use a webgroup as a base and then specify users to add and
  * users to exclude from that base.
  *
  * When a webgroup is used, it is a live view on the webgroup (other
  * than the included and excluded users and caching), so it will
  * change when the webgroup does (caches permitting).
  *
  * Depending on what the UserGroup is attached to, the UI might choose
  * not to allow a webgroup to be used, and only allow included users.
  * We might want to subclass UserGroup to make it a bit more explicit which
  * groups support Webgroups, and prevent invalid operations.
  *
  * Depending on context, the usercodes may be university IDs.
  */
@Entity
@Proxy
@Access(AccessType.FIELD)
class UserGroup private(val universityIds: Boolean)
  extends UnspecifiedTypeUserGroup
    with GeneratedId
    with KnownTypeUserGroup
    with Daoisms {

  /* For Hibernate xx */
  def this() = {
    this(false)
  }

  @transient var userLookup: UserLookupService = Wire[UserLookupService]

  private def groupService = userLookup.getGroupService

  var baseWebgroup: String = _

  def baseWebgroupSize: Int = groupService.getGroupInfo(baseWebgroup).getSize

  @ElementCollection
  @Column(name = "usercode")
  @JoinTable(name = "UserGroupInclude", joinColumns = Array(
    new JoinColumn(name = "group_id", referencedColumnName = "id")))
  private val includeUsers: JList[String] = JArrayList()

  @ElementCollection
  @Column(name = "usercode")
  @JoinTable(name = "UserGroupStatic", joinColumns = Array(
    new JoinColumn(name = "group_id", referencedColumnName = "id")))
  private val staticIncludeUsers: JList[String] = JArrayList()

  @ElementCollection
  @Column(name = "usercode")
  @JoinTable(name = "UserGroupExclude", joinColumns = Array(
    new JoinColumn(name = "group_id", referencedColumnName = "id")))
  private val excludeUsers: JList[String] = JArrayList()

  def includedUserIds: Set[String] = includeUsers.asScala.toSet

  def includedUserIds_=(userIds: Set[String]): Unit = {
    includeUsers.clear()
    includeUsers.addAll(userIds.asJava)
  }

  def staticUserIds: Set[String] = staticIncludeUsers.asScala.toSet

  def staticUserIds_=(userIds: Set[String]): Unit = {
    staticIncludeUsers.clear()
    staticIncludeUsers.addAll(userIds.asJava)
  }

  def excludedUserIds: Set[String] = excludeUsers.asScala.toSet

  def excludedUserIds_=(userIds: Set[String]): Unit = {
    excludeUsers.clear()
    excludeUsers.addAll(userIds.asJava)
  }

  def add(user: User): Boolean = {
    if (isAllowedMember(user)) {
      addUserId(getIdFromUser(user))
    } else false
  }

  def addUserId(user: String): Boolean = {
    if (!includeUsers.contains(user) && user.hasText) {
      includeUsers.add(user)
    } else false
  }

  def removeUserId(user: String): Boolean = includeUsers.remove(user)

  def remove(user: User): Boolean = {
    removeUserId(getIdFromUser(user))
  }

  def excludeUserId(user: String): Boolean = {
    if (!excludeUsers.contains(user) && user.hasText) {
      excludeUsers.add(user)
    } else false
  }

  def exclude(user: User): Boolean = {
    if (isAllowedMember(user)) {
      excludeUserId(getIdFromUser(user))
    } else false
  }

  def unexcludeUserId(user: String): Boolean = excludeUsers.remove(user)

  def unexclude(user: User): Boolean = {
    unexcludeUserId(getIdFromUser(user))
  }

  /*
   * Could implement as `members.contains(user)`
   * but this is more efficient
   */
  def includesUserId(user: String): Boolean =
    !(excludeUsers contains user) &&
      (
        includeUsers.contains(user) ||
          staticIncludeUsers.contains(user) ||
          (baseWebgroup != null && groupService.isUserInGroup(user, baseWebgroup)))

  def excludesUserId(user: String): Boolean = excludeUsers contains user

  def includesUser(user: User): Boolean = includesUserId(getIdFromUser(user))

  def excludesUser(user: User): Boolean = excludesUserId(getIdFromUser(user))

  def isEmpty: Boolean = members.isEmpty

  def size: Int = members.size

  def members: Set[String] = allIncludedIds diff allExcludedIds

  def allIncludedIds: Set[String] = staticIncludeUsers.asScala.toSet ++ includeUsers.asScala.toSet ++ webgroupMembers

  def allExcludedIds: Set[String] = excludeUsers.asScala.toSet

  private def getIdFromUser(user: User): String = {
    if (universityIds)
      user.getWarwickId
    else
      user.getUserId
  }

  private def getUsersFromIds(ids: Set[String]): Set[User] = ids.toSeq match {
    case Nil => Set.empty
    case list if universityIds => userLookup.usersByWarwickUniIds(list).values.toSet
    case list => userLookup.usersByUserIds(list).values.toSet
  }

  @transient private var _cachedUsers: Option[Set[User]] = None
  @transient private var _cachedUsersMembers: Option[Set[String]] = None

  def users: Set[User] = {
    val resolvedMembers = members
    _cachedUsers match {
      case Some(result) if _cachedUsersMembers.contains(resolvedMembers) => result
      case _ =>
        val result = getUsersFromIds(resolvedMembers)
        _cachedUsers = Some(result)
        _cachedUsersMembers = Some(resolvedMembers)
        result
    }
  }

  def items: Set[(User, UserGroupItemType)] =
    getUsersFromIds(includeUsers.asScala.toSet).map((_, UserGroupItemType.Included)) ++
    getUsersFromIds(staticIncludeUsers.asScala.toSet).map((_, UserGroupItemType.Static)) ++
    getUsersFromIds(webgroupMembers).map((_, UserGroupItemType.WebGroup)) ++
    getUsersFromIds(excludeUsers.asScala.toSet).map((_, UserGroupItemType.Excluded))

  def excludes: Set[User] = getUsersFromIds(excludeUsers.asScala.toSet)

  private def webgroupMembers: Set[String] = baseWebgroup match {
    case webgroup: String =>
      groupService.getUserCodesInGroup(webgroup).asScala.toSet ++ groupService.getGroupByName(webgroup).getOwners.asScala.toSet
    case _ => Set.empty
  }

  def copyFrom(otherGroup: UnspecifiedTypeUserGroup): Unit = {
    assert(this.universityIds == otherGroup.universityIds, "Can only copy from a group with same type of users")

    val other = otherGroup.knownType
    baseWebgroup = other.baseWebgroup
    includedUserIds = other.includedUserIds
    excludedUserIds = other.excludedUserIds
    staticUserIds = other.staticUserIds
  }

  def duplicate(): UserGroup = {
    val newGroup = new UserGroup(this.universityIds)
    newGroup.sessionFactory = this.sessionFactory
    newGroup.copyFrom(this)
    newGroup.userLookup = this.userLookup
    newGroup
  }

  def hasSameMembersAs(other: UnspecifiedTypeUserGroup): Boolean = {
    other match {
      case otherUg: UserGroup if otherUg.universityIds == this.universityIds => this.members == otherUg.members
      case _ => this.users == other.users
    }
  }

  def knownType: UserGroup = this

  def isAllowedMember(user: User): Boolean = user.getUserType != "Applicant"
}

object UserGroup {
  def ofUsercodes = new UserGroup(false)

  def ofUniversityIds = new UserGroup(true)
}

sealed trait UserGroupItemType extends EnumEntry
object UserGroupItemType extends Enum[UserGroupItemType] {
  case object Included extends UserGroupItemType
  case object Static extends UserGroupItemType
  case object WebGroup extends UserGroupItemType
  case object Excluded extends UserGroupItemType

  override val values: immutable.IndexedSeq[UserGroupItemType] = findValues
}

/**
  * A usergroup where the value of universityId is hidden from the caller.
  *
  * This means that callers can only add/remove Users, not UserIds/UniversityIds - and therefore they can't add the
  * wrong type of identifier.
  *
  */

trait UnspecifiedTypeUserGroup {
  /**
    * @return All of the included users (includedUsers, staticUsers, and webgroup members), minus the excluded users
    */
  def users: Set[User]

  /**
   * @return All of the included items in this user group, with the type - this will include excluded users with the Excluded type
   */
  def items: Set[(User, UserGroupItemType)]

  def baseWebgroup: String

  /**
    * @return The explicitly excluded users
    */
  def excludes: Set[User]

  def add(user: User): Boolean

  def remove(user: User): Boolean

  def exclude(user: User): Boolean

  def unexclude(user: User): Boolean

  def size: Int

  def isEmpty: Boolean
  def nonEmpty: Boolean = !isEmpty

  def includesUser(user: User): Boolean

  def excludesUser(user: User): Boolean

  /**
    * @return true if the other.users() would return the same values as this.users(), else false
    */
  def hasSameMembersAs(other: UnspecifiedTypeUserGroup): Boolean

  def copyFrom(otherGroup: UnspecifiedTypeUserGroup): Unit

  def duplicate(): UnspecifiedTypeUserGroup

  val universityIds: Boolean

  def knownType: KnownTypeUserGroup
}

trait KnownTypeUserGroup extends UnspecifiedTypeUserGroup {
  def allIncludedIds: Set[String]

  def allExcludedIds: Set[String]

  def members: Set[String]

  def addUserId(userId: String): Boolean

  def removeUserId(userId: String): Boolean

  def excludeUserId(userId: String): Boolean

  def unexcludeUserId(userId: String): Boolean

  def staticUserIds: Set[String]

  def staticUserIds_=(userIds: Set[String]): Unit

  def includedUserIds: Set[String]

  def includedUserIds_=(userIds: Set[String]): Unit

  def excludedUserIds: Set[String]

  def excludedUserIds_=(userIds: Set[String]): Unit

  def includesUserId(userId: String): Boolean

  def excludesUserId(userId: String): Boolean
}
