package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.{Logging, RequestLevelCaching}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions, PermissionsTarget, ScopelessPermission, SelectorPermission}
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.services.permissions.RoleService
import uk.ac.warwick.tabula.{CurrentUser, Features, PermissionDeniedException, SubmitPermissionDeniedException}
import uk.ac.warwick.tabula.commands.TaskBenchmarking

trait SecurityServiceComponent {
	def securityService: SecurityService
}

trait AutowiringSecurityServiceComponent extends SecurityServiceComponent {
	var securityService: SecurityService = Wire[SecurityService]
}

/**
 * Checks permissions.
 */
@Service
class SecurityService extends Logging with RequestLevelCaching[(CurrentUser, Permission, PermissionsTarget), Option[Boolean]]
	with TaskBenchmarking {

	var roleService: RoleService = Wire[RoleService]
	var features: Features = Wire[Features]

	type Response = Option[Boolean]
	type PermissionChecker = (CurrentUser, Permission, PermissionsTarget) => Response

	// The possible response types for a permissions check.
	// Continue means continue to the next check, otherwise it will stop and use the returned value.
	// It is an error for all the checks to return Continue, so it makes sense to have one check
	// at the end which never returns Continue.
	val Allow: Response = Some(true)
	val Deny: Response = Some(false)
	val Continue: Response = None // delegate to the next handler

	val checks: Seq[PermissionChecker] = List(checkRuntimeMember _, checkGod _, checkPermissions(_,_,_), checkRoles(delegatablePermissionsOnly = false, _,_,_))
	val delegationChecks:Seq[PermissionChecker] = List(checkGod _, checkRoles(delegatablePermissionsOnly = true,_,_,_))

	def checkRuntimeMember(user: CurrentUser, permission: Permission, scope: PermissionsTarget): Response = scope match {
		case ignore: RuntimeMember => Deny
		case _ => Continue
	}

	def checkGod(user: CurrentUser, permission: Permission, scope: PermissionsTarget): Response = if (user.god) Allow else Continue

	private def checkScopedPermission(
		allPermissions: Seq[(Permission, Option[PermissionsTarget])],
		user: CurrentUser,
		permission: Permission,
		scope: PermissionsTarget
	): Response = {
		def scopeMatches(permissionScope: PermissionsTarget, targetScope: PermissionsTarget): Boolean =
			// The ID matches, or there exists a parent that matches (recursive)
			permissionScope == targetScope ||
				(targetScope != null &&
					targetScope.permissionsParents != null &&
					targetScope.permissionsParents.exists(scopeMatches(permissionScope, _)))

		val matchingPermissions: Seq[Option[PermissionsTarget]] = (permission match {
			case selectorPerm: SelectorPermission[_] => allPermissions.filter {
				case (otherSelectorPerm: SelectorPermission[_], target)
					if (otherSelectorPerm.getClass == selectorPerm.getClass) &&
						 (selectorPerm <= otherSelectorPerm) => true
				case _ => false
			}
			case _ => allPermissions.filter{case(p, _) => p == permission}
		}) map { case (_, target) => target }

		val matchesScope = matchingPermissions.exists {
			case Some(permissionScope) => scopeMatches(permissionScope, scope)
			case None =>
				if (scope != null) true // Global permission
				else false
		}

		matchesScope match {
			case true => Allow
			case false => Continue
		}
	}

	def checkPermissions(
		allPermissions: Seq[(Permission, Option[PermissionsTarget])],
		user: CurrentUser,
		permission: Permission,
		scope: PermissionsTarget
	): Response =
		if (allPermissions == null || allPermissions.isEmpty) Continue
		else permission match {
			case _: ScopelessPermission => if (allPermissions.exists{case(p, _) => p == permission}) Allow else Continue
			case _ => checkScopedPermission(allPermissions, user, permission, scope)
		}

	def checkPermissions(user: CurrentUser, permission: Permission, scope: PermissionsTarget): Response = {
		val explicitPermissions = roleService.getExplicitPermissionsFor(user, scope)
		if (explicitPermissions == null || explicitPermissions.isEmpty) Continue
		else {
			val (allow, deny) = explicitPermissions.partition(_.permissionType)

			// Confusingly, we check for an "Allow" for the deny perms and then immediately deny it
			val denyPerms = deny map { defn => defn.permission -> defn.scope}

			if (checkPermissions(denyPerms, user, permission, scope) != Continue) Deny
			else {
				val allowPerms = allow map { defn => defn.permission -> defn.scope}

				checkPermissions(allowPerms, user, permission, scope)
			}
		}
	}

	// By using Some() here, we ensure that we return Deny if there isn't a role match - this must be the LAST permissions provider
	def checkRoles(roles: Iterable[Role], user: CurrentUser, permission: Permission, scope: PermissionsTarget): Response = Some(
		roles != null && (roles exists { role =>
			checkPermissions(role.explicitPermissions, user, permission, scope) == Allow ||
			checkRoles(role.subRoles, user, permission, scope) == Allow
		})
	)

	def checkRoles(delegatablePermissionsOnly:Boolean, user: CurrentUser, permission: Permission, scope: PermissionsTarget ): Response ={
		val rolesToCheck = roleService.getRolesFor(user, scope).filter(r=>{
			(! delegatablePermissionsOnly) ||r.definition.canDelegateThisRolesPermissions})
		checkRoles(rolesToCheck, user, permission, scope)
	}


	def can(user: CurrentUser, permission: ScopelessPermission ): Boolean = _can(user, permission, None, canDelegate = false)
	def can(user: CurrentUser, permission: Permission, scope: PermissionsTarget): Boolean = _can(user, permission, Option(scope), canDelegate = false)
	def canForAny(user: CurrentUser, permission: Permission, scopes: Seq[PermissionsTarget]): Boolean  = {
		scopes.exists(scope => _can(user, permission, Option(scope), canDelegate = false))
	}
	def canDelegate(user: CurrentUser, permission: ScopelessPermission ): Boolean = _can(user, permission, None, canDelegate = true)
	def canDelegate(user: CurrentUser, permission: Permission, scope: PermissionsTarget): Boolean = _can(user, permission, Option(scope), canDelegate = true)

	private def _can(user: CurrentUser, permission: Permission, scope: Option[PermissionsTarget],canDelegate:Boolean): Boolean = transactional(readOnly=true) {
		// Lazily go through the checks using a view, and try to get the first one that's Allow or Deny
		val checksToRun = if (canDelegate) delegationChecks else checks
		val result: Response = cachedBy((user, permission, scope.orNull)) {
			benchmarkTask(s"Checking permission ${if (permission != null) permission.getName}") {checksToRun.view.flatMap { _(user, permission, scope.orNull ) }.headOption}
		}

		/*
		 * If you are masquerading, we only show you the intersection of
		 * both yours and the target user's permissions, unless:
		 *
		 * - You're a sysadmin
		 * - The permission is Submission.Create (so you don't get not enrolled messages when trying to submit) FIXME Hardcoded ignorance
		 * - The feature flag is set to allow permission elevation (only for sandbox)
		 */
		val combinedResult = if (result.getOrElse(false) && user.masquerading && !user.sysadmin && permission != Permissions.Submission.Create && !features.masqueradeElevatedPermissions) {
			val realUser = new CurrentUser(user.realUser, user.realUser)
			cachedBy((realUser, permission, scope.orNull)) {
				checksToRun.view.flatMap { _(realUser, permission, scope.orNull ) }.headOption
			}
		} else result

		combinedResult.map { canDo =>
			if (debugEnabled) logger.debug("can " + user + " do " + permission + " on " + scope + "? " + (if (canDo) "Yes" else "NO"))
			canDo
		} getOrElse {
			throw new IllegalStateException("No security rule handled request for " + user + " doing " + permission + " on " + scope)
		}
	}

	def check(user: CurrentUser, permission: ScopelessPermission): Unit = _check(user, permission, None)
	def check(user: CurrentUser, permission: Permission, scope: PermissionsTarget): Unit = _check(user, permission, Option(scope))

	private def _check(user: CurrentUser, permission: Permission, scope: Option[PermissionsTarget]) = if (!_can(user, permission, scope, canDelegate = false)) {
		(permission, scope) match {
			case (Permissions.Submission.Create, Some(assignment: Assignment)) => throw new SubmitPermissionDeniedException(user, assignment)
			case (p, s) => throw new PermissionDeniedException(user, p, s)
		}
	}
}
