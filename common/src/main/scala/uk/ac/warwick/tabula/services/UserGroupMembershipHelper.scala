package uk.ac.warwick.tabula.services

import scala.reflect._
import scala.collection.JavaConverters._
import scala.beans.BeanProperty
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.{HelperRestrictions, SessionComponent, Daoisms}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.util.cache.{Cache, Caches, SingularCacheEntryFactory}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.model.{StringId, KnownTypeUserGroup, UnspecifiedTypeUserGroup}
import uk.ac.warwick.util.queue.{Queue, QueueListener}
import org.springframework.beans.factory.InitializingBean
import uk.ac.warwick.util.queue.conversion.ItemType
import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.hibernate.criterion.Projections
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.ScalaFactoryBean
import org.joda.time.Days
import org.springframework.util.Assert
import org.apache.commons.lang3.builder.{EqualsBuilder, HashCodeBuilder, ToStringStyle, ToStringBuilder}
import uk.ac.warwick.tabula.services.permissions.AutowiringCacheStrategyComponent
import uk.ac.warwick.util.cache.Caches.CacheStrategy
import uk.ac.warwick.tabula.commands.TaskBenchmarking

trait UserGroupMembershipHelperMethods[A <: StringId with Serializable] {
	def findBy(user: User): Seq[A]
	def cache: Option[Cache[String, Array[String]]]
}

/**
 * Class for getting a collection of entities that contain
 * a UserGroup that holds a given user. For example, you could use
 * UserGroupMembershipHelper[SmallGroupEvent]("_tutors") to access small group
 * events for which user X is a tutor.
 *
 * The path can have a second part if the object goes through yet another object,
 * like [SmallGroup]("events._tutors"). We could technically allow longer paths
 * but it doesn't support that currently and if you want to do this you should
 * maybe think about whether this would be a bit much to ask of the database.
 *
 * This will cache under the default EHcache config unless you define a cache
 * called ClassName-dashed-path,
 * e.g. SmallGroup-events-_tutors or SmallGroupEvent-_tutors.
 *
 * Requires a cache bean to be specified with the cache name above.
 *
 * TODO PermissionsService ought to use this when it is fully featured
 */
private[services] class UserGroupMembershipHelper[A <: StringId with Serializable : ClassTag] (val path: String, val checkUniversityIds: Boolean = true)
	extends UserGroupMembershipHelperMethods[A]
	with UserGroupMembershipHelperLookup
	with Daoisms
	with AutowiringUserLookupComponent
	with Logging with TaskBenchmarking {

	val runtimeClass: Class[_] = classTag[A].runtimeClass

	lazy val cacheName: String = simpleEntityName + "-" + path.replace(".","-")
	lazy val cache: Option[Cache[String, Array[String]]] = Wire.optionNamed[Cache[String, Array[String]]](cacheName)

	def findBy(user: User): Seq[A] = {
		val ids = cache match {
			case Some(c) => benchmarkTask("findByUsingCache") {
				c.get(user.getUserId).toSeq
			}
			case None =>
				logger.warn(s"Couldn't find a cache bean named $cacheName")
				findByInternal(user)
		}

		if (ids.isEmpty) Nil
		else session.newCriteria[A].add(safeIn("id", ids)).seq
	}
}

trait UserGroupMembershipHelperLookup {
	self: SessionComponent with HelperRestrictions with UserLookupComponent =>

	def runtimeClass: Class[_]
	def path: String
	def checkUniversityIds: Boolean

	lazy val simpleEntityName: String = runtimeClass.getSimpleName

	// A series of confusing variables for building joined queries across paths split by.dots
	private lazy val pathParts = {
		val parts = path.split("\\.").toList.reverse

		if (parts.size > 2) throw new IllegalArgumentException("Only allowed one or two parts to the path")

		parts
	}

	// The actual name of the UserGroup
	lazy val usergroupName: String = pathParts.head
	// A possible table to join through to get to userProp
	lazy val joinTable: Option[String] = pathParts.tail.headOption
	// The overall property name, possibly including the joinTable
	lazy val prop: String = joinTable.fold("")(_ + ".") + usergroupName

	lazy val groupsByUserSql: String = {
		val leftJoin = joinTable.fold("")( table => s"left join r.$table as $table" )

		// skip the university IDs check if we know we only ever use usercodes
		val universityIdsClause =
			if (checkUniversityIds) s""" or (
					$prop.universityIds = true and
					((:universityId in elements($prop.staticIncludeUsers)
					or :universityId in elements($prop.includeUsers))
					and :universityId not in elements($prop.excludeUsers))
				)"""
			else ""

		s"""
			select r.id
			from $simpleEntityName r
			$leftJoin
			where
				(
					$prop.universityIds = false and
			 		((:userId in elements($prop.staticIncludeUsers)
					or :userId in elements($prop.includeUsers))
					and :userId not in elements($prop.excludeUsers))
				) $universityIdsClause
		"""
	}

	// To override in tests
	protected def getUser(usercode: String): User = userLookup.getUserByUserId(usercode)
	protected def getWebgroups(usercode: String): Seq[String] = usercode.maybeText.map {
		usercode => userLookup.getGroupService.getGroupsNamesForUser(usercode).asScala
	}.getOrElse(Nil)

	protected def findByInternal(user: User): Seq[String] = {
		val groupsByUser = session.createQuery(groupsByUserSql)
			.setString("universityId", user.getWarwickId)
			.setString("userId", user.getUserId)
			.list.asInstanceOf[JList[String]]
			.asScala

		val webgroupNames: Seq[String] = getWebgroups(user.getUserId)
		val groupsByWebgroup =
			if (webgroupNames.isEmpty) Nil
			else {
				val criteria = session.createCriteria(runtimeClass)

				joinTable.foreach { table =>
					criteria.createAlias(table, table)
				}

				criteria
					.createAlias(prop, "usergroupAlias")
					.add(safeIn("usergroupAlias.baseWebgroup", webgroupNames))
					.setProjection(Projections.id())
					.list.asInstanceOf[JList[String]]
					.asScala
			}

		(groupsByUser ++ groupsByWebgroup).distinct
	}
}

class UserGroupMembershipCacheFactory(val runtimeClass: Class[_], val path: String, val checkUniversityIds: Boolean = true)
	extends SingularCacheEntryFactory[String, Array[String]] with UserGroupMembershipHelperLookup with Daoisms with AutowiringUserLookupComponent  with TaskBenchmarking {

	def create(usercode: String): Array[String] = {
		val user = getUser(usercode)
		benchmarkTask("findByInternal") {
			findByInternal(user).toArray
		}
	}

	def shouldBeCached(value: Array[String]) = true
}

class UserGroupMembershipCacheBean extends ScalaFactoryBean[Cache[String, Array[String]]] with AutowiringCacheStrategyComponent {
	@BeanProperty var runtimeClass: Class[_ <: StringId with Serializable] = _
	@BeanProperty var path: String = _
	@BeanProperty var checkUniversityIds = true

	def cacheName: String = runtimeClass.getSimpleName + "-" + path.replace(".","-")

	def createInstance: Cache[String, Array[String]] = {
		Caches.newCache(
			cacheName,
			new UserGroupMembershipCacheFactory(runtimeClass, path, checkUniversityIds),
			Days.ONE.toStandardSeconds.getSeconds,
			cacheStrategy
		)
	}

	override def afterPropertiesSet() {
		Assert.notNull(runtimeClass, "Must set runtime class")
		Assert.notNull(path, "Must set path")

		super.afterPropertiesSet()
	}
}

class UserGroupMembershipHelperCacheService extends QueueListener with InitializingBean with Logging with AutowiringCacheStrategyComponent {

	var queue: Queue = Wire.named[Queue]("settingsSyncTopic")
	var context: String = Wire.property("${module.context}")

	def invalidate(helper: UserGroupMembershipHelperMethods[_], user: User) {
		helper.cache.foreach { cache =>
			cache.remove(user.getUserId)

			// Must also inform other app servers - unless we're using a shared distributed cache, i.e. Memcached
			if (cacheStrategy != CacheStrategy.MemcachedRequired && cacheStrategy != CacheStrategy.MemcachedIfAvailable) {
				val msg = new UserGroupMembershipHelperCacheBusterMessage
				msg.cacheName = cache.getName
				msg.usercode = user.getUserId
				queue.send(msg)
			}
		}
	}

	override def isListeningToQueue = true
	override def onReceive(item: Any) {
		logger.debug(s"Synchronising item $item for $context")
		item match {
			case msg: UserGroupMembershipHelperCacheBusterMessage =>
				Wire.optionNamed[Cache[String, Array[String]]](msg.cacheName) match {
					case Some(cache) =>
						cache.remove(msg.usercode)
					case None => logger.warn(s"Couldn't find a cache bean named ${msg.cacheName}")
				}
			case _ =>
		}
	}

	override def afterPropertiesSet() {
		queue.addListener(classOf[UserGroupMembershipHelperCacheBusterMessage].getAnnotation(classOf[ItemType]).value, this)
	}
}

@ItemType("UserGroupMembershipHelperCacheBuster")
@JsonAutoDetect
class UserGroupMembershipHelperCacheBusterMessage {
	@BeanProperty var cacheName: String = _
	@BeanProperty var usercode: String = _
}

class UserGroupCacheManager(val underlying: UnspecifiedTypeUserGroup, private val helper: UserGroupMembershipHelperMethods[_])
	extends UnspecifiedTypeUserGroup with KnownTypeUserGroup {

	// FIXME this isn't really an optional wire, it's just represented as such to make testing easier
	var cacheService: Option[UserGroupMembershipHelperCacheService] = Wire.option[UserGroupMembershipHelperCacheService]
	var userLookup: UserLookupService = Wire[UserLookupService]

	def add(user: User) {
		underlying.add(user)
		cacheService.foreach { _.invalidate(helper, user) }
	}
	def remove(user: User) {
		underlying.remove(user)
		cacheService.foreach { _.invalidate(helper, user) }
	}
	def exclude(user: User) {
		underlying.exclude(user)
		cacheService.foreach { _.invalidate(helper, user) }
	}
	def unexclude(user: User) {
		underlying.unexclude(user)
		cacheService.foreach { _.invalidate(helper, user) }
	}

	private def getUserFromUserId(userId: String): User =
		if (universityIds) userLookup.getUserByWarwickUniId(userId)
		else userLookup.getUserByUserId(userId)

	def addUserId(userId: String): Unit = {
		underlying.knownType.addUserId(userId)
		cacheService.foreach { _.invalidate(helper, getUserFromUserId(userId)) }
	}
	def removeUserId(userId: String): Unit = {
		underlying.knownType.removeUserId(userId)
		cacheService.foreach { _.invalidate(helper, getUserFromUserId(userId)) }
	}
	def excludeUserId(userId: String): Unit = {
		underlying.knownType.excludeUserId(userId)
		cacheService.foreach { _.invalidate(helper, getUserFromUserId(userId)) }
	}
	def unexcludeUserId(userId: String): Unit = {
		underlying.knownType.unexcludeUserId(userId)
		cacheService.foreach { _.invalidate(helper, getUserFromUserId(userId)) }
	}
	def staticUserIds_=(newIds: Seq[String]): Unit = {
		val allIds = (underlying.knownType.staticUserIds ++ newIds).distinct

		underlying.knownType.staticUserIds = newIds

		for (cacheService <- cacheService; user <- allIds.map(getUserFromUserId))
			cacheService.invalidate(helper, user)
	}
	def includedUserIds_=(newIds: Seq[String]): Unit = {
		val allIds = (underlying.knownType.includedUserIds ++ newIds).distinct

		underlying.knownType.includedUserIds = newIds

		for (cacheService <- cacheService; user <- allIds.map(getUserFromUserId))
			cacheService.invalidate(helper, user)
	}
	def excludedUserIds_=(newIds: Seq[String]): Unit = {
		val allIds = (underlying.knownType.excludedUserIds ++ newIds).distinct

		underlying.knownType.excludedUserIds = newIds

		for (cacheService <- cacheService; user <- allIds.map(getUserFromUserId))
			cacheService.invalidate(helper, user)
	}
	def copyFrom(otherGroup: UnspecifiedTypeUserGroup): Unit = {
		val allIds = (underlying.knownType.members ++ otherGroup.knownType.members).distinct

		underlying.copyFrom(otherGroup)

		for (cacheService <- cacheService; user <- allIds.map(getUserFromUserId))
			cacheService.invalidate(helper, user)
	}

	def users: Seq[User] = underlying.users
	def baseWebgroup: String = underlying.baseWebgroup
	def excludes: Seq[User] = underlying.excludes
	def size: Int = underlying.size
	def isEmpty: Boolean = underlying.isEmpty
	def includesUser(user: User): Boolean = underlying.includesUser(user)
	def excludesUser(user: User): Boolean = underlying.excludesUser(user)
	def hasSameMembersAs(other: UnspecifiedTypeUserGroup): Boolean = underlying.hasSameMembersAs(other)
	def duplicate() = new UserGroupCacheManager(underlying.duplicate(), helper) // Should this be wrapped or not?
	val universityIds: Boolean = underlying.universityIds
	def knownType: UserGroupCacheManager = this
	def allIncludedIds: Seq[String] = underlying.knownType.allIncludedIds
	def allExcludedIds: Seq[String] = underlying.knownType.allExcludedIds
	def members: Seq[String] = underlying.knownType.members
	def includedUserIds: Seq[String] = underlying.knownType.includedUserIds
	def excludedUserIds: Seq[String] = underlying.knownType.excludedUserIds
	def staticUserIds: Seq[String] = underlying.knownType.staticUserIds
	def includesUserId(userId: String): Boolean = underlying.knownType.includesUserId(userId)
	def excludesUserId(userId: String): Boolean = underlying.knownType.excludesUserId(userId)

	override def toString: String =
		new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append(underlying)
			.append(helper)
			.build()

	override def hashCode(): Int =
		new HashCodeBuilder()
			.append(underlying)
			.append(helper)
			.build()

	override def equals(other: Any): Boolean = other match {
		case that: UserGroupCacheManager =>
			new EqualsBuilder()
				.append(underlying, that.underlying)
				.append(helper, that.helper)
				.build()
		case _ => false
	}
}
