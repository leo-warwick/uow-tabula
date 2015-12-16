package uk.ac.warwick.tabula.data.model

import javax.persistence.CascadeType._
import javax.persistence._

import org.hibernate.SessionFactory
import org.hibernate.annotations.Type
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.util.Try

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
@Access(AccessType.FIELD)
class UserGroup private(val universityIds: Boolean)
	extends UnspecifiedTypeUserGroup
		with GeneratedId
		with KnownTypeUserGroup {

	/* For Hibernate xx */
	def this() { this(false) }

	@transient var userLookup = Wire[UserLookupService]
	private def groupService = userLookup.getGroupService

	var baseWebgroup: String = _

	def baseWebgroupSize = groupService.getGroupInfo(baseWebgroup).getSize

	@ElementCollection @Column(name = "usercode")
	@JoinTable(name = "UserGroupInclude", joinColumns = Array(
		new JoinColumn(name = "group_id", referencedColumnName = "id")))
	private val includeUsers: JList[String] = JArrayList()

	@OneToMany(mappedBy = "userGroup", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@OrderBy("position")
	private val staticIncludeUsers: JList[OrderedGroupMember] = JArrayList()

	@ElementCollection @Column(name = "usercode")
	@JoinTable(name = "UserGroupExclude", joinColumns = Array(
		new JoinColumn(name = "group_id", referencedColumnName = "id")))
	private val excludeUsers: JList[String] = JArrayList()

	def includedUserIds: Seq[String] = includeUsers.asScala
	def includedUserIds_=(userIds: Seq[String]) {
		includeUsers.clear()
		includeUsers.addAll(userIds.asJava)
	}

	def staticUserIds: Seq[String] = staticIncludeUsers.asScala.map(_.memberId)
	def staticUserIds_=(userIds: Seq[String]) {

		val newMembers = userIds.map(uid => {
			val group = new OrderedGroupMember
			group.memberId = uid
			group.userGroup = this
			group
		})

		if (!staticIncludeUsers.isEmpty) {
			staticIncludeUsers.clear()
			flushSession()
		}

		staticIncludeUsers.addAll(newMembers.asJava)
	}

	private def saveNewMembers(members: Seq[OrderedGroupMember]): Unit =
		Wire.option[SessionFactory].flatMap { sf => Try(sf.getCurrentSession).toOption }.foreach { s => members.foreach(s.save) }

	private def flushSession(): Unit =
		// TAB-3343 - force deletions before inserts
		Wire.option[SessionFactory].flatMap { sf => Try(sf.getCurrentSession).toOption }.foreach { _.flush() }

	def replaceStaticUsers(registrations: Seq[UpstreamModuleRegistration]) {
		val newMembers = registrations.map(member => {
			val m = new OrderedGroupMember
			m.memberId = member.universityId
			m.userGroup = this
			m.position = None
			m
		})
		val distinctNewMembers = newMembers.groupBy(m => (m.memberId, m.userGroup)).mapValues(_.head).values.toSeq

		if (!staticIncludeUsers.isEmpty) {

			staticIncludeUsers.clear()
			flushSession()
		}

		staticIncludeUsers.addAll(distinctNewMembers.asJava)
		saveNewMembers(distinctNewMembers)
	}

	def excludedUserIds: Seq[String] = excludeUsers.asScala
	def excludedUserIds_=(userIds: Seq[String]) {
		excludeUsers.clear()
		excludeUsers.addAll(userIds.asJava)
	}

	def add(user:User) = {
		addUserId(getIdFromUser(user))
	}
	def addUserId(user: String) {
		if (!includeUsers.contains(user) && user.hasText) {
			includeUsers.add(user)
		}
	}
	def removeUserId(user: String) = includeUsers.remove(user)

	def remove(user:User) = {
		removeUserId(getIdFromUser(user))
	}

	def excludeUserId(user: String) {
		if (!excludeUsers.contains(user) && user.hasText) {
			excludeUsers.add(user)
		}
	}
	def exclude(user:User)={
		excludeUserId(getIdFromUser(user))
	}
	def unexcludeUserId(user: String) = excludeUsers.remove(user)
  def unexclude(user:User)={
		unexcludeUserId(getIdFromUser(user))
	}

	/*
	 * Could implement as `members.contains(user)`
	 * but this is more efficient
	 */
	def includesUserId(user: String) =
		!(excludeUsers contains user) &&
			(
				(includeUsers contains user) ||
				(staticIncludeUsers.asScala.map(_.memberId) contains user) ||
				(baseWebgroup != null && groupService.isUserInGroup(user, baseWebgroup)))

	def excludesUserId(user: String) = excludeUsers contains user

	def includesUser(user:User) = includesUserId(getIdFromUser(user))
	def excludesUser(user:User) = excludesUserId(getIdFromUser(user))

	def isEmpty = members.isEmpty
	def size = members.size

	def members: Seq[String] = allIncludedIds diff allExcludedIds
	def sortedStaticMembers: Seq[OrderedGroupMember] = staticIncludeUsers.asScala.sortBy(_.position)

	def allIncludedIds: Seq[String] = (staticIncludeUsers.asScala.map(_.memberId) ++ includeUsers.asScala.toSeq ++ webgroupMembers).distinct
	def allExcludedIds: Seq[String] = excludeUsers.asScala.toSeq

	private def getIdFromUser(user:User):String = {
		if (universityIds)
			user.getWarwickId
		else
			user.getUserId
	}
	private def getUsersFromIds(ids: Seq[String]): Seq[User] = ids match {
		case Nil => Nil
		case list if universityIds => userLookup.getUsersByWarwickUniIds(list).values.toSeq
		case list => userLookup.getUsersByUserIds(list.asJava).values.asScala.toSeq
	}

	def users: Seq[User] = getUsersFromIds(members)

	def excludes: Seq[User] = getUsersFromIds(excludeUsers.asScala)

	private def webgroupMembers: List[String] = baseWebgroup match {
		case webgroup: String => groupService.getUserCodesInGroup(webgroup).asScala.toList
		case _ => Nil
	}


	def copyFrom(otherGroup: UnspecifiedTypeUserGroup) {
		assert(this.universityIds == otherGroup.universityIds, "Can only copy from a group with same type of users")

		val other = otherGroup.knownType
		baseWebgroup = other.baseWebgroup
		includedUserIds = other.includedUserIds
		excludedUserIds = other.excludedUserIds
		staticUserIds = other.staticUserIds
	}

	def duplicate(): UserGroup = {
		val newGroup = new UserGroup(this.universityIds)
		newGroup.copyFrom(this)
		newGroup.userLookup = this.userLookup
		newGroup
	}

	def hasSameMembersAs(other:UnspecifiedTypeUserGroup):Boolean ={
		other match {
			case otherUg:UserGroup if otherUg.universityIds == this.universityIds => this.members == otherUg.members
			case _ => this.users == other.users
		}
	}

	def knownType = this
}

object UserGroup {
	def ofUsercodes = new UserGroup(false)
	def ofUniversityIds = new UserGroup(true)
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
	def users: Seq[User]

	def baseWebgroup: String

	/**
	 * @return The explicitly excluded users
	 */
	def excludes: Seq[User]
	def add(user:User)
	def remove(user:User)
	def exclude(user:User)
	def unexclude(user:User)
	def size:Int
	def isEmpty:Boolean
  def includesUser(user:User):Boolean
  def excludesUser(user:User):Boolean

	/**
	 * @return true if the other.users() would return the same values as this.users(), else false
	 */
	def hasSameMembersAs(other:UnspecifiedTypeUserGroup): Boolean

	def copyFrom(otherGroup: UnspecifiedTypeUserGroup): Unit
	def duplicate(): UnspecifiedTypeUserGroup

	val universityIds: Boolean
	def knownType: KnownTypeUserGroup
}

trait KnownTypeUserGroup extends UnspecifiedTypeUserGroup {
	def allIncludedIds: Seq[String]
	def allExcludedIds: Seq[String]
	def members: Seq[String]

	def addUserId(userId: String)
	def removeUserId(userId: String)
	def excludeUserId(userId: String)
	def unexcludeUserId(userId: String)

	def staticUserIds: Seq[String]
	def staticUserIds_=(userIds: Seq[String])

	def includedUserIds: Seq[String]
	def includedUserIds_=(userIds: Seq[String])

	def excludedUserIds: Seq[String]
	def excludedUserIds_=(userIds: Seq[String])

	def includesUserId(userId: String): Boolean
	def excludesUserId(userId: String): Boolean
}

@Entity(name="usergroupstatic")
@Access(AccessType.FIELD)
class OrderedGroupMember() extends GeneratedId {

	@Column(name="usercode")
	var memberId: String = _

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="group_id")
	var userGroup: UserGroup = _

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	var position: Option[Int] = _
}