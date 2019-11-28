package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.CurrentUser
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsSelector, PermissionsTarget}
import uk.ac.warwick.tabula.roles._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, GrantedPermission}
import uk.ac.warwick.tabula.helpers.RequestLevelCaching
import uk.ac.warwick.tabula.data.model.Department

/**
  * Provides a stream of roles that apply for a particular user on a particular scope. The role service
  * will go through parents of a scope (unless the provider returns results and isExhaustive is true),
  * so you don't need to do that manually in the RoleProvider - it will be called repeatedly as we go up
  * the parents of the scope.
  */
trait RoleProvider {
  def getRolesFor(user: CurrentUser, scope: PermissionsTarget): LazyList[Role]

  def rolesProvided: Set[Class[_ <: Role]]

  /**
    * Override and return true if this service is exhaustive - i.e. you should continue to interrogate it even after it has returned results
    */
  def isExhaustive = false

  protected def customRoleFor[A <: PermissionsTarget](department: Option[Department])(definition: RoleDefinition, scope: A): Option[Role] =
    department.flatMap { d =>
      customRoleFor(d)(definition, scope)
    }

  protected def customRoleFor[A <: PermissionsTarget, B <: PermissionsSelector[B]](department: Department)(originalDefinition: RoleDefinition, scope: A): Option[Role] =
    department.replacedRoleDefinitionFor(originalDefinition).map { customDefinition => {
      originalDefinition match {
        case originalSelectorRoleDefinition: SelectorBuiltInRoleDefinition[B@unchecked] => customDefinition.baseRoleDefinition match {
          case customBaseSelectorRoleDefinition: SelectorBuiltInRoleDefinition[B@unchecked] =>
            val correctedBaseSelectorDefinition = customBaseSelectorRoleDefinition.duplicate(Option(originalSelectorRoleDefinition.selector))
            val newCustomDefinition = new CustomRoleDefinition
            newCustomDefinition.baseRoleDefinition = correctedBaseSelectorDefinition
            newCustomDefinition.name = customDefinition.name
            newCustomDefinition.canDelegateThisRolesPermissions = customDefinition.canDelegateThisRolesPermissions
            newCustomDefinition.overrides = customDefinition.overrides
            newCustomDefinition.replacesBaseDefinition = customDefinition.replacesBaseDefinition
            newCustomDefinition.department = customDefinition.department
            RoleBuilder.build(newCustomDefinition, Some(scope), newCustomDefinition.getName)
          case _ =>
            RoleBuilder.build(customDefinition, Some(scope), customDefinition.getName)
        }
        case _ =>
          RoleBuilder.build(customDefinition, Some(scope), customDefinition.getName)
      }
    }
    }


}

/**
  * A specialisation of RoleProvider that doesn't care about scope. This allows us to cache it per-request
  * because it's unaffected by scope, and do other optimisations.
  */
trait ScopelessRoleProvider extends RoleProvider with RequestLevelCaching[CurrentUser, LazyList[Role]] {
  final def getRolesFor(user: CurrentUser, scope: PermissionsTarget): LazyList[Role] = cachedBy(user) {
    getRolesFor(user)
  }

  def getRolesFor(user: CurrentUser): LazyList[Role]
}

case class PermissionDefinition(permission: Permission, scope: Option[PermissionsTarget], permissionType: GrantedPermission.OverrideType)

/**
  * Provides a stream of individual permissions that apply for a particular user on a particular scope. The service
  * will go through parents of a scope (unless the provider returns results and isExhaustive is true),
  * so you don't need to do that manually in the PermissionsProvider - it will be called repeatedly as we go up
  * the parents of the scope.
  */
trait PermissionsProvider {
  def getPermissionsFor(user: CurrentUser, scope: PermissionsTarget): LazyList[PermissionDefinition]

  /**
    * Override and return true if this service is exhaustive - i.e. you should continue to interrogate it even after it has returned results
    */
  def isExhaustive = false
}

/**
  * Specialisation of PermissionsProvider that ignores scope. Use this if possible as it has
  * performance enhancements.
  */
trait ScopelessPermissionsProvider extends PermissionsProvider with RequestLevelCaching[CurrentUser, LazyList[PermissionDefinition]] {
  final def getPermissionsFor(user: CurrentUser, scope: PermissionsTarget): LazyList[PermissionDefinition] = cachedBy(user) {
    getPermissionsFor(user)
  }

  def getPermissionsFor(user: CurrentUser): LazyList[PermissionDefinition]
}

trait RoleService {
  def getExplicitPermissionsFor(user: CurrentUser, scope: PermissionsTarget): LazyList[PermissionDefinition]

  def getRolesFor(user: CurrentUser, scope: PermissionsTarget, isAssistant: Boolean = false): LazyList[Role]

  def hasRole(user: CurrentUser, role: Role): Boolean
}

@Service
class RoleServiceImpl extends RoleService with Logging {

  /** Spring should wire in all beans that extend RoleProvider */
  @Autowired var roleProviders: Array[RoleProvider] = Array()

  /** Spring should wire in all beans that extend PermissionsProvider */
  @Autowired var permissionsProviders: Array[PermissionsProvider] = Array()

  /**
    * Go through all the permissions providers iteratively for the scope and then any
    * parents of the scope, collecting a stream of all the explicitly granted permissions
    * for the user and the scope.
    */
  def getExplicitPermissionsFor(user: CurrentUser, scope: PermissionsTarget): LazyList[PermissionDefinition] = {
    def streamScoped(providers: LazyList[PermissionsProvider], scope: PermissionsTarget): LazyList[PermissionDefinition] = {
      val results = providers map { provider => (provider, provider.getPermissionsFor(user, scope)) }
      val (hasResults, noResults) = results.partition {
        _._2.nonEmpty
      }

      val stream = hasResults.flatMap(_._2)

      // For each of the parents, call the stack again, excluding any exhaustive providers that have returned results
      val next = scope.permissionsParents flatMap {
        streamScoped((noResults #::: (hasResults.filter(_._1.isExhaustive))).map(_._1), _)
      }

      stream #::: next
    }

    def streamUnscoped(providers: LazyList[PermissionsProvider]): LazyList[PermissionDefinition] = {
      providers.flatMap(_.getPermissionsFor(user, null)).filter(_.scope.isEmpty)
    }

    if (scope == null) {
      streamUnscoped(permissionsProviders.to(LazyList))
    } else {
      streamScoped(permissionsProviders.to(LazyList), scope)
    }
  }


  def getRolesFor(user: CurrentUser, scope: PermissionsTarget, isAssistant: Boolean = false): LazyList[Role] = {
    // Split providers into Scopeless and scoped
    val (scopeless, allScoped) = roleProviders.to(LazyList).partition(_.isInstanceOf[ScopelessRoleProvider])

    // if we are getting roles for an assistant then don't use the StaffMemberAssistantRoleProvider again (avoids assistant relationships chaining)
    val scoped = if (isAssistant) allScoped.filterNot(_.isInstanceOf[StaffMemberAssistantRoleProvider]) else allScoped

    // We only need to do scopeless once
    // (we call the (User, Target) method signature otherwise it bypasses the request level caching)
    val scopelessLazyList = scopeless.flatMap(_.asInstanceOf[ScopelessRoleProvider].getRolesFor(user, null))

    /* We don't want to needlessly continue to interrogate scoped providers even after they
     * have returned something that isn't an empty Seq. Anything that isn't an empty Seq
     * can be treated as the final action of this provider EXCEPT in the case of the custom
     * role provider, so we special-case that */
    def lazyListScoped(providers: LazyList[RoleProvider], scope: PermissionsTarget): LazyList[Role] = {
      if (scope == null) LazyList.empty
      else {
        val results = providers map { provider => (provider, provider.getRolesFor(user, scope)) }
        val (hasResults, noResults) = results.partition(_._2.nonEmpty)

        val lazyList = hasResults.flatMap(_._2)
        val next = scope.permissionsParents flatMap {
          lazyListScoped((noResults #::: (hasResults.filter(_._1.isExhaustive))).map(_._1), _)
        }

        lazyList #::: next
      }
    }

    scopelessLazyList #::: lazyListScoped(scoped, scope)
  }

  def hasRole(user: CurrentUser, role: Role): Boolean = {
    val targetClass = role.getClass

    // Go through the list of RoleProviders and get any that provide this role
    val allRoles = roleProviders.filter(_.rolesProvided.contains(targetClass)).flatMap {
      case scopeless: ScopelessRoleProvider => scopeless.getRolesFor(user, null)
      case provider if role.scope.nonEmpty => provider.getRolesFor(user, role.scope.get)
      case _ => Seq()
    }

    allRoles.contains(role)
  }

}

trait RoleServiceComponent {
  def roleService: RoleService
}

trait AutowiringRoleServiceComponent extends RoleServiceComponent {
  var roleService: RoleService = Wire[RoleService]
}
