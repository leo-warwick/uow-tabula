package uk.ac.warwick.tabula.roles

import org.apache.commons.lang3.builder.{EqualsBuilder, HashCodeBuilder}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.{CaseObjectEqualityFixes, JavaImports}

import scala.collection.immutable.ListMap

trait RoleDefinition {
  /**
    * The canonical machine-readable name for this role. Used for listing and (for built-ins) as a database identifier
    */
  def getName: String

  /**
    * A short description of this definition; usually a human-readable version of getName
    */
  def description: String

  /**
    * Whether this role can be assigned to a user in the system. Return false for inferred roles
    */
  def isAssignable: Boolean

  def permissions(scope: Option[PermissionsTarget]): Map[Permission, Option[PermissionsTarget]]

  def subRoles(scope: Option[PermissionsTarget]): Set[Role]

  /**
    * Return all permissions, resolving sub-roles
    */
  def allPermissions(scope: Option[PermissionsTarget]): Map[Permission, Option[PermissionsTarget]]

  def delegatablePermissions(scope: Option[PermissionsTarget]): Map[Permission, Option[PermissionsTarget]] = {
    if (canDelegateThisRolesPermissions) allPermissions(scope) else Map.empty
  }

  def canDelegateThisRolesPermissions: JBoolean

  def mayGrant(target: Permission): Boolean
}

trait BuiltInRoleDefinition extends CaseObjectEqualityFixes[BuiltInRoleDefinition] with RoleDefinition {
  val getName: String = RoleDefinition.shortName(getClass.asInstanceOf[Class[_ <: BuiltInRoleDefinition]])

  private var scopedPermissions: List[Permission] = List()
  private var scopelessPermissions: List[ScopelessPermission] = List()
  private var globalPermissions: List[Permission] = List()
  private var subRoleDefinitions: Set[BuiltInRoleDefinition] = Set()

  def GrantsScopelessPermission(perms: ScopelessPermission*): Unit =
    for (permission <- perms) scopelessPermissions ::= permission

  def GrantsScopedPermission(perms: Permission*): Unit =
    for (permission <- perms) scopedPermissions ::= permission

  def GrantsGlobalPermission(perms: Permission*): Unit =
    for (permission <- perms) globalPermissions ::= permission

  def GeneratesSubRole(roles: BuiltInRoleDefinition*): Unit =
    for (role <- roles) subRoleDefinitions += role

  def permissions(scope: Option[PermissionsTarget]): ListMap[Permission, Option[PermissionsTarget]] =
    ListMap() ++
      (if (scope.isDefined) scopedPermissions map {
        _ -> scope
      } else Map()) ++
      (globalPermissions map {
        _ -> None
      }) ++
      (scopelessPermissions map {
        _ -> None
      })

  def subRoles(scope: Option[PermissionsTarget]): Set[Role] =
    subRoleDefinitions map { defn => RoleBuilder.build(defn, scope, defn.getName) }

  def mayGrant(permission: Permission): Boolean =
    scopedPermissions.contains(permission) ||
      scopelessPermissions.contains(permission) ||
      globalPermissions.contains(permission) ||
      (subRoleDefinitions.exists(_.mayGrant(permission)))

  /**
    * Return all permissions, resolving sub-roles
    */
  def allPermissions(scope: Option[PermissionsTarget]): Map[Permission, Option[PermissionsTarget]] =
    permissions(scope) ++ (subRoleDefinitions.flatMap(_.allPermissions(scope)))

  def isAssignable = true
}

abstract class SelectorBuiltInRoleDefinition[A <: PermissionsSelector[A]](val selector: PermissionsSelector[A]) extends BuiltInRoleDefinition {
  override val getName: String = SelectorBuiltInRoleDefinition.shortName(getClass.asInstanceOf[Class[_ <: SelectorBuiltInRoleDefinition[_]]])

  def <=[B <: PermissionsSelector[B]](other: SelectorBuiltInRoleDefinition[B]): Boolean = other match {
    case that: SelectorBuiltInRoleDefinition[A] => selector <= that.selector.asInstanceOf[PermissionsSelector[A]]
    case _ => false
  }

  override def equals(other: Any): Boolean = other match {
    case that: SelectorBuiltInRoleDefinition[A] =>
      new EqualsBuilder()
        .append(getName, that.getName)
        .append(selector, that.selector)
        .build()
    case _ => false
  }

  override def hashCode(): Int =
    new HashCodeBuilder()
      .append(getName)
      .append(selector)
      .build()

  override def toString(): String = "%s(%s)".format(super.toString(), selector)

  def duplicate(selector: Option[PermissionsSelector[A]]): SelectorBuiltInRoleDefinition[A]
}

object SelectorBuiltInRoleDefinition {
  private val ObjectClassPrefix = RoleDefinition.getClass.getPackage.getName + "."

  def of[A <: PermissionsSelector[A]](name: String, selector: Object): SelectorBuiltInRoleDefinition[A] = {
    try {
      // Go through the magical hierarchy
      val clz = Class.forName(ObjectClassPrefix + name)
      clz.getConstructors()(0).newInstance(selector).asInstanceOf[SelectorBuiltInRoleDefinition[A]]
    } catch {
      case e: ClassNotFoundException => throw new IllegalArgumentException("Role definition " + name + " not recognised")
    }
  }

  def shortName(clazz: Class[_ <: BuiltInRoleDefinition]): String
  = clazz.getName.substring(ObjectClassPrefix.length, clazz.getName.length).replace('$', '.')
}

trait UnassignableBuiltInRoleDefinition extends BuiltInRoleDefinition {
  override def isAssignable = false

  final def canDelegateThisRolesPermissions: JavaImports.JBoolean = false

}

object RoleDefinition {
  private val ObjectClassPrefix = RoleDefinition.getClass.getPackage.getName + "."

  /**
    * Create a RoleDefinition from its name (e.g. "ModuleManagerRoleDefinition").
    * Most likely useful in view templates, for permissions checking, or for db serialisation.
    *
    * Note that, like the templates they're used in, the correctness isn't
    * checked at runtime.
    */
  def of(name: String): BuiltInRoleDefinition = {
    try {
      // Go through the magical hierarchy
      val clz = Class.forName(ObjectClassPrefix + name + "$")
      clz.getDeclaredField("MODULE$").get(null).asInstanceOf[BuiltInRoleDefinition]
    } catch {
      case e: ClassNotFoundException => throw new IllegalArgumentException("Role definition " + name + " not recognised")
    }
  }

  def shortName(clazz: Class[_ <: BuiltInRoleDefinition]): String
  = clazz.getName.substring(ObjectClassPrefix.length, clazz.getName.length - 1).replace('$', '.')
}

abstract class Role(val definition: RoleDefinition, val scope: Option[PermissionsTarget]) {

  private var permissions: Seq[(Permission, Option[PermissionsTarget])] = Seq()
  private var roles: Set[Role] = Set()

  def getName: String = getClass.getSimpleName

  def isScoped: Boolean = scope.isDefined

  lazy val explicitPermissions: Seq[(Permission, Option[PermissionsTarget])] = permissions

  lazy val explicitPermissionsAsList: List[(Permission, Option[PermissionsTarget])] = explicitPermissions.toList
  lazy val viewablePermissionsAsList: List[(Permission, Option[PermissionsTarget])] = explicitPermissionsAsList.filterNot { case (p, _) => delegatePermissions.contains(p) }
  lazy val subRoles: Set[Role] = roles

  private final val delegatePermissions = Seq(
    Permissions.RolesAndPermissions.Create,
    Permissions.RolesAndPermissions.Read,
    Permissions.RolesAndPermissions.Update,
    Permissions.RolesAndPermissions.Delete
  )

  private final def applyRoleDefinition(definition: RoleDefinition): Role = {
    permissions ++= definition.permissions(scope)
    if (definition.canDelegateThisRolesPermissions) {
      permissions ++= delegatePermissions.map(_ -> scope)
    }
    roles ++= definition.subRoles(scope)

    this
  }

  applyRoleDefinition(definition)
}

abstract class BuiltInRole(definition: BuiltInRoleDefinition, scope: Option[PermissionsTarget]) extends Role(definition, scope) {
  def this(definition: BuiltInRoleDefinition, scope: PermissionsTarget) = {
    this(definition, Option(scope))
  }
}
