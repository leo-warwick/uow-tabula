package uk.ac.warwick.tabula.data

import org.hibernate.criterion._
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions._
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget}
import uk.ac.warwick.tabula.roles.{BuiltInRoleDefinition, RoleDefinition}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

trait PermissionsDao {
	def saveOrUpdate(roleDefinition: CustomRoleDefinition)
	def saveOrUpdate(permission: GrantedPermission[_])
	def saveOrUpdate(role: GrantedRole[_])

	def delete(roleDefinition: CustomRoleDefinition)

	def getCustomRoleDefinitionById(id: String): Option[CustomRoleDefinition]

	def getGrantedRole[A <: PermissionsTarget: ClassTag](id: String): Option[GrantedRole[A]]
	def getGrantedPermission[A <: PermissionsTarget: ClassTag](id: String): Option[GrantedPermission[A]]

	def getGrantedRolesById[A <: PermissionsTarget: ClassTag](ids: Seq[String]): Seq[GrantedRole[A]]
	def getGrantedPermissionsById[A <: PermissionsTarget: ClassTag](ids: Seq[String]): Seq[GrantedPermission[A]]

	def getGrantedRolesFor[A <: PermissionsTarget: ClassTag](scope: A): Seq[GrantedRole[A]]
	def getGrantedPermissionsFor[A <: PermissionsTarget: ClassTag](scope: A): Seq[GrantedPermission[A]]

	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, customRoleDefinition: CustomRoleDefinition): Option[GrantedRole[A]]
	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, builtInRoleDefinition: BuiltInRoleDefinition): Option[GrantedRole[A]]

	def getGrantedPermission[A <: PermissionsTarget: ClassTag](scope: A, permission: Permission, overrideType: Boolean): Option[GrantedPermission[A]]

	def getGrantedRolesForUser[A <: PermissionsTarget: ClassTag](user: User): Seq[GrantedRole[A]]
	def getGrantedRolesForWebgroups[A <: PermissionsTarget: ClassTag](groupNames: Seq[String]): Seq[GrantedRole[A]]

	def getGrantedPermissionsForUser[A <: PermissionsTarget: ClassTag](user: User): Seq[GrantedPermission[A]]
	def getGrantedPermissionsForWebgroups[A <: PermissionsTarget: ClassTag](groupNames: Seq[String]): Seq[GrantedPermission[A]]

	def getGrantedRolesForDefinition(roleDefinition: RoleDefinition): Seq[GrantedRole[_]]

	def getCustomRoleDefinitionsBasedOn(baseDef: RoleDefinition): Seq[CustomRoleDefinition]
	def getCustomRoleDefinitionsFor(departments: Seq[Department]): Seq[CustomRoleDefinition]
}

@Repository
class PermissionsDaoImpl extends PermissionsDao with Daoisms {
	import Restrictions._

	def saveOrUpdate(roleDefinition: CustomRoleDefinition): Unit = session.saveOrUpdate(roleDefinition)
	def saveOrUpdate(permission: GrantedPermission[_]): Unit = session.saveOrUpdate(permission)
	def saveOrUpdate(role: GrantedRole[_]): Unit = session.saveOrUpdate(role)

	def delete(roleDefinition: CustomRoleDefinition): Unit = session.delete(roleDefinition)

	def getCustomRoleDefinitionById(id: String): Option[CustomRoleDefinition] = getById[CustomRoleDefinition](id)

	def getGrantedRole[A <: PermissionsTarget: ClassTag](id: String): Option[GrantedRole[A]] = getById[GrantedRole[A]](id)
	def getGrantedPermission[A <: PermissionsTarget: ClassTag](id: String): Option[GrantedPermission[A]] = getById[GrantedPermission[A]](id)

	def getGrantedRolesById[A <: PermissionsTarget: ClassTag](ids: Seq[String]): Seq[GrantedRole[A]] =
		if (ids.isEmpty) Nil
		else session.newCriteria[GrantedRole[A]](GrantedRole.classObject[A])
					 .add(in("id", ids.asJava))
					 .seq

	def getGrantedPermissionsById[A <: PermissionsTarget: ClassTag](ids: Seq[String]): Seq[GrantedPermission[A]] =
		if (ids.isEmpty) Nil
		else session.newCriteria[GrantedPermission[A]]
					 .add(in("id", ids.asJava))
					 .seq

	def getGrantedRolesFor[A <: PermissionsTarget: ClassTag](scope: A): Seq[GrantedRole[A]] = canDefineRoleSeq(scope) {
		session.newCriteria[GrantedRole[A]](GrantedRole.classObject[A])
					 .add(is("scope", scope))
					 .seq
	}

	def getGrantedPermissionsFor[A <: PermissionsTarget: ClassTag](scope: A): Seq[GrantedPermission[A]] = canDefinePermissionSeq(scope) {
		session.newCriteria[GrantedPermission[A]]
					 .add(is("scope", scope))
					 .seq
	}

	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, customRoleDefinition: CustomRoleDefinition): Option[GrantedRole[A]] = canDefineRole(scope) {
		session.newCriteria[GrantedRole[A]](GrantedRole.classObject[A])
					 .add(is("scope", scope))
					 .add(is("customRoleDefinition", customRoleDefinition))
					 .seq.headOption
	}

	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, builtInRoleDefinition: BuiltInRoleDefinition): Option[GrantedRole[A]] = canDefineRole(scope) {
		// TAB-2959
		session.newCriteria[GrantedRole[A]](GrantedRole.classObject[A])
					 .add(is("scope", scope))
					 .add(is("builtInRoleDefinition", builtInRoleDefinition))
					 .seq.headOption
	}

	def getGrantedPermission[A <: PermissionsTarget: ClassTag](scope: A, permission: Permission, overrideType: Boolean): Option[GrantedPermission[A]] = canDefinePermission(scope) {
		session.newCriteria[GrantedPermission[A]]
					 .add(is("scope", scope))
					 .add(is("permission", permission))
					 .add(is("overrideType", overrideType))
					 .seq.headOption
	}

	private def canDefinePermissionSeq[A <: PermissionsTarget](scope: A)(f: => Seq[GrantedPermission[A]]) = {
		if (GrantedPermission.canDefineFor(scope)) f
		else Seq()
	}

	private def canDefineRoleSeq[A <: PermissionsTarget](scope: A)(f: => Seq[GrantedRole[A]]) = {
		if (GrantedRole.canDefineFor(scope)) f
		else Seq()
	}

	private def canDefinePermission[A <: PermissionsTarget](scope: A)(f: => Option[GrantedPermission[A]]) = {
		if (GrantedPermission.canDefineFor(scope)) f
		else None
	}

	private def canDefineRole[A <: PermissionsTarget](scope: A)(f: => Option[GrantedRole[A]]) = {
		if (GrantedRole.canDefineFor(scope)) f
		else None
	}

	def getGrantedRolesForUser[A <: PermissionsTarget: ClassTag](user: User): Seq[GrantedRole[A]] =
		session.newQuery[GrantedRole[A]]("""
				select distinct r
				from """ + GrantedRole.className[A] + """ r
				where
					(
						r._users.universityIds = false and
						((:userId in elements(r._users.staticIncludeUsers)
						or :userId in elements(r._users.includeUsers))
						and :userId not in elements(r._users.excludeUsers))
					) or (
						r._users.universityIds = true and
						((:universityId in elements(r._users.staticIncludeUsers)
						or :universityId in elements(r._users.includeUsers))
						and :universityId not in elements(r._users.excludeUsers))
					)
		""")
			.setString("universityId", user.getWarwickId)
			.setString("userId", user.getUserId)
			.seq

	def getGrantedRolesForWebgroups[A <: PermissionsTarget: ClassTag](groupNames: Seq[String]): Seq[GrantedRole[A]] = {
		if (groupNames.nonEmpty) {
			val criteriaFactory = () => {
				val c =
					session.newCriteria[GrantedRole[A]](GrantedRole.classObject[A])
						.createAlias("_users", "users")

				GrantedRole.discriminator[A] foreach { discriminator =>
					c.add(is("class", discriminator))
				}

				c
			}

			safeInSeq(criteriaFactory, "users.baseWebgroup", groupNames)
		} else Nil
	}

	def getGrantedPermissionsForUser[A <: PermissionsTarget: ClassTag](user: User): Seq[GrantedPermission[A]] =
		session.newQuery[GrantedPermission[A]]("""
				select distinct r
				from """ + GrantedPermission.className[A] + """ r
				where
					(
						r._users.universityIds = false and
						((:userId in elements(r._users.staticIncludeUsers)
						or :userId in elements(r._users.includeUsers))
						and :userId not in elements(r._users.excludeUsers))
					) or (
						r._users.universityIds = true and
						((:universityId in elements(r._users.staticIncludeUsers)
						or :universityId in elements(r._users.includeUsers))
						and :universityId not in elements(r._users.excludeUsers))
					)
		""")
			.setString("universityId", user.getWarwickId)
			.setString("userId", user.getUserId)
			.seq


	def getGrantedPermissionsForWebgroups[A <: PermissionsTarget: ClassTag](groupNames: Seq[String]): Seq[GrantedPermission[A]] = {
		if (groupNames.isEmpty) Nil
		else {
			val criteriaFactory = () => {
				val c = session.newCriteria[GrantedPermission[A]]
					.createAlias("_users", "users")

				GrantedPermission.discriminator[A] map { discriminator =>
					c.add(is("class", discriminator))
				}

				c
			}

			safeInSeq(criteriaFactory, "users.baseWebgroup", groupNames)
		}
	}

	def getCustomRoleDefinitionsBasedOn(roleDefinition: RoleDefinition): Seq[CustomRoleDefinition] = {
		val criteria = session.newCriteria[CustomRoleDefinition]

		roleDefinition match {
			case builtIn: BuiltInRoleDefinition => criteria.add(is("builtInBaseRoleDefinition", builtIn))
			case custom: CustomRoleDefinition   => criteria.add(is("customBaseRoleDefinition", custom))
		}

		criteria.seq
	}

	def getCustomRoleDefinitionsFor(departments: Seq[Department]): Seq[CustomRoleDefinition] = {
		session.newCriteria[CustomRoleDefinition]
			.add(in("department", departments.asJavaCollection))
			.seq
	}

	def getGrantedRolesForDefinition(roleDefinition: RoleDefinition): Seq[GrantedRole[_]] = {
		val criteria = session.newCriteria[GrantedRole[_]]

		roleDefinition match {
			case builtIn: BuiltInRoleDefinition => criteria.add(is("builtInRoleDefinition", builtIn))
			case custom: CustomRoleDefinition   => criteria.add(is("customRoleDefinition", custom))
		}

		criteria.seq
	}
}

trait PermissionsDaoComponent {
	def permissionsDao: PermissionsDao
}

trait AutowiringPermissionsDaoComponent extends PermissionsDaoComponent {
	val permissionsDao: PermissionsDao = Wire[PermissionsDao]
}
