package uk.ac.warwick.tabula.data.model.permissions

import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{Department, GeneratedId, HibernateVersioned}
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget}
import uk.ac.warwick.tabula.roles.{BuiltInRoleDefinition, RoleDefinition}

import scala.jdk.CollectionConverters._

@Entity
@Proxy
class CustomRoleDefinition extends RoleDefinition with HibernateVersioned with GeneratedId with PermissionsTarget {

  // The department which owns this definition - probably want to expand this to include sub-departments later
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  var department: Department = _

  var name: String = _

  // Role uses getName. Could change it to name.
  def getName: String = name

  def description: String = "%s (derived from %s)".format(name, Option(baseRoleDefinition).map(_.description) getOrElse ("another role"))

  def isAssignable = true

  // The role definition that this role infers from; can be a built in role definition
  // or a custom role definition
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "custom_base_role_id")
  var customBaseRoleDefinition: CustomRoleDefinition = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.permissions.BuiltInRoleDefinitionUserType")
  var builtInBaseRoleDefinition: BuiltInRoleDefinition = _

  def baseRoleDefinition: RoleDefinition = Option(customBaseRoleDefinition) getOrElse builtInBaseRoleDefinition

  def baseRoleDefinition_=(definition: RoleDefinition): Unit = definition match {
    case customDefinition: CustomRoleDefinition => {
      customBaseRoleDefinition = customDefinition
      builtInBaseRoleDefinition = null
    }
    case builtInDefinition: BuiltInRoleDefinition => {
      customBaseRoleDefinition = null
      builtInBaseRoleDefinition = builtInDefinition
    }
    case _ => {
      customBaseRoleDefinition = null
      builtInBaseRoleDefinition = null
    }
  }

  @Column(name = "CanDelegate")
  var canDelegateThisRolesPermissions: JBoolean = false

  // A set of role overrides
  @OneToMany(mappedBy = "customRoleDefinition", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @BatchSize(size = 200)
  var overrides: JList[RoleOverride] = JArrayList()

  @Column(name = "REPLACES_PARENT")
  var replacesBaseDefinition: JBoolean = false

  def permissionsParents: LazyList[Department] =
    Option(department).to(LazyList)

  /**
    * This method eagerly resolves sub-roles, which is why we return
    * an empty set of actual sub-roles. It has to resolve now so that
    * we can do the removal accurately - otherwise we won't be able to
    * remove permissions added in sub-roles.
    */
  def permissions(scope: Option[PermissionsTarget]): Map[Permission, Option[PermissionsTarget]] = {
    val basePermissions = baseRoleDefinition.allPermissions(scope)

    val (additionOverrides, removalOverrides) = overrides.asScala.partition(_.overrideType)
    val additions = additionOverrides.map(_.permission -> scope)
    val removals = removalOverrides.map(_.permission)

    (basePermissions ++ additions) -- removals
  }

  def subRoles(scope: Option[PermissionsTarget]) = Set()

  /**
    * Return all permissions, resolving sub-roles. This is the behaviour of permissions() anyway
    */
  def allPermissions(scope: Option[PermissionsTarget]): Map[Permission, Option[PermissionsTarget]] =
    permissions(scope)

  def mayGrant(target: Permission): Boolean = {
    val baseGrants = baseRoleDefinition.mayGrant(target)
    val explicitDeny = overrides.asScala.exists { o => o.overrideType == RoleOverride.Deny && o.permission == target }
    val explicitGrant = overrides.asScala.exists { o => o.overrideType == RoleOverride.Allow && o.permission == target }

    (baseGrants || explicitGrant) && !explicitDeny
  }

}
