package uk.ac.warwick.tabula.services

import java.time.Duration

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.apache.commons.lang3.builder.{EqualsBuilder, HashCodeBuilder, ToStringBuilder, ToStringStyle}
import org.hibernate.criterion.Projections
import org.springframework.beans.factory.InitializingBean
import org.springframework.util.Assert
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ScalaFactoryBean
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model.{KnownTypeUserGroup, StringId, UnspecifiedTypeUserGroup, UserGroupItemType}
import uk.ac.warwick.tabula.data.{Daoisms, HelperRestrictions, SessionComponent}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{Logging, RequestLevelCache}
import uk.ac.warwick.tabula.services.permissions.AutowiringCacheStrategyComponent
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.cache.Caches.CacheStrategy
import uk.ac.warwick.util.cache.{Cache, Caches, SingularCacheEntryFactory}
import uk.ac.warwick.util.queue.conversion.ItemType
import uk.ac.warwick.util.queue.{Queue, QueueListener}

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._
import scala.reflect._

trait UserGroupMembershipHelperMethods[A <: StringId with Serializable] {
  def findBy(user: User): Seq[A]

  def cacheName: String

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
private[services] class UserGroupMembershipHelper[A <: StringId with Serializable : ClassTag](val path: String, val checkUniversityIds: Boolean = true)
  extends UserGroupMembershipHelperMethods[A]
    with UserGroupMembershipHelperLookup
    with Daoisms
    with AutowiringUserLookupComponent
    with Logging with TaskBenchmarking {

  val runtimeClass: Class[_] = classTag[A].runtimeClass

  lazy val cacheName: String = simpleEntityName + "-" + path.replace(".", "-")
  lazy val cache: Option[Cache[String, Array[String]]] = Wire.optionNamed[Cache[String, Array[String]]](cacheName)

  def findBy(user: User): Seq[A] = RequestLevelCache.cachedBy(s"UserGroupMembershipHelper[${runtimeClass.getName}].$path", user.getUserId) {
    val ids = cache match {
      case Some(c) => benchmarkTask("findByUsingCache") {
        c.get(user.getUserId).toSeq
      }
      case None => benchmarkTask("findByInternal") {
        logger.warn(s"Couldn't find a cache bean named $cacheName")
        findByInternal(user)
      }
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

  require(!path.contains('.'), "Nested paths for UserGroupMembershipHelper are no longer supported")

  lazy val groupsByUserHql: String = {
    // skip the university IDs check if we know we only ever use usercodes
    def joinRestriction(alias: String): String =
      if (checkUniversityIds)
        s"""on (
          (ug.universityIds = false and $alias = :userId) or
          (ug.universityIds = true and $alias = :universityId)
        )"""
      else s"on ug.universityIds = false and $alias = :userId"

    s"""
      select r.id
      from $simpleEntityName r
        inner join r.$path ug
        left join ug.staticIncludeUsers static ${joinRestriction("static")}
        left join ug.includeUsers include ${joinRestriction("include")}
        left join ug.excludeUsers exclude ${joinRestriction("exclude")}
			where
        (static is not null or include is not null) and exclude is null
    """
  }

  // To override in tests
  protected def getUser(usercode: String): User = userLookup.getUserByUserId(usercode)

  protected def getWebgroups(usercode: String): Seq[String] = usercode.maybeText.map { usercode =>
    userLookup.getGroupService.getGroupsNamesForUser(usercode).asScala.toSeq
  }.getOrElse(Nil)

  protected def findByInternal(user: User): Seq[String] = {
    val groupsByUser =
      session.createQuery(groupsByUserHql, classOf[String])
        .setString("universityId", user.getWarwickId)
        .setString("userId", user.getUserId)
        .list
        .asScala
        .toSeq

    val webgroupNames: Seq[String] = getWebgroups(user.getUserId)
    val groupsByWebgroup =
      if (webgroupNames.isEmpty) Nil
      else {
        val criteria = session.createCriteria(runtimeClass)

        criteria
          .createAlias(path, "usergroupAlias")
          .add(safeIn("usergroupAlias.baseWebgroup", webgroupNames))
          .setProjection(Projections.id())
          .list.asInstanceOf[JList[String]]
          .asScala
      }

    (groupsByUser ++ groupsByWebgroup).distinct
  }
}

class UserGroupMembershipCacheFactory(val runtimeClass: Class[_], val path: String, val checkUniversityIds: Boolean = true)
  extends SingularCacheEntryFactory[String, Array[String]] with UserGroupMembershipHelperLookup with Daoisms with AutowiringUserLookupComponent with TaskBenchmarking {

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

  def cacheName: String = runtimeClass.getSimpleName + "-" + path.replace(".", "-")

  def createInstance: Cache[String, Array[String]] =
    Caches.builder(cacheName, new UserGroupMembershipCacheFactory(runtimeClass, path, checkUniversityIds), cacheStrategy)
      .expireAfterWrite(Duration.ofDays(1))
      .maximumSize(10000) // Ignored by Memcached, just for Caffeine (testing)
      .build()

  override def afterPropertiesSet(): Unit = {
    Assert.notNull(runtimeClass, "Must set runtime class")
    Assert.notNull(path, "Must set path")

    super.afterPropertiesSet()
  }
}

class UserGroupMembershipHelperCacheService extends QueueListener with InitializingBean with Logging with AutowiringCacheStrategyComponent {

  var queue: Queue = Wire.named[Queue]("settingsSyncTopic")
  var context: String = Wire.property("${module.context}")

  def invalidate(helper: UserGroupMembershipHelperMethods[_], user: User): Unit = {
    helper.cache match {
      case Some(cache: Cache[String, Array[String]]) =>
        cache.remove(user.getUserId)

        // Must also inform other app servers - unless we're using a shared distributed cache, i.e. Memcached
        if (cacheStrategy != CacheStrategy.MemcachedRequired && cacheStrategy != CacheStrategy.MemcachedIfAvailable) {
          val msg = new UserGroupMembershipHelperCacheBusterMessage
          msg.cacheName = cache.getName
          msg.usercode = user.getUserId
          queue.send(msg)
        }

      case None =>
        logger.warn(s"Couldn't find a cache bean named ${helper.cacheName}")
    }
  }

  override def isListeningToQueue = true

  override def onReceive(item: Any): Unit = {
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

  override def afterPropertiesSet(): Unit = {
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

  def add(user: User): Boolean = {
    if (underlying.add(user)) {
      cacheService.foreach(_.invalidate(helper, user))
      true
    } else false
  }

  def remove(user: User): Boolean = {
    if (underlying.remove(user)) {
      cacheService.foreach(_.invalidate(helper, user))
      true
    } else false
  }

  def exclude(user: User): Boolean = {
    if (underlying.exclude(user)) {
      cacheService.foreach(_.invalidate(helper, user))
      true
    } else false
  }

  def unexclude(user: User): Boolean = {
    if (underlying.unexclude(user)) {
      cacheService.foreach(_.invalidate(helper, user))
      true
    } else false
  }

  private def getUserFromUserId(userId: String): User =
    if (universityIds) userLookup.getUserByWarwickUniId(userId)
    else userLookup.getUserByUserId(userId)

  def addUserId(userId: String): Boolean = {
    if (underlying.knownType.addUserId(userId)) {
      cacheService.foreach(_.invalidate(helper, getUserFromUserId(userId)))
      true
    } else false
  }

  def removeUserId(userId: String): Boolean = {
    if (underlying.knownType.removeUserId(userId)) {
      cacheService.foreach(_.invalidate(helper, getUserFromUserId(userId)))
      true
    } else false
  }

  def excludeUserId(userId: String): Boolean = {
    if (underlying.knownType.excludeUserId(userId)) {
      cacheService.foreach(_.invalidate(helper, getUserFromUserId(userId)))
      true
    } else false
  }

  def unexcludeUserId(userId: String): Boolean = {
    if (underlying.knownType.unexcludeUserId(userId)) {
      cacheService.foreach(_.invalidate(helper, getUserFromUserId(userId)))
      true
    } else false
  }

  def staticUserIds_=(newIds: Set[String]): Unit = {
    val existingIds = underlying.knownType.staticUserIds
    val addedIds = newIds.diff(existingIds)
    val removedIds = existingIds.diff(newIds)

    val allIds = addedIds ++ removedIds

    underlying.knownType.staticUserIds = newIds

    for (cacheService <- cacheService; user <- allIds.map(getUserFromUserId))
      cacheService.invalidate(helper, user)
  }

  def includedUserIds_=(newIds: Set[String]): Unit = {
    val existingIds = underlying.knownType.includedUserIds
    val addedIds = newIds.diff(existingIds)
    val removedIds = existingIds.diff(newIds)

    val allIds = addedIds ++ removedIds

    underlying.knownType.includedUserIds = newIds

    for (cacheService <- cacheService; user <- allIds.map(getUserFromUserId))
      cacheService.invalidate(helper, user)
  }

  def excludedUserIds_=(newIds: Set[String]): Unit = {
    val existingIds = underlying.knownType.excludedUserIds
    val addedIds = newIds.diff(existingIds)
    val removedIds = existingIds.diff(newIds)

    val allIds = addedIds ++ removedIds

    underlying.knownType.excludedUserIds = newIds

    for (cacheService <- cacheService; user <- allIds.map(getUserFromUserId))
      cacheService.invalidate(helper, user)
  }

  def copyFrom(otherGroup: UnspecifiedTypeUserGroup): Unit = {
    val existingIds = underlying.knownType.members
    val newIds = otherGroup.knownType.members

    val addedIds = newIds.diff(existingIds)
    val removedIds = existingIds.diff(newIds)

    val allIds = addedIds ++ removedIds

    underlying.copyFrom(otherGroup)

    for (cacheService <- cacheService; user <- allIds.map(getUserFromUserId))
      cacheService.invalidate(helper, user)
  }

  def users: Set[User] = underlying.users

  def items: Set[(User, UserGroupItemType)] = underlying.items

  def baseWebgroup: String = underlying.baseWebgroup

  def excludes: Set[User] = underlying.excludes

  def size: Int = underlying.size

  def isEmpty: Boolean = underlying.isEmpty

  def includesUser(user: User): Boolean = underlying.includesUser(user)

  def excludesUser(user: User): Boolean = underlying.excludesUser(user)

  def hasSameMembersAs(other: UnspecifiedTypeUserGroup): Boolean = underlying.hasSameMembersAs(other)

  def duplicate() = new UserGroupCacheManager(underlying.duplicate(), helper) // Should this be wrapped or not?
  val universityIds: Boolean = underlying.universityIds

  def knownType: UserGroupCacheManager = this

  def allIncludedIds: Set[String] = underlying.knownType.allIncludedIds

  def allExcludedIds: Set[String] = underlying.knownType.allExcludedIds

  def members: Set[String] = underlying.knownType.members

  def includedUserIds: Set[String] = underlying.knownType.includedUserIds

  def excludedUserIds: Set[String] = underlying.knownType.excludedUserIds

  def staticUserIds: Set[String] = underlying.knownType.staticUserIds

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
