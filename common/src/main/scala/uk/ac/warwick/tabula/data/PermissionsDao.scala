package uk.ac.warwick.tabula.data

import java.util.UUID

import org.hibernate.criterion._
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.permissions._
import uk.ac.warwick.tabula.data.model.{Department, UserGroup}
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget}
import uk.ac.warwick.tabula.roles.{BuiltInRoleDefinition, RoleDefinition}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

trait PermissionsDao {
  def saveOrUpdate(roleDefinition: CustomRoleDefinition): Unit

  def saveOrUpdate(permission: GrantedPermission[_]): Unit

  def saveOrUpdate(role: GrantedRole[_]): Unit

  def delete(role: GrantedRole[_]): Unit

  def delete(roleDefinition: CustomRoleDefinition): Unit

  def getCustomRoleDefinitionById(id: String): Option[CustomRoleDefinition]

  def getGrantedRole[A <: PermissionsTarget : ClassTag](id: String): Option[GrantedRole[A]]

  def getGrantedPermission[A <: PermissionsTarget : ClassTag](id: String): Option[GrantedPermission[A]]

  def getGrantedRolesById[A <: PermissionsTarget : ClassTag](ids: Seq[String]): Seq[GrantedRole[A]]

  def getGrantedPermissionsById[A <: PermissionsTarget : ClassTag](ids: Seq[String]): Seq[GrantedPermission[A]]

  def getGrantedRolesFor[A <: PermissionsTarget : ClassTag](scope: A): Seq[GrantedRole[A]]

  def getGrantedPermissionsFor[A <: PermissionsTarget : ClassTag](scope: A): Seq[GrantedPermission[A]]

  def getGrantedRole[A <: PermissionsTarget : ClassTag](scope: A, customRoleDefinition: CustomRoleDefinition): Option[GrantedRole[A]]

  def getGrantedRole[A <: PermissionsTarget : ClassTag](scope: A, builtInRoleDefinition: BuiltInRoleDefinition): Option[GrantedRole[A]]

  def getGrantedPermission[A <: PermissionsTarget : ClassTag](scope: A, permission: Permission, overrideType: Boolean): Option[GrantedPermission[A]]

  def getGrantedRolesForUser[A <: PermissionsTarget : ClassTag](user: User): Seq[GrantedRole[A]]

  def getGrantedRolesForWebgroups[A <: PermissionsTarget : ClassTag](groupNames: Seq[String]): Seq[GrantedRole[A]]

  def getGrantedPermissionsForUser[A <: PermissionsTarget : ClassTag](user: User): Seq[GrantedPermission[A]]

  def getGrantedPermissionsForWebgroups[A <: PermissionsTarget : ClassTag](groupNames: Seq[String]): Seq[GrantedPermission[A]]

  def getGrantedRolesForDefinition(roleDefinition: RoleDefinition): Seq[GrantedRole[_]]

  def getCustomRoleDefinitionsBasedOn(baseDef: RoleDefinition): Seq[CustomRoleDefinition]

  def getCustomRoleDefinitionsFor(departments: Seq[Department]): Seq[CustomRoleDefinition]

  def getGlobalGrantedRole(builtIn: BuiltInRoleDefinition): Option[GrantedRole[PermissionsTarget]]
  def getGlobalGrantedRole(custom: CustomRoleDefinition): Option[GrantedRole[PermissionsTarget]]
  def getGlobalGrantedRoles: Seq[GrantedRole[PermissionsTarget]]
  def createGlobalGrantedRole(builtIn: BuiltInRoleDefinition): GrantedRole[PermissionsTarget]

  def getGlobalGrantedPermission(permission: Permission, overrideType: Boolean): Option[GrantedPermission[PermissionsTarget]]
  def getGlobalGrantedPermissions: Seq[GrantedPermission[PermissionsTarget]]
  def createGlobalGrantedPermission(permission: Permission, overrideType: Boolean): GrantedPermission[PermissionsTarget]
}

@Repository
class PermissionsDaoImpl extends PermissionsDao with Daoisms {

  import Restrictions._

  def saveOrUpdate(roleDefinition: CustomRoleDefinition): Unit = session.saveOrUpdate(roleDefinition)

  def saveOrUpdate(permission: GrantedPermission[_]): Unit = session.saveOrUpdate(permission)

  def saveOrUpdate(role: GrantedRole[_]): Unit = session.saveOrUpdate(role)

  def delete(role: GrantedRole[_]): Unit = session.delete(role)

  def delete(roleDefinition: CustomRoleDefinition): Unit = session.delete(roleDefinition)

  def getCustomRoleDefinitionById(id: String): Option[CustomRoleDefinition] = getById[CustomRoleDefinition](id)

  def getGrantedRole[A <: PermissionsTarget : ClassTag](id: String): Option[GrantedRole[A]] = getById[GrantedRole[A]](id)

  def getGrantedPermission[A <: PermissionsTarget : ClassTag](id: String): Option[GrantedPermission[A]] = getById[GrantedPermission[A]](id)

  private def newGrantedRoleCriteria[A <: PermissionsTarget : ClassTag]: ScalaCriteria[GrantedRole[A]] = {
    val c = session.newCriteria[GrantedRole[A]]

    GrantedRole.scopeType[A].foreach { scopeType =>
      c.add(is("scopeType", scopeType))
    }

    c
  }

  private def newGrantedPermissionCriteria[A <: PermissionsTarget : ClassTag]: ScalaCriteria[GrantedPermission[A]] = {
    val c = session.newCriteria[GrantedPermission[A]]

    GrantedPermission.scopeType[A].foreach { scopeType =>
      c.add(is("scopeType", scopeType))
    }

    c
  }

  def getGrantedRolesById[A <: PermissionsTarget : ClassTag](ids: Seq[String]): Seq[GrantedRole[A]] =
    if (ids.isEmpty) Nil
    else newGrantedRoleCriteria[A]
      .add(in("id", ids.asJava))
      .seq

  def getGrantedPermissionsById[A <: PermissionsTarget : ClassTag](ids: Seq[String]): Seq[GrantedPermission[A]] =
    if (ids.isEmpty) Nil
    else newGrantedPermissionCriteria[A]
      .add(in("id", ids.asJava))
      .seq

  def getGrantedRolesFor[A <: PermissionsTarget : ClassTag](scope: A): Seq[GrantedRole[A]] = canDefineRoleSeq(scope) {
    newGrantedRoleCriteria[A]
      .add(is("scope", scope))
      .seq
  }

  def getGrantedPermissionsFor[A <: PermissionsTarget : ClassTag](scope: A): Seq[GrantedPermission[A]] = canDefinePermissionSeq(scope) {
    newGrantedPermissionCriteria[A]
      .add(is("scope", scope))
      .seq
  }

  def getGrantedRole[A <: PermissionsTarget : ClassTag](scope: A, customRoleDefinition: CustomRoleDefinition): Option[GrantedRole[A]] = canDefineRole(scope) {
    newGrantedRoleCriteria[A]
      .add(is("scope", scope))
      .add(is("customRoleDefinition", customRoleDefinition))
      .seq.headOption
  }

  def getGrantedRole[A <: PermissionsTarget : ClassTag](scope: A, builtInRoleDefinition: BuiltInRoleDefinition): Option[GrantedRole[A]] = canDefineRole(scope) {
    // TAB-2959
    newGrantedRoleCriteria[A]
      .add(is("scope", scope))
      .add(is("builtInRoleDefinition", builtInRoleDefinition))
      .seq.headOption
  }

  def getGrantedPermission[A <: PermissionsTarget : ClassTag](scope: A, permission: Permission, overrideType: Boolean): Option[GrantedPermission[A]] = canDefinePermission(scope) {
    newGrantedPermissionCriteria[A]
      .add(is("scope", scope))
      .add(is("permission", permission))
      .add(is("overrideType", overrideType))
      .seq.headOption
  }

  private def canDefinePermissionSeq[A <: PermissionsTarget : ClassTag](scope: A)(f: => Seq[GrantedPermission[A]]) = {
    if (GrantedPermission.canDefineFor[A]) f
    else Seq()
  }

  private def canDefineRoleSeq[A <: PermissionsTarget : ClassTag](scope: A)(f: => Seq[GrantedRole[A]]) = {
    if (GrantedRole.canDefineFor[A]) f
    else Seq()
  }

  private def canDefinePermission[A <: PermissionsTarget : ClassTag](scope: A)(f: => Option[GrantedPermission[A]]) = {
    if (GrantedPermission.canDefineFor[A]) f
    else None
  }

  private def canDefineRole[A <: PermissionsTarget : ClassTag](scope: A)(f: => Option[GrantedRole[A]]) = {
    if (GrantedRole.canDefineFor[A]) f
    else None
  }

  def getGrantedRolesForUser[A <: PermissionsTarget : ClassTag](user: User): Seq[GrantedRole[A]] = {
    val scopeType = GrantedPermission.scopeType[A]

    def joinRestriction(alias: String): String =
      s"""on (
        (ug.universityIds = false and $alias = :userId) or
        (ug.universityIds = true and $alias = :universityId)
      )"""

    val q =
      session.newQuery[GrantedRole[A]](
    s"""
          select distinct r
          from GrantedRole r
            inner join r._users ug
            left join ug.staticIncludeUsers static ${joinRestriction("static")}
            left join ug.includeUsers include ${joinRestriction("include")}
            left join ug.excludeUsers exclude ${joinRestriction("exclude")}
          where
					  ${if (scopeType.nonEmpty) "r.scopeType = :scopeType and " else ""}
            (static is not null or include is not null) and exclude is null
        """)
        .setString("universityId", user.getWarwickId)
        .setString("userId", user.getUserId)

    scopeType.foreach(q.setString("scopeType", _))

    q.seq
  }

  def getGrantedRolesForWebgroups[A <: PermissionsTarget : ClassTag](groupNames: Seq[String]): Seq[GrantedRole[A]] = {
    if (groupNames.nonEmpty) {
      val criteriaFactory = () => newGrantedRoleCriteria[A].createAlias("_users", "users")

      safeInSeq(criteriaFactory, "users.baseWebgroup", groupNames)
    } else Nil
  }

  def getGrantedPermissionsForUser[A <: PermissionsTarget : ClassTag](user: User): Seq[GrantedPermission[A]] = {
    val scopeType = GrantedPermission.scopeType[A]

    def joinRestriction(alias: String): String =
      s"""on (
        (ug.universityIds = false and $alias = :userId) or
        (ug.universityIds = true and $alias = :universityId)
      )"""

    val q =
      session.newQuery[GrantedPermission[A]](
        s"""
          select distinct r
          from GrantedPermission r
            inner join r._users ug
            left join ug.staticIncludeUsers static ${joinRestriction("static")}
            left join ug.includeUsers include ${joinRestriction("include")}
            left join ug.excludeUsers exclude ${joinRestriction("exclude")}
          where
					  ${if (scopeType.nonEmpty) "r.scopeType = :scopeType and " else ""}
            (static is not null or include is not null) and exclude is null
        """)
        .setString("universityId", user.getWarwickId)
        .setString("userId", user.getUserId)

    scopeType.foreach(q.setString("scopeType", _))

    q.seq
  }

  def getGrantedPermissionsForWebgroups[A <: PermissionsTarget : ClassTag](groupNames: Seq[String]): Seq[GrantedPermission[A]] = {
    if (groupNames.isEmpty) Nil
    else {
      val criteriaFactory = () => newGrantedPermissionCriteria[A].createAlias("_users", "users")

      safeInSeq(criteriaFactory, "users.baseWebgroup", groupNames)
    }
  }

  def getCustomRoleDefinitionsBasedOn(roleDefinition: RoleDefinition): Seq[CustomRoleDefinition] = {
    val criteria = session.newCriteria[CustomRoleDefinition]

    roleDefinition match {
      case builtIn: BuiltInRoleDefinition => criteria.add(is("builtInBaseRoleDefinition", builtIn))
      case custom: CustomRoleDefinition => criteria.add(is("customBaseRoleDefinition", custom))
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
      case custom: CustomRoleDefinition => criteria.add(is("customRoleDefinition", custom))
    }

    criteria.seq
  }

  override def getGlobalGrantedRole(builtInRoleDefinition: BuiltInRoleDefinition): Option[GrantedRole[PermissionsTarget]] =
    session.newCriteria[GrantedRole[PermissionsTarget]]
      .add(is("scopeType", PermissionsTarget.GlobalScopeType))
      .add(is("builtInRoleDefinition", builtInRoleDefinition))
      .seq.headOption

  override def getGlobalGrantedRole(customRoleDefinition: CustomRoleDefinition): Option[GrantedRole[PermissionsTarget]] =
    session.newCriteria[GrantedRole[PermissionsTarget]]
      .add(is("scopeType", PermissionsTarget.GlobalScopeType))
      .add(is("customRoleDefinition", customRoleDefinition))
      .seq.headOption

  override def getGlobalGrantedRoles: Seq[GrantedRole[PermissionsTarget]] =
    session.newCriteria[GrantedRole[PermissionsTarget]]
      .add(is("scopeType", PermissionsTarget.GlobalScopeType))
      .seq

  override def getGlobalGrantedPermission(permission: Permission, overrideType: Boolean): Option[GrantedPermission[PermissionsTarget]] =
    session.newCriteria[GrantedPermission[PermissionsTarget]]
      .add(is("scopeType", PermissionsTarget.GlobalScopeType))
      .add(is("permission", permission))
      .add(is("overrideType", overrideType))
      .seq.headOption

  override def getGlobalGrantedPermissions: Seq[GrantedPermission[PermissionsTarget]] =
    session.newCriteria[GrantedPermission[PermissionsTarget]]
      .add(is("scopeType", PermissionsTarget.GlobalScopeType))
      .seq

  // Hnnnnnnng we can't create these in Hibernate because it doesn't know how to persist scope_type when scope is null
  override def createGlobalGrantedRole(builtInRoleDefinition: BuiltInRoleDefinition): GrantedRole[PermissionsTarget] = {
    val id = UUID.randomUUID().toString
    val userGroupId = session.save(UserGroup.ofUsercodes)

    val query =
      session.createNativeQuery("insert into GrantedRole (id, usergroup_id, builtinroledefinition, scope_type) values (:id, :usergroup_id, :builtinroledefinition, :scope_type)")
        .setParameter("id", id)
        .setParameter("usergroup_id", userGroupId)
        .setParameter("builtinroledefinition", new BuiltInRoleDefinitionUserType().convertToValue(builtInRoleDefinition))
        .setParameter("scope_type", PermissionsTarget.GlobalScopeType)

    require(query.executeUpdate() == 1)

    getGrantedRole[PermissionsTarget](id).get
  }

  override def createGlobalGrantedPermission(permission: Permission, overrideType: Boolean): GrantedPermission[PermissionsTarget] = {
    val id = UUID.randomUUID().toString
    val userGroupId = session.save(UserGroup.ofUsercodes)

    val query =
      session.createNativeQuery("insert into GrantedPermission (id, usergroup_id, permission, overridetype, scope_type) values (:id, :usergroup_id, :permission, :overridetype, :scope_type)")
        .setParameter("id", id)
        .setParameter("usergroup_id", userGroupId)
        .setParameter("permission", new PermissionUserType().convertToValue(permission))
        .setParameter("overridetype", overrideType)
        .setParameter("scope_type", PermissionsTarget.GlobalScopeType)

    require(query.executeUpdate() == 1)

    getGrantedPermission[PermissionsTarget](id).get
  }
}

trait PermissionsDaoComponent {
  def permissionsDao: PermissionsDao
}

trait AutowiringPermissionsDaoComponent extends PermissionsDaoComponent {
  val permissionsDao: PermissionsDao = Wire[PermissionsDao]
}
