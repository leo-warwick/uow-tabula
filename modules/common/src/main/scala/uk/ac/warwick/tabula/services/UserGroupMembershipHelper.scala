package uk.ac.warwick.tabula.services

import org.hibernate.SessionFactory

import scala.reflect._
import scala.beans.BeanProperty
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.util.cache.{Cache, Caches, SingularCacheEntryFactory}
import uk.ac.warwick.tabula.data.model.{StringId, KnownTypeUserGroup, UnspecifiedTypeUserGroup}
import uk.ac.warwick.util.queue.{Queue, QueueListener}
import org.springframework.beans.factory.InitializingBean
import uk.ac.warwick.util.queue.conversion.ItemType
import com.fasterxml.jackson.annotation.JsonAutoDetect
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.ScalaFactoryBean
import org.joda.time.Days
import org.springframework.util.Assert
import org.apache.commons.lang3.builder.{EqualsBuilder, HashCodeBuilder, ToStringStyle, ToStringBuilder}
import uk.ac.warwick.tabula.services.permissions.AutowiringCacheStrategyComponent
import uk.ac.warwick.util.cache.Caches.CacheStrategy

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
		with AutowiringUserLookupComponent
		with AutowiringUserGroupDaoComponent
		with Logging with HelperRestrictions {

	var sessionFactory = Wire[SessionFactory]
	val runtimeClass = classTag[A].runtimeClass

	lazy val cacheName = runtimeClass.getSimpleName + "-" + path.replace(".","-")
	lazy val cache: Option[Cache[String, Array[String]]] = Wire.optionNamed[Cache[String, Array[String]]](cacheName)

	def findBy(user: User): Seq[A] = {
		val ids = cache match {
			case Some(c) =>
				c.get(user.getUserId).toSeq
			case None =>
				logger.warn(s"Couldn't find a cache bean named $cacheName")
				userGroupDao.getUserGroupIds(user, runtimeClass, path, checkUniversityIds)
		}

		if (ids.isEmpty) Nil
		else {
			val session = MaintenanceModeAwareSession(sessionFactory.getCurrentSession)
			safeInSeq({ () => session.newCriteria[A] }, "id", ids)
		}
	}
}

class UserGroupMembershipCacheFactory(val runtimeClass: Class[_ <: StringId with Serializable], val path: String, val checkUniversityIds: Boolean = true)
	extends SingularCacheEntryFactory[String, Array[String]] with AutowiringUserLookupComponent with AutowiringUserGroupDaoComponent {

	def create(usercode: String) = {
		val user = userLookup.getUserByUserId(usercode)
		userGroupDao.getUserGroupIds(user, runtimeClass, path, checkUniversityIds).toArray
	}

	def shouldBeCached(value: Array[String]) = true
}

class UserGroupMembershipCacheBean extends ScalaFactoryBean[Cache[String, Array[String]]] with AutowiringCacheStrategyComponent {
	@BeanProperty var runtimeClass: Class[_ <: StringId with Serializable] = _
	@BeanProperty var path: String = _
	@BeanProperty var checkUniversityIds = true

	def cacheName = runtimeClass.getSimpleName + "-" + path.replace(".","-")

	def createInstance = {
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

	var queue = Wire.named[Queue]("settingsSyncTopic")
	var context = Wire.property("${module.context}")

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
	var cacheService = Wire.option[UserGroupMembershipHelperCacheService]
	var userLookup = Wire[UserLookupService]

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

	def addUserId(userId: String) = {
		underlying.knownType.addUserId(userId)
		cacheService.foreach { _.invalidate(helper, getUserFromUserId(userId)) }
	}
	def removeUserId(userId: String) = {
		underlying.knownType.removeUserId(userId)
		cacheService.foreach { _.invalidate(helper, getUserFromUserId(userId)) }
	}
	def excludeUserId(userId: String) = {
		underlying.knownType.excludeUserId(userId)
		cacheService.foreach { _.invalidate(helper, getUserFromUserId(userId)) }
	}
	def unexcludeUserId(userId: String) = {
		underlying.knownType.unexcludeUserId(userId)
		cacheService.foreach { _.invalidate(helper, getUserFromUserId(userId)) }
	}
	def staticUserIds_=(newIds: Seq[String]) = {
		val allIds = (underlying.knownType.staticUserIds ++ newIds).distinct

		underlying.knownType.staticUserIds = newIds

		for (cacheService <- cacheService; user <- allIds.map(getUserFromUserId))
			cacheService.invalidate(helper, user)
	}
	def includedUserIds_=(newIds: Seq[String]) = {
		val allIds = (underlying.knownType.includedUserIds ++ newIds).distinct

		underlying.knownType.includedUserIds = newIds

		for (cacheService <- cacheService; user <- allIds.map(getUserFromUserId))
			cacheService.invalidate(helper, user)
	}
	def excludedUserIds_=(newIds: Seq[String]) = {
		val allIds = (underlying.knownType.excludedUserIds ++ newIds).distinct

		underlying.knownType.excludedUserIds = newIds

		for (cacheService <- cacheService; user <- allIds.map(getUserFromUserId))
			cacheService.invalidate(helper, user)
	}
	def copyFrom(otherGroup: UnspecifiedTypeUserGroup) = {
		val allIds = (underlying.knownType.members ++ otherGroup.knownType.members).distinct

		underlying.copyFrom(otherGroup)

		for (cacheService <- cacheService; user <- allIds.map(getUserFromUserId))
			cacheService.invalidate(helper, user)
	}

	def users = underlying.users
	def baseWebgroup = underlying.baseWebgroup
	def excludes = underlying.excludes
	def size = underlying.size
	def isEmpty = underlying.isEmpty
	def includesUser(user: User) = underlying.includesUser(user)
	def excludesUser(user: User) = underlying.excludesUser(user)
	def hasSameMembersAs(other: UnspecifiedTypeUserGroup) = underlying.hasSameMembersAs(other)
	def duplicate() = new UserGroupCacheManager(underlying.duplicate(), helper) // Should this be wrapped or not?
	val universityIds = underlying.universityIds
	def knownType = this
	def allIncludedIds = underlying.knownType.allIncludedIds
	def allExcludedIds = underlying.knownType.allExcludedIds
	def members = underlying.knownType.members
	def includedUserIds = underlying.knownType.includedUserIds
	def excludedUserIds = underlying.knownType.excludedUserIds
	def staticUserIds = underlying.knownType.staticUserIds
	def includesUserId(userId: String) = underlying.knownType.includesUserId(userId)
	def excludesUserId(userId: String) = underlying.knownType.excludesUserId(userId)

	override def toString =
		new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append(underlying)
			.append(helper)
			.build()

	override def hashCode() =
		new HashCodeBuilder()
			.append(underlying)
			.append(helper)
			.build()

	override def equals(other: Any) = other match {
		case that: UserGroupCacheManager =>
			new EqualsBuilder()
				.append(underlying, that.underlying)
				.append(helper, that.helper)
				.build()
		case _ => false
	}
}
