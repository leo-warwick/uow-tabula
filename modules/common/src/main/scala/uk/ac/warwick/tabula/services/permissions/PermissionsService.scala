package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.PermissionsDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.permissions.Permission
import scala.reflect._
import uk.ac.warwick.userlookup.GroupService
import scala.collection.JavaConverters._
import uk.ac.warwick.util.cache.CacheEntryFactory
import uk.ac.warwick.util.cache.Caches
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User

trait PermissionsService {
	def saveOrUpdate(roleDefinition: CustomRoleDefinition)
	def saveOrUpdate(permission: GrantedPermission[_])
	def saveOrUpdate(role: GrantedRole[_])
	
	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, roleDefinition: RoleDefinition): Option[GrantedRole[A]]
	def getGrantedPermission[A <: PermissionsTarget: ClassTag](scope: A, permission: Permission, overrideType: Boolean): Option[GrantedPermission[A]]
	
	def getGrantedRolesFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedRole[_]]
	def getGrantedPermissionsFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedPermission[_]]
	
	def getAllGrantedRolesFor(user: CurrentUser): Seq[GrantedRole[_]]
	def getAllGrantedPermissionsFor(user: CurrentUser): Seq[GrantedPermission[_]]
	
	def getGrantedRolesFor[A <: PermissionsTarget: ClassTag](user: CurrentUser): Seq[GrantedRole[A]]
	def getGrantedPermissionsFor[A <: PermissionsTarget: ClassTag](user: CurrentUser): Seq[GrantedPermission[A]]
	
	def getAllPermissionDefinitionsFor[A <: PermissionsTarget: ClassTag](user: CurrentUser, targetPermission: Permission): Set[A]
	
	def ensureUserGroupFor[A <: PermissionsTarget: ClassTag](scope: A, roleDefinition: RoleDefinition): UserGroup
}

@Service(value = "permissionsService")
class PermissionsServiceImpl extends PermissionsService with Logging
	with GrantedRolesForUserCache
	with GrantedRolesForGroupCache
	with GrantedPermissionsForUserCache
	with GrantedPermissionsForGroupCache {
	
	var dao = Wire[PermissionsDao]
	var groupService = Wire[GroupService]
	
	private def clearCaches() {
		// This is monumentally dumb. There's a more efficient way than this!
		GrantedRolesForUserCache.clear()
		GrantedRolesForGroupCache.clear()
		GrantedPermissionsForUserCache.clear()
		GrantedPermissionsForGroupCache.clear()
	}
	
	def saveOrUpdate(roleDefinition: CustomRoleDefinition) = dao.saveOrUpdate(roleDefinition)
	def saveOrUpdate(permission: GrantedPermission[_]) = {
		dao.saveOrUpdate(permission)
		clearCaches()
	}
	def saveOrUpdate(role: GrantedRole[_]) = {
		dao.saveOrUpdate(role)
		clearCaches()
	}
	
	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, roleDefinition: RoleDefinition): Option[GrantedRole[A]] = 
		transactional(readOnly = true) {
			roleDefinition match {
				case builtIn: BuiltInRoleDefinition => dao.getGrantedRole(scope, builtIn)
				case custom: CustomRoleDefinition => dao.getGrantedRole(scope, custom)
				case _ => None
			}
		}
	
	def getGrantedPermission[A <: PermissionsTarget: ClassTag](scope: A, permission: Permission, overrideType: Boolean): Option[GrantedPermission[A]] =
		transactional(readOnly = true) {
			dao.getGrantedPermission(scope, permission, overrideType)
		}
	
	def getGrantedRolesFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedRole[_]] = transactional(readOnly = true) {
		dao.getGrantedRolesFor(scope) filter { _.users.includes(user.apparentId) }
	}
	
	def getGrantedPermissionsFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedPermission[_]] = transactional(readOnly = true) {
		dao.getGrantedPermissionsFor(scope).toStream filter { _.users.includes(user.apparentId) }
	}
	
	def getAllGrantedRolesFor(user: CurrentUser): Seq[GrantedRole[_]] = getGrantedRolesFor[PermissionsTarget](user)
	
	def getAllGrantedPermissionsFor(user: CurrentUser): Seq[GrantedPermission[_]] = getGrantedPermissionsFor[PermissionsTarget](user)
	
	def getGrantedRolesFor[A <: PermissionsTarget: ClassTag](user: CurrentUser): Seq[GrantedRole[A]] = transactional(readOnly = true) {
		val groupNames = groupService.getGroupsNamesForUser(user.apparentId).asScala
		
		dao.getGrantedRolesById(
			// Get all roles where usercode is included,
			GrantedRolesForUserCache.get((user.apparentUser, classTag[A])).asScala
			
			// Get all roles backed by one of the webgroups, 		
			++ (groupNames flatMap { groupName => GrantedRolesForGroupCache.get((groupName, classTag[A])).asScala })
		)
			// For sanity's sake, filter by the users including the user
			.filter { _.users.includes(user.apparentId) }
	}
	
	def getGrantedPermissionsFor[A <: PermissionsTarget: ClassTag](user: CurrentUser): Seq[GrantedPermission[A]] = transactional(readOnly = true) {
		val groupNames = groupService.getGroupsNamesForUser(user.apparentId).asScala
		
		dao.getGrantedPermissionsById(
			// Get all permissions where usercode is included,
			GrantedPermissionsForUserCache.get((user.apparentUser, classTag[A])).asScala
			
			// Get all permissions backed by one of the webgroups, 		
			++ (groupNames flatMap { groupName => GrantedPermissionsForGroupCache.get((groupName, classTag[A])).asScala })
		)
			// For sanity's sake, filter by the users including the user
			.filter { _.users.includes(user.apparentId) }
	}
	
	def getAllPermissionDefinitionsFor[A <: PermissionsTarget: ClassTag](user: CurrentUser, targetPermission: Permission): Set[A] = {
		val scopesWithGrantedRole = 
			getGrantedRolesFor[A](user)
			.filter { _.mayGrant(targetPermission) }
			.map { _.scope }
			
		val scopesWithGrantedPermission =
			getGrantedPermissionsFor[A](user)
			.filter { perm => perm.overrideType == GrantedPermission.Allow && perm.permission == targetPermission }
			.map { _.scope }
			
		Set() ++ scopesWithGrantedRole ++ scopesWithGrantedPermission
	}
	
	def ensureUserGroupFor[A <: PermissionsTarget: ClassTag](scope: A, roleDefinition: RoleDefinition): UserGroup = transactional() {
		getGrantedRole(scope, roleDefinition) match {
			case Some(role) => role.users
			case _ => {
				val role = GrantedRole(scope, roleDefinition)
				
				dao.saveOrUpdate(role)
				role.users
			}
		}
	}
	
}

/*
 * All caches map from a combination of the class tag for the scope, and the user (or webgroup name) 
 * and map to a list of IDs of the granted roles / permissions.
 */ 

trait GrantedRolesForUserCache { self: PermissionsServiceImpl =>
	final val GrantedRolesForUserCacheName = "GrantedRolesForUser"
	final val GrantedRolesForUserCacheMaxAgeSecs = 60 * 60 // 1 hour
	final val GrantedRolesForUserCacheMaxSize = 1000
	
	final val GrantedRolesForUserCache = 
		Caches.newCache(GrantedRolesForUserCacheName, new GrantedRolesForUserCacheFactory, GrantedRolesForUserCacheMaxAgeSecs)
	GrantedRolesForUserCache.setMaxSize(GrantedRolesForUserCacheMaxSize)
	
	class GrantedRolesForUserCacheFactory extends CacheEntryFactory[(User, ClassTag[_ <: PermissionsTarget]), JArrayList[String]] {
		def create(cacheKey: (User, ClassTag[_ <: PermissionsTarget])) = cacheKey match {
			case (user, tag) => JArrayList(dao.getGrantedRolesForUser(user)(tag).map { role => role.id }.asJava)
		}
		def shouldBeCached(ids: JArrayList[String]) = true
		
		override def isSupportsMultiLookups() = false
		def create(cacheKeys: JList[(User, ClassTag[_ <: PermissionsTarget])]): JMap[(User, ClassTag[_ <: PermissionsTarget]), JArrayList[String]] = {
			throw new UnsupportedOperationException("Multi lookups not supported")
		}
	}
}

trait GrantedRolesForGroupCache { self: PermissionsServiceImpl =>
	final val GrantedRolesForGroupCacheName = "GrantedRolesForGroup"
	final val GrantedRolesForGroupCacheMaxAgeSecs = 60 * 60 // 1 hour
	final val GrantedRolesForGroupCacheMaxSize = 1000
	
	final val GrantedRolesForGroupCache = 
		Caches.newCache(GrantedRolesForGroupCacheName, new GrantedRolesForGroupCacheFactory, GrantedRolesForGroupCacheMaxAgeSecs)
	GrantedRolesForGroupCache.setMaxSize(GrantedRolesForGroupCacheMaxSize)
	
	class GrantedRolesForGroupCacheFactory extends CacheEntryFactory[(String, ClassTag[_ <: PermissionsTarget]), JArrayList[String]] {
		def create(cacheKey: (String, ClassTag[_ <: PermissionsTarget])) = cacheKey match {
			case (groupName, tag) => JArrayList(dao.getGrantedRolesForWebgroup(groupName)(tag).map { role => role.id }.asJava)
		}
		def shouldBeCached(ids: JArrayList[String]) = true
		
		override def isSupportsMultiLookups() = false
		def create(cacheKeys: JList[(String, ClassTag[_ <: PermissionsTarget])]): JMap[(String, ClassTag[_ <: PermissionsTarget]), JArrayList[String]] = {
			throw new UnsupportedOperationException("Multi lookups not supported")
		}
	}
}

trait GrantedPermissionsForUserCache { self: PermissionsServiceImpl =>
	final val GrantedPermissionsForUserCacheName = "GrantedPermissionsForUser"
	final val GrantedPermissionsForUserCacheMaxAgeSecs = 60 * 60 // 1 hour
	final val GrantedPermissionsForUserCacheMaxSize = 1000
	
	final val GrantedPermissionsForUserCache = 
		Caches.newCache(GrantedPermissionsForUserCacheName, new GrantedPermissionsForUserCacheFactory, GrantedPermissionsForUserCacheMaxAgeSecs)
	GrantedPermissionsForUserCache.setMaxSize(GrantedPermissionsForUserCacheMaxSize)
	
	class GrantedPermissionsForUserCacheFactory extends CacheEntryFactory[(User, ClassTag[_ <: PermissionsTarget]), JArrayList[String]] {
		def create(cacheKey: (User, ClassTag[_ <: PermissionsTarget])) = cacheKey match {
			case (user, tag) => JArrayList(dao.getGrantedPermissionsForUser(user)(tag).map { role => role.id }.asJava)
		}
		def shouldBeCached(ids: JArrayList[String]) = true
		
		override def isSupportsMultiLookups() = false
		def create(cacheKeys: JList[(User, ClassTag[_ <: PermissionsTarget])]): JMap[(User, ClassTag[_ <: PermissionsTarget]), JArrayList[String]] = {
			throw new UnsupportedOperationException("Multi lookups not supported")
		}
	}
}

trait GrantedPermissionsForGroupCache { self: PermissionsServiceImpl =>
	final val GrantedPermissionsForGroupCacheName = "GrantedPermissionsForGroup"
	final val GrantedPermissionsForGroupCacheMaxAgeSecs = 60 * 60 // 1 hour
	final val GrantedPermissionsForGroupCacheMaxSize = 1000
	
	final val GrantedPermissionsForGroupCache = 
		Caches.newCache(GrantedPermissionsForGroupCacheName, new GrantedPermissionsForGroupCacheFactory, GrantedPermissionsForGroupCacheMaxAgeSecs)
	GrantedPermissionsForGroupCache.setMaxSize(GrantedPermissionsForGroupCacheMaxSize)
	
	class GrantedPermissionsForGroupCacheFactory extends CacheEntryFactory[(String, ClassTag[_ <: PermissionsTarget]), JArrayList[String]] {
		def create(cacheKey: (String, ClassTag[_ <: PermissionsTarget])) = cacheKey match {
			case (groupName, tag) => JArrayList(dao.getGrantedPermissionsForWebgroup(groupName)(tag).map { role => role.id }.asJava)
		}
		def shouldBeCached(ids: JArrayList[String]) = true
		
		override def isSupportsMultiLookups() = false
		def create(cacheKeys: JList[(String, ClassTag[_ <: PermissionsTarget])]): JMap[(String, ClassTag[_ <: PermissionsTarget]), JArrayList[String]] = {
			throw new UnsupportedOperationException("Multi lookups not supported")
		}
	}
}