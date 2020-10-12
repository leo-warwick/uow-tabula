package uk.ac.warwick.tabula.commands.admin.permissions

import uk.ac.warwick.tabula.commands.admin.permissions.BuildPermissionsTreeCommand.{PermissionAndUsers, PermissionsTree, RoleAndUsers}
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import uk.ac.warwick.tabula.permissions.{Permission, Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.roles.{RoleDefinition, UserAccessMgrRoleDefinition}
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

object BuildPermissionsTreeCommand {

  // Case classes to make Freemarker more explicit
  case class RoleAndUsers(definition: RoleDefinition, users: Seq[User])

  case class PermissionAndUsers(permission: Permission, users: Seq[User])

  case class PermissionsTree[A <: PermissionsTarget : ClassTag](
    target: A,
    roles: Seq[RoleAndUsers],
    permissions: Seq[PermissionAndUsers],
    children: Seq[PermissionsTree[_]]
  )

  def apply[A <: PermissionsTarget : ClassTag](target: A) =
    new BuildPermissionsTreeCommandInternal(target)
      with ComposableCommand[PermissionsTree[A]]
      with PermissionsTreeBuilderImpl
      with AutowiringPermissionsServiceComponent
      with BuildPermissionsTreeCommandPermissions
      with Unaudited with ReadOnly
}

trait BuildPermissionsTreeCommandState {
  def target: PermissionsTarget
}

trait PermissionsTreeBuilder {
  def buildTree[A <: PermissionsTarget : ClassTag](target: A): PermissionsTree[A]
}

trait PermissionsTreeBuilderImpl extends PermissionsTreeBuilder {
  self: PermissionsServiceComponent =>

  def buildTree[A <: PermissionsTarget : ClassTag](target: A): PermissionsTree[A] = {
    val roles =
      permissionsService.getAllGrantedRolesFor(target)
        .groupBy(_.roleDefinition)
        .toSeq
        .sortBy { case (defn, _) => defn.allPermissions(Some(null)).size }
        .reverse
        .map { case (defn, grantedRoles) => RoleAndUsers(defn, grantedRoles.flatMap(_.users.users).distinct)
        }
        .filter { case r =>
          // Only include empty roles if it's the UAM role
          r.definition == UserAccessMgrRoleDefinition || r.users.nonEmpty
        }

    val permissions =
      permissionsService.getAllGrantedPermissionsFor(target)
        .filter(_.overrideType == GrantedPermission.Allow)
        .groupBy(_.permission)
        .toSeq
        .map { case (p, grantedPermissions) => PermissionAndUsers(p, grantedPermissions.flatMap(_.users.users).distinct)
        }

    PermissionsTree(
      target = target,
      roles = roles,
      permissions = permissions,
      children = permissionsChildren(target)
    )
  }

  def permissionsChildren[A <: PermissionsTarget](target: A): Seq[PermissionsTree[_]] = target match {
    case d: Department =>
      d.children.asScala.toSeq.sortBy(_.name).map(buildTree) ++
        d.modules.asScala.toSeq.sorted.map(buildTree) ++
        d.routes.asScala.toSeq.sorted.map(buildTree)
    case _ => Nil
  }
}

abstract class BuildPermissionsTreeCommandInternal[A <: PermissionsTarget : ClassTag](val target: A) extends CommandInternal[PermissionsTree[A]] with BuildPermissionsTreeCommandState {
  self: PermissionsTreeBuilder =>

  def applyInternal(): PermissionsTree[A] = buildTree(target)
}

trait BuildPermissionsTreeCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: BuildPermissionsTreeCommandState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.RolesAndPermissions.Read, mandatory(target))
  }
}
