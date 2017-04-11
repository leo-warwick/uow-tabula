package uk.ac.warwick.tabula.services

import java.io.Serializable

import uk.ac.warwick.userlookup.webgroups.{GroupInfo, GroupNotFoundException, GroupServiceException}

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup._
import uk.ac.warwick.util.cache._
import uk.ac.warwick.tabula.helpers.StringUtils._
import javax.annotation.PreDestroy

import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.sso.client.core.OnCampusService
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.services.UserLookupService._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.permissions.{AutowiringCacheStrategyComponent, CacheStrategyComponent}

import scala.util.{Failure, Success, Try}

object UserLookupService {
	type UniversityId = String
	type Usercode = String
}

trait UserLookupComponent {
	def userLookup: UserLookupService
}

trait AutowiringUserLookupComponent extends UserLookupComponent {
	@transient var userLookup: UserLookupService = Wire[UserLookupService]
}

trait UserLookupService extends UserLookupInterface {
	override def getGroupService: LenientGroupService

	def getUserByWarwickUniIdUncached(id: UniversityId, skipMemberLookup: Boolean): User

	/**
	 * Takes a List of universityIds, and returns a Map that maps universityIds to Users. Users found
	 * in the local cache will be taken from there (and not searched for), and all other
	 * users will be searched for and entered into the cache.
	 *
	 * All universityIds will be returned in the Map, but ones that weren't found will map to
	 * AnonymousUser objects.
	 *
	 * @param ids Seq[UniversityId]
	 * @return Map[UniversityId, User]
	 */
	def getUsersByWarwickUniIds(ids: Seq[UniversityId]): Map[UniversityId, User]
	def getUsersByWarwickUniIdsUncached(ids: Seq[UniversityId], skipMemberLookup: Boolean): Map[UniversityId, User]

	def getUsersByUserIds(ids: Seq[String]): Map[String, User] = getUsersByUserIds(ids.asJava).asScala.toMap
}

class UserLookupServiceImpl(d: UserLookupInterface) extends UserLookupAdapter(d) with UserLookupService
	with UserByWarwickIdCache with AutowiringCacheStrategyComponent with Logging {

	var profileService: ProfileService = Wire[ProfileService]

	override def getGroupService: LenientGroupService = new LenientGroupService(super.getGroupService)

	override def getUserByUserId(id: String): User = super.getUserByUserId(id) match {
		case anon: AnonymousUser =>
			anon.setUserId(id)
			anon
		case user => filterApplicantUsers(user)
	}

	override def getUserByWarwickUniId(id: UniversityId): User =
		getUserByWarwickUniId(id, ignored=true)

	override def getUserByWarwickUniId(id: UniversityId, ignored: Boolean): User =
		try {
			id.maybeText.map(UserByWarwickIdCache.get).getOrElse(new AnonymousUser)
		} catch {
			case e: CacheEntryUpdateException =>
				logger.error("Error fetching user by warwick ID", e)
				new AnonymousUser
		}

	override def getUsersByWarwickUniIds(ids: Seq[UniversityId]): Map[UniversityId, User] =
		try {
			UserByWarwickIdCache.get(ids.filter(_.hasText).asJava).asScala.toMap
		} catch {
			case e: CacheEntryUpdateException =>
				logger.error("Error fetching users by warwick ID", e)
				Map()
		}

	private def getUserByWarwickUniIdFromUserLookup(id: UniversityId) = {
		/*
		 * TAB-2004 We go directly to the UserLookup filter method in order to change the behaviour. In particular,
		 * we want to prefer loginDisabled=FALSE over ones whose logins are disabled.
		 */
		val filter = Map("warwickuniid" -> id)
		findUsersWithFilter(filter.asJava, true)
			.asScala
			.map { user => getUserByUserId(user.getUserId) }
			.filter { user => user.getExtraProperty("urn:websignon:usertype") != "Applicant" }
			.sortBy(user => (user.isLoginDisabled, !user.getEmail.hasText))
			.headOption
			.getOrElse {
				logger.debug("No user found that matches Warwick Uni Id:" + id)
				new AnonymousUser
			}
	}

	def getUserByWarwickUniIdUncached(id: UniversityId, skipMemberLookup: Boolean): User = {
		if (skipMemberLookup) getUserByWarwickUniIdFromUserLookup(id)
		else profileService.getMemberByUniversityIdStaleOrFresh(id)
			.map { _.asSsoUser }
			.getOrElse { getUserByWarwickUniIdFromUserLookup(id) }
	}

	def getUsersByWarwickUniIdsUncached(ids: Seq[UniversityId], skipMemberLookup: Boolean): Map[UniversityId, User] = {
		val dbUsers =
			if (skipMemberLookup) Map.empty
			else profileService.getAllMembersWithUniversityIdsStaleOrFresh(ids).map { m => m.universityId -> m.asSsoUser }.toMap

		val others = ids.diff(dbUsers.keys.toSeq).par.map { id =>
			id -> getUserByWarwickUniIdFromUserLookup(id)
		}.toMap

		dbUsers ++ others
	}

	private def filterApplicantUsers(user: User) = user.getExtraProperty("urn:websignon:usertype") match {
		case "Applicant" => {
			val result = new AnonymousUser()
			result.setUserId(user.getUserId)
			result.setWarwickId(user.getWarwickId)
			result
		}
		case _ => user
	}

}

trait UserByWarwickIdCache extends CacheEntryFactory[UniversityId, User] { self: UserLookupAdapter with CacheStrategyComponent =>
	final val UserByWarwickIdCacheName = "UserByWarwickIdCache"
	final val UserByWarwickIdCacheMaxAgeSecs: Int = 60 * 60 * 24 // 1 day

	final lazy val UserByWarwickIdCache: Cache[UniversityId, User] = {
		val cache = Caches.newCache(UserByWarwickIdCacheName, this, UserByWarwickIdCacheMaxAgeSecs, cacheStrategy)
		cache.setAsynchronousUpdateEnabled(true)
		cache
	}

	def getUserByWarwickUniIdUncached(id: UniversityId, skipMemberLookup: Boolean): User
	def getUsersByWarwickUniIdsUncached(ids: Seq[UniversityId], skipMemberLookup: Boolean): Map[UniversityId, User]

	def create(warwickId: UniversityId): User = {
		try {
			getUserByWarwickUniIdUncached(warwickId, false)
		} catch {
			case e: Exception => throw new CacheEntryUpdateException(e)
		}
	}

	def shouldBeCached(user: User): Boolean = user.isVerified && user.isFoundUser // TAB-1734 don't cache not found users

	def create(warwickIds: JList[UniversityId]): JMap[UniversityId, User] = {
		try {
			getUsersByWarwickUniIdsUncached(warwickIds.asScala, false).asJava
		} catch {
			case e: Exception => throw new CacheEntryUpdateException(e)
		}
	}
	override def isSupportsMultiLookups() = true

	@PreDestroy
	def shutdownCache() {
		try {
			UserByWarwickIdCache.shutdown()
		} catch {
			case _: Throwable =>
		}
	}
}

class SandboxUserLookup(d: UserLookupInterface) extends UserLookupAdapter(d) {
	var profileService: ProfileService = Wire[ProfileService]

	private def sandboxUser(member: Member) = {
		val ssoUser = new User(member.userId)
		ssoUser.setFoundUser(true)
		ssoUser.setVerified(true)
		ssoUser.setDepartment(member.homeDepartment.name)
		ssoUser.setDepartmentCode(member.homeDepartment.code)
		ssoUser.setEmail(member.email)
		ssoUser.setFirstName(member.firstName)
		ssoUser.setLastName(member.lastName)

		member.userType match {
			case MemberUserType.Student => ssoUser.setStudent(true)
			case _ => ssoUser.setStaff(true)
		}

		ssoUser.setWarwickId(member.universityId)

		ssoUser
	}

	override def getUsersInDepartment(d: String): JList[User] =
		SandboxData.Departments.find { case (code, department) => department.name == d } match {
			case Some((code, department)) => getUsersInDepartmentCode(code)
			case _ => super.getUsersInDepartment(d)
		}

	override def getUsersInDepartmentCode(c: String): JList[User] =
		SandboxData.Departments.get(c) match {
			case Some(department) => {
				val students = department.routes.values.flatMap { route =>
					(route.studentsStartId to route.studentsEndId).flatMap { uniId =>
						profileService.getMemberByUniversityId(uniId.toString) map { sandboxUser(_) }
					}
				}

				val staff = (department.staffStartId to department.staffEndId).flatMap { uniId =>
					profileService.getMemberByUniversityId(uniId.toString) map { sandboxUser(_) }
				}

				(students ++ staff).toSeq.asJava
			}
			case _ => super.getUsersInDepartmentCode(c)
		}

	override def getUsersByUserIds(ids: JList[String]): JMap[String, User] =
		ids.asScala.map { userId => (userId, getUserByUserId(userId)) }.toMap.asJava

	override def getUserByUserId(id: String): User =
		profileService.getAllMembersWithUserId(id, true).headOption.map { sandboxUser(_) }.getOrElse { super.getUserByUserId(id) }

	override def getUserByWarwickUniId(id: String): User =
		profileService.getMemberByUniversityId(id).map { sandboxUser(_) }.getOrElse { super.getUserByUserId(id) }

	override def getUserByWarwickUniId(id: String, ignored: Boolean): User = getUserByWarwickUniId(id)

}

class SwappableUserLookupService(d: UserLookupService) extends UserLookupServiceAdapter(d)

abstract class UserLookupServiceAdapter(var delegate: UserLookupService) extends UserLookupService {

	override def getUsersInDepartment(d: String): JList[User] = delegate.getUsersInDepartment(d)
	override def getUsersInDepartmentCode(c: String): JList[User] = delegate.getUsersInDepartmentCode(c)
	override def getUserByToken(t: String): User = delegate.getUserByToken(t)
	override def getUsersByUserIds(ids: JList[String]): JMap[UniversityId, User] = delegate.getUsersByUserIds(ids)
	override def getUserByWarwickUniId(id: UniversityId): User = delegate.getUserByWarwickUniId(id)
	override def getUserByWarwickUniId(id: UniversityId, ignored: Boolean): User = delegate.getUserByWarwickUniId(id, ignored)
	override def getUserByWarwickUniIdUncached(id: UniversityId, skipMemberLookup: Boolean): User = delegate.getUserByWarwickUniIdUncached(id, skipMemberLookup)
	override def getUsersByWarwickUniIds(ids: Seq[UniversityId]): Map[UniversityId, User] = delegate.getUsersByWarwickUniIds(ids)
	override def getUsersByWarwickUniIdsUncached(ids: Seq[UniversityId], skipMemberLookup: Boolean): Map[UniversityId, User] = delegate.getUsersByWarwickUniIdsUncached(ids, skipMemberLookup)
	override def findUsersWithFilter(map: JMap[String, String]): JList[User] = delegate.findUsersWithFilter(map)
	override def findUsersWithFilter(map: JMap[String, String], includeInactive: Boolean): JList[User] = delegate.findUsersWithFilter(map, includeInactive)
	override def getGroupService: LenientGroupService = delegate.getGroupService
	override def getOnCampusService: OnCampusService = delegate.getOnCampusService
	override def getUserByUserId(id: String): User = delegate.getUserByUserId(id)
	override def getCaches: JMap[UniversityId, JSet[Cache[_ <: Serializable, _ <: Serializable]]] = delegate.getCaches
	override def clearCaches(): Unit = delegate.clearCaches()
	override def getUserByIdAndPassNonLoggingIn(u: String, p: String): User = delegate.getUserByIdAndPassNonLoggingIn(u, p)
	override def requestClearWebGroup(webgroup: String): Unit = delegate.requestClearWebGroup(webgroup)

}

class LenientGroupService(delegate: GroupService) extends GroupService with Logging {
	private def tryOrElse[A](r: => A, default: => A): A =
		Try(r) match {
			case Success(any) => any
			case Failure(e: GroupServiceException) =>
				logger.warn("Caught GroupService error", e)
				default
			case Failure(t) => throw t
		}

	def isUserInGroup(userId: String, group: String): Boolean = tryOrElse(delegate.isUserInGroup(userId, group), false)

	def getGroupInfo(name: String): GroupInfo = tryOrElse(delegate.getGroupInfo(name), throw new GroupNotFoundException(name))
	def getGroupByName(name: String): Group = tryOrElse(delegate.getGroupByName(name), throw new GroupNotFoundException(name))

	def getGroupsNamesForUser(userId: String): JList[String] = tryOrElse(delegate.getGroupsNamesForUser(userId), JArrayList())
	def getGroupsForUser(userId: String): JList[Group] = tryOrElse(delegate.getGroupsForUser(userId), JArrayList())
	def getUserCodesInGroup(group: String): JList[String] = tryOrElse(delegate.getUserCodesInGroup(group), JArrayList())
	def getGroupsForQuery(search: String): JList[Group] = tryOrElse(delegate.getGroupsForQuery(search), JArrayList())
	def getRelatedGroups(group: String): JList[Group] = tryOrElse(delegate.getRelatedGroups(group), JArrayList())
	def getGroupsForDeptCode(deptCode: String): JList[Group] = tryOrElse(delegate.getGroupsForDeptCode(deptCode), JArrayList())

	def getCaches: JMap[String, JSet[Cache[_, _]]] = delegate.getCaches
	def clearCaches(): Unit = delegate.clearCaches()
	def setTimeoutConfig(config: WebServiceTimeoutConfig): Unit = delegate.setTimeoutConfig(config)
}