package uk.ac.warwick.tabula.data

import org.hibernate.criterion.Projections
import org.springframework.stereotype.Repository

import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

trait UserGroupDaoComponent {
	val userGroupDao: UserGroupDao
}

trait AutowiringUserGroupDaoComponent extends UserGroupDaoComponent {
	val userGroupDao = Wire[UserGroupDao]
}

trait UserGroupDao {
	def saveOrUpdate(userGroup: UserGroup)
	def getUserGroupsByIds(ids: Seq[String]): Seq[UserGroup]
	def getUserGroupIds(user: User, runtimeClass: Class[_], path: String, checkUniversityIds: Boolean = true): Seq[String]
}

abstract class AbstractUserGroupDao extends UserGroupDao with HelperRestrictions {
	self: SessionComponent =>

	def saveOrUpdate(userGroup: UserGroup) = session.saveOrUpdate(userGroup)
	def getUserGroupsByIds(ids: Seq[String]): Seq[UserGroup] =
		safeInSeq(() => { session.newCriteria[UserGroup] }, "id", ids)
}

@Repository
class UserGroupDaoImpl extends AbstractUserGroupDao with Daoisms
	with UserGroupMembershipHelperLookup
	with AutowiringUserLookupComponent

trait UserGroupMembershipHelperLookup {
	self: UserGroupDao with SessionComponent with HelperRestrictions with UserLookupComponent =>

	// A series of confusing variables for building joined queries across paths split by.dots
	private def pathParts(path: String) = {
		val parts = path.split("\\.").toList.reverse

		if (parts.size > 2) throw new IllegalArgumentException("Only allowed one or two parts to the path")

		parts
	}

	private def groupsByUserSql(runtimeClass: Class[_], path: String, checkUniversityIds: Boolean) = {
		val simpleEntityName = runtimeClass.getSimpleName
		val parts = pathParts(path)

		// The actual name of the UserGroup
		val usergroupName = parts.head

		// A possible table to join through to get to userProp
		val joinTable: Option[String] = parts.tail.headOption

		// The overall property name, possibly including the joinTable
		val prop: String = joinTable.fold("")(_ + ".") + usergroupName

		val leftJoin = joinTable.fold("")( table => s"left join r.$table as $table" )

		// skip the university IDs check if we know we only ever use usercodes
		val universityIdsClause =
			if (checkUniversityIds) s""" or (
					$prop.universityIds = true and
					((:universityId in (select memberId from usergroupstatic where userGroup = r.$prop)
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
					((:userId in (select memberId from usergroupstatic where userGroup = r.$prop)
					or :userId in elements($prop.includeUsers))
					and :userId not in elements($prop.excludeUsers))
				) $universityIdsClause
		"""
	}

	protected def getWebgroups(usercode: String): Seq[String] = usercode.maybeText.map {
		usercode => userLookup.getGroupService.getGroupsNamesForUser(usercode).asScala
	}.getOrElse(Nil)

	override def getUserGroupIds(user: User, runtimeClass: Class[_], path: String, checkUniversityIds: Boolean = true): Seq[String] = {
		val parts = pathParts(path)

		// The actual name of the UserGroup
		val usergroupName = parts.head

		// A possible table to join through to get to userProp
		val joinTable: Option[String] = parts.tail.headOption

		// The overall property name, possibly including the joinTable
		val prop: String = joinTable.fold("")(_ + ".") + usergroupName

		val groupsByUser = session.newQuery[String](groupsByUserSql(runtimeClass, path, checkUniversityIds))
			.setString("universityId", user.getWarwickId)
			.setString("userId", user.getUserId)
			.seq

		val webgroupNames: Seq[String] = getWebgroups(user.getUserId)
		val groupsByWebgroup =
			if (webgroupNames.isEmpty) Nil
			else {
				val criteria = session.newCriteria(runtimeClass)

				joinTable.foreach { table =>
					criteria.createAlias(table, table)
				}

				criteria
					.createAlias(prop, "usergroupAlias")
					.add(safeIn("usergroupAlias.baseWebgroup", webgroupNames))
					.project[String](Projections.id())
					.seq
			}

		(groupsByUser ++ groupsByWebgroup).distinct
	}
}