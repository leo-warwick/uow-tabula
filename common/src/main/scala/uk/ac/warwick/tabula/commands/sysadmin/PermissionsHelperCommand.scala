package uk.ac.warwick.tabula.commands.sysadmin

import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.permissions.RoleService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.permissions.Permissions._
import org.springframework.core.convert.ConversionService
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.commands.SelfValidating
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.StringUtils
import org.springframework.core.convert.ConversionException
import uk.ac.warwick.tabula.services.permissions.PermissionDefinition

class PermissionsHelperCommand extends Command[PermissionHelperResult] with Unaudited with ReadOnly with SelfValidating {

	// This is a global permissions check, but we can't pass null here, so just pass a pretend object
	PermissionCheck(RolesAndPermissions.Read, PermissionsTarget.Global)

	var securityService: SecurityService = Wire.auto[SecurityService]
	var roleService: RoleService = Wire.auto[RoleService]

	var conversionService: ConversionService = Wire.auto[ConversionService]

	var user: User = null
	var scopeType: Class[_ <: PermissionsTarget] = null
	var scope: String = null
	var permission: Permission = null

	private def resolveScope() = {
		if (scopeType == null) {
			None
		} else if (!conversionService.canConvert(classOf[String], scopeType)) {
			logger.warn("Couldn't convert to " + scopeType)
			None
		} else try {
			Option(conversionService.convert(scope, scopeType))
		} catch {
			case e: ConversionException => {
				logger.info("Couldn't convert " + scope + " to " + scopeType)
				None
			}
		}
	}

	def validate(errors: Errors) {
		if (user == null || !user.isFoundUser) {
			errors.rejectValue("user","permissionsHelper.user.invalid")
		}

		if (scopeType != null && !conversionService.canConvert(classOf[String], scopeType)) {
			errors.rejectValue("scopeType","permissionsHelper.scopeType.invalid")
		} else if (scopeType != null && resolveScope().isEmpty) {
			// Check that we can resolve the scope
			errors.rejectValue("scope","permissionsHelper.scope.invalid")
		}

		if (permission != null && permission.getName == "Permission") {
			errors.rejectValue("permission","permissionsHelper.permission.invalid")
		}
	}

	def applyInternal(): PermissionHelperResult = {
		val currentUser = new CurrentUser(user, user)

		val scope = resolveScope().orNull
		val scopeMissing = scope == null
		val scopeMismatch = permission != null && (permission.isScoped == scopeMissing)

		val permissions = roleService.getExplicitPermissionsFor(currentUser, scope)
		val roles = roleService.getRolesFor(currentUser, scope)

		val canDo =
			if (permission != null) securityService.can(currentUser, permission, scope)
			else false

		PermissionHelperResult(
			canDo = canDo,
			permissions = permissions.toList,
			roles = roles.toList,
			resolvedScope = scope,
			scopeMismatch = scopeMismatch,
			scopeMissing = scopeMissing
		)
	}

}

case class PermissionHelperResult(
	canDo: Boolean,
	permissions: List[PermissionDefinition],
	roles: List[Role],
	resolvedScope: PermissionsTarget,
	scopeMismatch: Boolean,
	scopeMissing: Boolean
)
