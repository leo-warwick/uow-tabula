package uk.ac.warwick.tabula

import scala.collection.JavaConverters._
import org.apache.commons.lang3.StringUtils._
import uk.ac.warwick.tabula.services.{LenientGroupService, UserByWarwickIdCache, UserLookupService}
import uk.ac.warwick.userlookup._
import uk.ac.warwick.userlookup.webgroups.GroupInfo
import uk.ac.warwick.userlookup.webgroups.GroupNotFoundException
import uk.ac.warwick.tabula.JavaImports._
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.codec.binary.Base64
import uk.ac.warwick.tabula.UserFlavour.{Anonymous, Applicant, Unverified, Vanilla}
import uk.ac.warwick.tabula.services.UserLookupService.UniversityId
import uk.ac.warwick.tabula.services.permissions.CacheStrategyComponent
import uk.ac.warwick.util.cache.Caches.CacheStrategy

class MockUserLookup(var defaultFoundUser: Boolean)
	extends UserLookupAdapter(null)
		 with UserLookupService
		 with JavaImports
		 with MockUser {

	def this() = {
		this(false)
	}

	var users: Map[String, User] = Map()
	var filterUserResult: JList[User] = JArrayList()

	var findUsersEnabled: Boolean = false

	val groupService: MockGroupService = new MockGroupService

	override def getGroupService = new LenientGroupService(groupService)

	def addFindUsersWithFilterResult(user: User) {
		filterUserResult.add(user)
	}

	override def getUserByUserId(theUserId: String): User = {
		users.getOrElse(theUserId, {
			if (defaultFoundUser) {
				val user = new User()
				user.setUserId(theUserId)
				user.setFoundUser(true)
				user
			} else {
				new AnonymousUser
			}
		})
	}

	override def getUserByWarwickUniId(warwickId: String): User =
		users.values.find(_.getWarwickId == warwickId).getOrElse {
			if (defaultFoundUser) {
				val user = new User()
				user.setWarwickId(warwickId)
				user.setFoundUser(true)
				user
			} else {
				new AnonymousUser
			}
		}

	override def getUserByWarwickUniIdUncached(warwickId: String, skipMemberLookup: Boolean): User = getUserByWarwickUniId(warwickId)

	override def getUserByWarwickUniId(warwickId: String, includeDisabledLogins: Boolean): User = getUserByWarwickUniId(warwickId)

	override def getUsersByWarwickUniIds(warwickIds: Seq[String]): Map[UniversityId, User] = warwickIds.map { id => id -> getUserByWarwickUniId(id) }.toMap

	override def getUsersByWarwickUniIdsUncached(warwickIds: Seq[String], skipMemberLookup: Boolean): Map[String, User] =
		getUsersByWarwickUniIds(warwickIds)

	override def findUsersWithFilter(filterValues: JMap[String, String]): JList[User] = {
		if (filterValues.size() == 1 && filterValues.containsKey("warwickuniid")) {
			JArrayList(getUserByWarwickUniId(filterValues.get("warwickuniid")))
		} else if (findUsersEnabled) {
			val list: JList[User] = JArrayList()
			list.addAll(filterUserResult)
			list
		} else {
			throw new UnsupportedOperationException()
		}
	}

	override def findUsersWithFilter(filterValues: JMap[String, String], returnDisabledUsers: Boolean): JList[User] = findUsersWithFilter(filterValues)

	override def getUsersByUserIds(userIds: JList[String]): JMap[String, User] = {
		val map: Map[String, User] = userIds.asScala.map { id => id -> getUserByUserId(id)}.toMap

		map.asJava
	}

	/**
	 * Method to quickly add some mock users who exist and definitely
	 * have genuine, real names.
	 */
	def registerUsers(userIds: String*): Seq[User] = {
		userIds.map { id =>
			val user = mockUser(id)
			users += (id -> user)
			user
		}
	}

	def registerUserObjects(u: User*) {
		for (user <- u) {
			users += (user.getUserId -> user)
		}
	}

}


class MockCachingLookupService(var flavour: UserFlavour = Vanilla)
	extends UserLookupAdapter(null)
	with UserLookupService
	with UserByWarwickIdCache
	with CacheStrategyComponent
	with MockUser {

	val cacheStrategy = CacheStrategy.InMemoryOnly

	override def getUserByWarwickUniIdUncached(warwickId: String, skipMemberLookup: Boolean): User = {
		flavour match {
			case Unverified => new UnverifiedUser(new UserLookupException)
			case Anonymous => new AnonymousUser()
			case Applicant =>
				val user = new AnonymousUser()
				user.setWarwickId(warwickId)
				user.setUserId(warwickId)
				user
			case Vanilla =>
				// hash WarwickId to a consistent 6 char 'usercode'. Tiny, but finite risk of collision/short usercodes.
				val userId = Base64.encodeBase64String(DigestUtils.sha256(warwickId.getBytes)).filter(_.isLower).take(6)
				val user = mockUser(userId)
				user.setWarwickId(warwickId)
				user.setUserId(warwickId)
				user
		}
	}

	override def getUserByWarwickUniId(id: String): User = getUserByWarwickUniId(id, ignored = true)

	override def getUserByWarwickUniId(id: String, ignored: Boolean): User = UserByWarwickIdCache.get(id)

	override def getUsersByWarwickUniIds(warwickIds: Seq[String]): Map[UniversityId, User] = UserByWarwickIdCache.get(warwickIds.asJava).asScala.toMap

	override def getUsersByWarwickUniIdsUncached(warwickIds: Seq[String], skipMemberLookup: Boolean): Map[String, User] =
		warwickIds.map { id => id -> getUserByWarwickUniIdUncached(id, skipMemberLookup) }.toMap

	override def getGroupService = new LenientGroupService(super.getGroupService)
}


trait MockUser {
	def mockUser(id: String): User = {
		val user = new User(id)
		user.setFoundUser(true)
		user.setVerified(true)
		user.setFirstName(capitalize(id)+"y")
		user.setLastName("Mc"+capitalize(id)+"erson")
		user.setDepartment("Department of " + capitalize(id))
		user.setShortDepartment("Dept of " + capitalize(id))
		user.setDepartmentCode("D" + capitalize(id.substring(0, 1)))
		user.setEmail(id + "@example.com")
		user.setWarwickId({
			val universityId = Math.abs(id.hashCode()).toString
			if (universityId.length > 7) universityId.substring(universityId.length - 7)
			else UniversityId.zeroPad(universityId)
		})
		user
	}
}

/* Vanilla is a verified user, whose session was completely retrieved from SSO.

 * Anonymous means user is *not* logged into SSO, but *is*
 * implicitly verified - as anonymous - with default properties set,
 * including warwickId of empty string.
 *
 * Unverified means a UserLookupException has been raised, which should
 * return an UnverifiedUser, a subclass of AnonymousUser;
 * most properties should have default properties set too.
 *
 * Applicant is another special case of AnonymousUser, this time with
 * a real warwickID set.
 */
sealed abstract class UserFlavour
object UserFlavour {
	case object Anonymous extends UserFlavour
	case object Unverified extends UserFlavour
	case object Applicant extends UserFlavour
	case object Vanilla extends UserFlavour
}


class MockGroupService extends GroupService {
	var groupMap: Map[String, Group] = Map()
	var usersInGroup: Map[(String, String), Boolean] = Map() withDefaultValue false
	var groupNamesForUserMap: Map[String, Seq[String]] = Map()

	override def getGroupByName(groupName: String): Group = groupMap.getOrElse(groupName, throw new GroupNotFoundException(groupName))

	override def getGroupsForDeptCode(deptCode: String) = ???

	override def getGroupsForQuery(query: String) = ???

	override def getGroupsForUser(user: String) = ???

	override def getGroupsNamesForUser(user: String): JList[String] =
		groupNamesForUserMap.get(user) match {
			case Some(groups) => groups.asJava
			case _ => JArrayList[String]()
		}

	override def getRelatedGroups(groupName: String) = ???

	override def getUserCodesInGroup(groupName: String): _root_.uk.ac.warwick.tabula.JavaImports.JList[String] = {
		try {
			getGroupByName(groupName).getUserCodes
		} catch {
			case e: GroupNotFoundException => JList()
		}
	}

	override def isUserInGroup(user: String, group: String): Boolean = usersInGroup((user, group))

	override def setTimeoutConfig(config: WebServiceTimeoutConfig) {}

	override def getGroupInfo(name: String) = new GroupInfo(getGroupByName(name).getUserCodes.size())

	override def clearCaches() = ???
	override def getCaches() = ???

}