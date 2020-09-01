package uk.ac.warwick.tabula.services.permissions

import java.time.Duration

import com.fasterxml.jackson.annotation.{JsonAutoDetect, JsonIgnore}
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, GrantedPermission, GrantedRole}
import uk.ac.warwick.tabula.data.model.{Department, UnspecifiedTypeUserGroup}
import uk.ac.warwick.tabula.data.{AutowiringPermissionsDaoComponent, PermissionsDao, PermissionsDaoComponent}
import uk.ac.warwick.tabula.helpers.{Logging, RequestLevelCaching}
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget}
import uk.ac.warwick.tabula.roles.{BuiltInRoleDefinition, RoleDefinition}
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.userlookup.GroupService
import uk.ac.warwick.util.cache.Caches.CacheStrategy
import uk.ac.warwick.util.cache.{Cache, CacheEntryFactory, Caches, SingularCacheEntryFactory}
import uk.ac.warwick.util.queue.conversion.ItemType
import uk.ac.warwick.util.queue.{Queue, QueueListener}

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._
import scala.reflect._

trait PermissionsService {
  def saveOrUpdate(roleDefinition: CustomRoleDefinition): Unit

  def saveOrUpdate(permission: GrantedPermission[_]): Unit

  def saveOrUpdate(role: GrantedRole[_]): Unit

  def delete(role: GrantedRole[_]): Unit

  def delete(roleDefinition: CustomRoleDefinition): Unit

  def getCustomRoleDefinitionById(id: String): Option[CustomRoleDefinition]

  def getOrCreateGrantedRole[A <: PermissionsTarget : ClassTag](target: A, defn: RoleDefinition): GrantedRole[A]

  def getGrantedRole[A <: PermissionsTarget : ClassTag](scope: A, roleDefinition: RoleDefinition): Option[GrantedRole[A]]

  def getGrantedPermission[A <: PermissionsTarget : ClassTag](scope: A, permission: Permission, overrideType: Boolean): Option[GrantedPermission[A]]

  def getGrantedRolesFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedRole[_]]

  def getGrantedPermissionsFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedPermission[_]]

  def getAllGrantedRolesFor(user: CurrentUser): Seq[GrantedRole[_]]

  def getAllGrantedPermissionsFor(user: CurrentUser): Seq[GrantedPermission[_]]

  def getAllGrantedRolesFor[A <: PermissionsTarget : ClassTag](scope: A): Seq[GrantedRole[A]]

  def getAllGrantedPermissionsFor[A <: PermissionsTarget : ClassTag](scope: A): Seq[GrantedPermission[A]]

  def getAllGrantedRolesForDefinition(roleDefinition: RoleDefinition): Seq[GrantedRole[_]]

  def getGrantedRolesFor[A <: PermissionsTarget : ClassTag](user: CurrentUser): LazyList[GrantedRole[A]]

  def getGrantedPermissionsFor[A <: PermissionsTarget : ClassTag](user: CurrentUser): LazyList[GrantedPermission[A]]

  def getAllPermissionDefinitionsFor[A <: PermissionsTarget : ClassTag](user: CurrentUser, targetPermission: Permission): Set[A]

  def ensureUserGroupFor[A <: PermissionsTarget : ClassTag](scope: A, roleDefinition: RoleDefinition): UnspecifiedTypeUserGroup

  def getCustomRoleDefinitionsBasedOn(roleDefinition: RoleDefinition): Seq[CustomRoleDefinition]

  def getCustomRoleDefinitionsFor(department: Department): Seq[CustomRoleDefinition]

  def getGlobalGrantedRole(roleDefinition: RoleDefinition): Option[GrantedRole[PermissionsTarget]]
  def getOrCreateGlobalGrantedRole(roleDefinition: RoleDefinition): GrantedRole[PermissionsTarget]
  def getAllGlobalGrantedRoles: Seq[GrantedRole[PermissionsTarget]]

  def getGlobalGrantedPermission(permission: Permission, overrideType: Boolean): Option[GrantedPermission[PermissionsTarget]]
  def getOrCreateGlobalGrantedPermission(permission: Permission, overrideType: Boolean): GrantedPermission[PermissionsTarget]
  def getAllGlobalGrantedPermissions: Seq[GrantedPermission[PermissionsTarget]]

  def clearCachesForUser(cacheKey: (String, ClassTag[_ <: PermissionsTarget]), propagate: Boolean = true): Unit

  def clearCachesForWebgroups(cacheKey: (Seq[String], ClassTag[_ <: PermissionsTarget]), propagate: Boolean = true): Unit
}

@Service(value = "permissionsService")
class AutowiringPermissionsServiceImpl
  extends AbstractPermissionsService
    with AutowiringUserLookupComponent
    with AutowiringCacheStrategyComponent
    with AutowiringPermissionsDaoComponent
    with PermissionsServiceCachesImpl
    with QueueListener
    with InitializingBean
    with Logging

trait PermissionsServiceComponent {
  def permissionsService: PermissionsService
}

trait AutowiringPermissionsServiceComponent extends PermissionsServiceComponent {
  var permissionsService: PermissionsService = Wire[PermissionsService]
}

trait CacheStrategyComponent {
  def cacheStrategy: CacheStrategy
}

trait AutowiringCacheStrategyComponent extends CacheStrategyComponent {
  var cacheStrategy: CacheStrategy = CacheStrategy.valueOf(Wire.property("${tabula.cacheStrategy:CaffeineRequired}"))
}

abstract class AbstractPermissionsService extends PermissionsService {
  self: PermissionsDaoComponent
    with UserLookupComponent
    with CacheStrategyComponent
    with PermissionsServiceCaches
    with QueueListener
    with InitializingBean
    with Logging =>

  var groupService: GroupService = Wire[GroupService]
  var queue: Queue = Wire.named[Queue]("settingsSyncTopic")


  override def isListeningToQueue = true

  override def onReceive(item: Any): Unit = {
    item match {
      case copy: PermissionsCacheBusterMessage if Option(copy.usercode).isDefined =>
        clearCachesForUser((copy.usercode, copy.classTag), propagate = false)
      case copy: PermissionsCacheBusterMessage =>
        clearCachesForWebgroups((copy.webgroups, copy.classTag), propagate = false)
      case _ =>
    }
  }

  override def afterPropertiesSet(): Unit = {
    queue.addListener(classOf[PermissionsCacheBusterMessage].getAnnotation(classOf[ItemType]).value, this)
  }

  def clearCachesForUser(cacheKey: (String, ClassTag[_ <: PermissionsTarget]), propagate: Boolean = true): Unit = {
    GrantedRolesForUserCache.remove(cacheKey)
    GrantedPermissionsForUserCache.remove(cacheKey)

    // Also clear for [PermissionsTarget]
    GrantedRolesForUserCache.remove((cacheKey._1, classTag[PermissionsTarget]))
    GrantedPermissionsForUserCache.remove((cacheKey._1, classTag[PermissionsTarget]))

    if (propagate && cacheStrategy != CacheStrategy.MemcachedRequired && cacheStrategy != CacheStrategy.MemcachedIfAvailable) {
      val msg = new PermissionsCacheBusterMessage
      msg.usercode = cacheKey._1
      msg.classTag = cacheKey._2
      queue.send(msg)
    }
  }

  def clearCachesForWebgroups(cacheKey: (Seq[String], ClassTag[_ <: PermissionsTarget]), propagate: Boolean = true): Unit = {
    GrantedRolesForGroupCache.remove(cacheKey)
    GrantedPermissionsForGroupCache.remove(cacheKey)

    // Also clear for [PermissionsTarget]
    GrantedRolesForGroupCache.remove((cacheKey._1, classTag[PermissionsTarget]))
    GrantedPermissionsForGroupCache.remove((cacheKey._1, classTag[PermissionsTarget]))

    if (propagate && cacheStrategy != CacheStrategy.MemcachedRequired && cacheStrategy != CacheStrategy.MemcachedIfAvailable) {
      val msg = new PermissionsCacheBusterMessage
      msg.webgroups = cacheKey._1
      msg.classTag = cacheKey._2
      queue.send(msg)
    }
  }

  def saveOrUpdate(roleDefinition: CustomRoleDefinition): Unit = permissionsDao.saveOrUpdate(roleDefinition)

  def saveOrUpdate(permission: GrantedPermission[_]): Unit = permissionsDao.saveOrUpdate(permission)

  def saveOrUpdate(role: GrantedRole[_]): Unit = permissionsDao.saveOrUpdate(role)

  def delete(role: GrantedRole[_]): Unit = permissionsDao.delete(role)

  def delete(roleDefinition: CustomRoleDefinition): Unit = {
    roleDefinition.department.customRoleDefinitions.remove(roleDefinition)
    permissionsDao.delete(roleDefinition)
  }

  def getCustomRoleDefinitionById(id: String): Option[CustomRoleDefinition] = permissionsDao.getCustomRoleDefinitionById(id)

  def getOrCreateGrantedRole[A <: PermissionsTarget : ClassTag](target: A, defn: RoleDefinition): GrantedRole[A] =
    getGrantedRole(target, defn) match {
      case Some(role) => role
      case _ => GrantedRole(target, defn)
    }

  def getGrantedRole[A <: PermissionsTarget : ClassTag](scope: A, roleDefinition: RoleDefinition): Option[GrantedRole[A]] =
    transactional(readOnly = true) {
      roleDefinition match {
        case builtIn: BuiltInRoleDefinition => permissionsDao.getGrantedRole(scope, builtIn)
        case custom: CustomRoleDefinition => permissionsDao.getGrantedRole(scope, custom)
        case _ => None
      }
    }

  def getGrantedPermission[A <: PermissionsTarget : ClassTag](scope: A, permission: Permission, overrideType: Boolean): Option[GrantedPermission[A]] =
    transactional(readOnly = true) {
      permissionsDao.getGrantedPermission(scope, permission, overrideType)
    }

  private def ensureFoundUserSeq[A](user: CurrentUser)(fn: => Seq[A]): Seq[A] =
    if (user.exists) fn
    else Seq.empty

  private def ensureFoundUserSet[A](user: CurrentUser)(fn: => Set[A]): Set[A] =
    if (user.exists) fn
    else Set.empty

  private def ensureFoundUserLazyList[A](user: CurrentUser)(fn: => LazyList[A]): LazyList[A] =
    if (user.exists) fn
    else LazyList.empty

  def getGrantedRolesFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedRole[_]] = ensureFoundUserSeq(user)(transactional(readOnly = true) {
    permissionsDao.getGrantedRolesFor(scope).filter(_.users.includesUser(user.apparentUser))
  })

  def getGrantedPermissionsFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedPermission[_]] =
    ensureFoundUserSeq(user)(transactional(readOnly = true) {
      permissionsDao.getGrantedPermissionsFor(scope).to(LazyList).filter(_.users.includesUser(user.apparentUser))
    })

  def getAllGrantedRolesFor(user: CurrentUser): Seq[GrantedRole[_]] = ensureFoundUserSeq(user)(getGrantedRolesFor[PermissionsTarget](user))

  def getAllGrantedPermissionsFor(user: CurrentUser): Seq[GrantedPermission[_]] = ensureFoundUserSeq(user)(getGrantedPermissionsFor[PermissionsTarget](user))

  def getAllGrantedRolesFor[A <: PermissionsTarget : ClassTag](scope: A): Seq[GrantedRole[A]] = permissionsDao.getGrantedRolesFor(scope)

  def getAllGrantedPermissionsFor[A <: PermissionsTarget : ClassTag](scope: A): Seq[GrantedPermission[A]] = permissionsDao.getGrantedPermissionsFor(scope)

  def getAllGrantedRolesForDefinition(roleDefinition: RoleDefinition): Seq[GrantedRole[_]] = permissionsDao.getGrantedRolesForDefinition(roleDefinition)

  def getGrantedRolesFor[A <: PermissionsTarget : ClassTag](user: CurrentUser): LazyList[GrantedRole[A]] =
    ensureFoundUserLazyList(user)(transactional(readOnly = true) {
      val groupNames = groupService.getGroupsNamesForUser(user.apparentId).asScala.toSeq

      rolesByIdCache.getGrantedRolesByIds[A](
        // Get all roles where usercode is included,
        GrantedRolesForUserCache.get((user.apparentId, classTag[A])).asScala.toSeq

        // Get all roles backed by one of the webgroups,
        ++ GrantedRolesForGroupCache.get((groupNames, classTag[A])).asScala.toSeq
      ).to(LazyList)
        // For sanity's sake, filter by the users including the user
        .filter(_.users.includesUser(user.apparentUser))
    })

  def getGrantedPermissionsFor[A <: PermissionsTarget : ClassTag](user: CurrentUser): LazyList[GrantedPermission[A]] =
    ensureFoundUserLazyList(user)(transactional(readOnly = true) {
      val groupNames = groupService.getGroupsNamesForUser(user.apparentId).asScala.toSeq

      permissionsByIdCache.getGrantedPermissionsByIds[A](
        // Get all permissions where usercode is included,
        GrantedPermissionsForUserCache.get((user.apparentId, classTag[A])).asScala.toSeq

        // Get all permissions backed by one of the webgroups,
        ++ GrantedPermissionsForGroupCache.get((groupNames, classTag[A])).asScala.toSeq
      ).to(LazyList)
        // For sanity's sake, filter by the users including the user
        .filter(_.users.includesUser(user.apparentUser))
    })

  def getAllPermissionDefinitionsFor[A <: PermissionsTarget : ClassTag](user: CurrentUser, targetPermission: Permission): Set[A] = ensureFoundUserSet(user) { transactional(readOnly = true) {
    val scopesWithGrantedRole =
      getGrantedRolesFor[A](user)
        .filter(_.mayGrant(targetPermission))
        .map(_.scope)
        .toSet

    val scopesWithGrantedPermission =
      getGrantedPermissionsFor[A](user)
        .filter { perm => perm.overrideType == GrantedPermission.Allow && perm.permission == targetPermission }
        .map(_.scope)
        .toSet

    scopesWithGrantedRole ++ scopesWithGrantedPermission
  }}

  def ensureUserGroupFor[A <: PermissionsTarget : ClassTag](scope: A, roleDefinition: RoleDefinition): UnspecifiedTypeUserGroup = transactional() {
    getGrantedRole(scope, roleDefinition) match {
      case Some(role) => role.users
      case _ =>
        val role = GrantedRole(scope, roleDefinition)

        permissionsDao.saveOrUpdate(role)
        role.users
    }
  }

  def getCustomRoleDefinitionsBasedOn(roleDefinition: RoleDefinition): Seq[CustomRoleDefinition] = {
    permissionsDao.getCustomRoleDefinitionsBasedOn(roleDefinition)
  }

  def getCustomRoleDefinitionsFor(department: Department): Seq[CustomRoleDefinition] = {
    def departmentPlusParents(department: Department): Seq[Department] = {
      if (!department.isImportDepartment) departmentPlusParents(department.parent) ++ Seq(department)
      else Seq(department)
    }

    permissionsDao.getCustomRoleDefinitionsFor(departmentPlusParents(department))
  }

  override def getGlobalGrantedRole(roleDefinition: RoleDefinition): Option[GrantedRole[PermissionsTarget]] =
    roleDefinition match {
      case builtIn: BuiltInRoleDefinition => permissionsDao.getGlobalGrantedRole(builtIn)
      case custom: CustomRoleDefinition => permissionsDao.getGlobalGrantedRole(custom)
    }

  override def getOrCreateGlobalGrantedRole(roleDefinition: RoleDefinition): GrantedRole[PermissionsTarget] =
    getGlobalGrantedRole(roleDefinition).getOrElse {
      require(roleDefinition.isInstanceOf[BuiltInRoleDefinition])

      permissionsDao.createGlobalGrantedRole(roleDefinition.asInstanceOf[BuiltInRoleDefinition])
    }

  override def getAllGlobalGrantedRoles: Seq[GrantedRole[PermissionsTarget]] = permissionsDao.getGlobalGrantedRoles

  override def getGlobalGrantedPermission(permission: Permission, overrideType: Boolean): Option[GrantedPermission[PermissionsTarget]] =
    permissionsDao.getGlobalGrantedPermission(permission, overrideType)

  override def getOrCreateGlobalGrantedPermission(permission: Permission, overrideType: Boolean): GrantedPermission[PermissionsTarget] =
    getGlobalGrantedPermission(permission, overrideType).getOrElse {
      permissionsDao.createGlobalGrantedPermission(permission, overrideType)
    }

  override def getAllGlobalGrantedPermissions: Seq[GrantedPermission[PermissionsTarget]] = permissionsDao.getGlobalGrantedPermissions
}

class GrantedPermissionsByIdCache(dao: PermissionsDao) extends RequestLevelCaching[String, Option[GrantedPermission[_]]] {

  def getGrantedPermissionsByIds[A <: PermissionsTarget : ClassTag](ids: Seq[String]): Seq[GrantedPermission[A]] =
    ids flatMap { id => getGrantedPermissionsById[A](id) }

  def getGrantedPermissionsById[A <: PermissionsTarget : ClassTag](id: String): Option[GrantedPermission[A]] =
    cachedBy(id) {
      dao.getGrantedPermission[A](id)
    }.asInstanceOf[Option[GrantedPermission[A]]]

}

class GrantedRoleByIdCache(dao: PermissionsDao) extends RequestLevelCaching[String, Option[GrantedRole[_]]] {

  def getGrantedRolesByIds[A <: PermissionsTarget : ClassTag](ids: Seq[String]): Seq[GrantedRole[A]] =
    ids flatMap { id => getGrantedRoleById[A](id) }

  def getGrantedRoleById[A <: PermissionsTarget : ClassTag](id: String): Option[GrantedRole[A]] =
    cachedBy(id) {
      dao.getGrantedRole[A](id)
    }.asInstanceOf[Option[GrantedRole[A]]]

}

/*
 * All caches map from a combination of the class tag for the scope, and the user (or webgroup name)
 * and map to a list of IDs of the granted roles / permissions.
 */

trait GrantedRolesForUserCache {
  self: PermissionsDaoComponent with CacheStrategyComponent with UserLookupComponent =>
  final val GrantedRolesForUserCacheName = "GrantedRolesForUser"
  final val GrantedRolesForUserCacheMaxAge: Duration = Duration.ofHours(1)

  final lazy val GrantedRolesForUserCache: Cache[(String, ClassTag[_ <: PermissionsTarget]), JArrayList[String]] =
    Caches.builder(GrantedRolesForUserCacheName, new GrantedRolesForUserCacheFactory, cacheStrategy)
      .expireAfterWrite(GrantedRolesForUserCacheMaxAge)
      .maximumSize(10000) // Ignored by Memcached, just for Caffeine (testing)
      .build()

  class GrantedRolesForUserCacheFactory extends CacheEntryFactory[(String, ClassTag[_ <: PermissionsTarget]), JArrayList[String]] {
    def create(cacheKey: (String, ClassTag[_ <: PermissionsTarget])): JArrayList[String] = cacheKey match {
      case (userId, tag) => JArrayList(permissionsDao.getGrantedRolesForUser(userLookup.getUserByUserId(userId))(tag).map { role => role.id }.asJava)
    }

    def shouldBeCached(ids: JArrayList[String]) = true

    override def isSupportsMultiLookups = false

    def create(cacheKeys: JList[(String, ClassTag[_ <: PermissionsTarget])]): JMap[(String, ClassTag[_ <: PermissionsTarget]), JArrayList[String]] = {
      throw new UnsupportedOperationException("Multi lookups not supported")
    }
  }

}

trait GrantedRolesForGroupCache {
  self: PermissionsDaoComponent with CacheStrategyComponent =>
  final val GrantedRolesForGroupCacheName = "GrantedRolesForGroup"
  final val GrantedRolesForGroupCacheMaxAge: Duration = Duration.ofHours(1)

  final lazy val GrantedRolesForGroupCache: Cache[(Seq[String], ClassTag[_ <: PermissionsTarget]), JArrayList[String]] =
    Caches.builder(GrantedRolesForGroupCacheName, new GrantedRolesForGroupCacheFactory, cacheStrategy)
      .expireAfterWrite(GrantedRolesForGroupCacheMaxAge)
      .maximumSize(10000) // Ignored by Memcached, just for Caffeine (testing)
      .build()

  class GrantedRolesForGroupCacheFactory extends SingularCacheEntryFactory[(Seq[String], ClassTag[_ <: PermissionsTarget]), JArrayList[String]] {
    def create(cacheKey: (Seq[String], ClassTag[_ <: PermissionsTarget])): JArrayList[String] = cacheKey match {
      case (groupNames, tag) => JArrayList(permissionsDao.getGrantedRolesForWebgroups(groupNames)(tag).map { role => role.id }.asJava)
    }

    def shouldBeCached(ids: JArrayList[String]) = true
  }

}

trait GrantedPermissionsForUserCache {
  self: PermissionsDaoComponent with CacheStrategyComponent with UserLookupComponent =>
  final val GrantedPermissionsForUserCacheName = "GrantedPermissionsForUser"
  final val GrantedPermissionsForUserCacheMaxAge: Duration = Duration.ofHours(1)

  final lazy val GrantedPermissionsForUserCache: Cache[(String, ClassTag[_ <: PermissionsTarget]), JArrayList[String]] =
    Caches.builder(GrantedPermissionsForUserCacheName, new GrantedPermissionsForUserCacheFactory, cacheStrategy)
      .expireAfterWrite(GrantedPermissionsForUserCacheMaxAge)
      .maximumSize(10000) // Ignored by Memcached, just for Caffeine (testing)
      .build()

  class GrantedPermissionsForUserCacheFactory extends SingularCacheEntryFactory[(String, ClassTag[_ <: PermissionsTarget]), JArrayList[String]] {
    def create(cacheKey: (String, ClassTag[_ <: PermissionsTarget])): JArrayList[String] = cacheKey match {
      case (userId, tag) => JArrayList(permissionsDao.getGrantedPermissionsForUser(userLookup.getUserByUserId(userId))(tag).map { role => role.id }.asJava)
    }

    def shouldBeCached(ids: JArrayList[String]) = true
  }

}

trait GrantedPermissionsForGroupCache {
  self: PermissionsDaoComponent with CacheStrategyComponent =>
  final val GrantedPermissionsForGroupCacheName = "GrantedPermissionsForGroup"
  final val GrantedPermissionsForGroupCacheMaxAge: Duration = Duration.ofHours(1)

  final lazy val GrantedPermissionsForGroupCache: Cache[(Seq[String], ClassTag[_ <: PermissionsTarget]), JArrayList[String]] =
    Caches.builder(GrantedPermissionsForGroupCacheName, new GrantedPermissionsForGroupCacheFactory, cacheStrategy)
      .expireAfterWrite(GrantedPermissionsForGroupCacheMaxAge)
      .maximumSize(10000) // Ignored by Memcached, just for Caffeine (testing)
      .build()

  class GrantedPermissionsForGroupCacheFactory extends SingularCacheEntryFactory[(Seq[String], ClassTag[_ <: PermissionsTarget]), JArrayList[String]] {
    def create(cacheKey: (Seq[String], ClassTag[_ <: PermissionsTarget])): JArrayList[String] = cacheKey match {
      case (groupNames, tag) => JArrayList(permissionsDao.getGrantedPermissionsForWebgroups(groupNames)(tag).map { role => role.id }.asJava)
    }

    def shouldBeCached(ids: JArrayList[String]) = true
  }

}

trait PermissionsServiceCaches {
  def rolesByIdCache: GrantedRoleByIdCache

  def permissionsByIdCache: GrantedPermissionsByIdCache

  def GrantedRolesForUserCache: Cache[(String, ClassTag[_ <: PermissionsTarget]), JArrayList[String]]

  def GrantedRolesForGroupCache: Cache[(Seq[String], ClassTag[_ <: PermissionsTarget]), JArrayList[String]]

  def GrantedPermissionsForUserCache: Cache[(String, ClassTag[_ <: PermissionsTarget]), JArrayList[String]]

  def GrantedPermissionsForGroupCache: Cache[(Seq[String], ClassTag[_ <: PermissionsTarget]), JArrayList[String]]
}

trait PermissionsServiceCachesImpl extends PermissionsServiceCaches with GrantedRolesForUserCache
  with GrantedRolesForGroupCache with GrantedPermissionsForUserCache with GrantedPermissionsForGroupCache
  with AutowiringCacheStrategyComponent with AutowiringUserLookupComponent {

  this: PermissionsDaoComponent =>
  val rolesByIdCache: GrantedRoleByIdCache = new GrantedRoleByIdCache(permissionsDao)
  val permissionsByIdCache = new GrantedPermissionsByIdCache(permissionsDao)
}

@ItemType("PermissionsCacheBuster")
@JsonAutoDetect
class PermissionsCacheBusterMessage {
  @JsonIgnore
  @BeanProperty var classTag: ClassTag[_ <: PermissionsTarget] = _
  @BeanProperty var usercode: String = _
  @BeanProperty var webgroups: Seq[String] = _
}
