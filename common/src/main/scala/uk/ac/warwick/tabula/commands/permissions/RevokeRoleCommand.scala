package uk.ac.warwick.tabula.commands.permissions

import scala.collection.JavaConverters._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{Appliable, ComposableCommand, CommandInternal, Describable, Description, SelfValidating}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent, AutowiringSecurityServiceComponent, SecurityServiceComponent}
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.RequestInfo
import scala.reflect._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object RevokeRoleCommand {
	def apply[A <: PermissionsTarget : ClassTag](scope: A): Appliable[GrantedRole[A]] with RevokeRoleCommandState[A] =
		new RevokeRoleCommandInternal(scope)
			with ComposableCommand[GrantedRole[A]]
			with RevokeRoleCommandPermissions
			with RevokeRoleCommandValidation
			with RevokeRoleCommandDescription[A]
			with AutowiringPermissionsServiceComponent
			with AutowiringSecurityServiceComponent
			with AutowiringUserLookupComponent

	def apply[A <: PermissionsTarget : ClassTag](scope: A, defin: RoleDefinition): Appliable[GrantedRole[A]] with RevokeRoleCommandState[A] = {
		val command = apply(scope)
		command.roleDefinition = defin
		command
	}
}

class RevokeRoleCommandInternal[A <: PermissionsTarget : ClassTag](val scope: A) extends CommandInternal[GrantedRole[A]] with RevokeRoleCommandState[A] {
	self: PermissionsServiceComponent with UserLookupComponent =>

	lazy val grantedRole: Option[GrantedRole[A]] = permissionsService.getGrantedRole(scope, roleDefinition)

	def applyInternal(): GrantedRole[A] = transactional() {
		grantedRole.foreach { role =>
			usercodes.asScala.foreach(role.users.knownType.removeUserId)

			permissionsService.saveOrUpdate(role)

			// For each usercode that we've removed, clear the cache
			usercodes.asScala.foreach { usercode =>
				permissionsService.clearCachesForUser((usercode, classTag[A]))
			}
		}

		grantedRole.orNull
	}

}

trait RevokeRoleCommandValidation extends SelfValidating {
	self: RevokeRoleCommandState[_ <: PermissionsTarget] with SecurityServiceComponent =>

	def validate(errors: Errors) {
		if (usercodes.asScala.forall { _.isEmptyOrWhitespace }) {
			errors.rejectValue("usercodes", "NotEmpty")
		} else {
			grantedRole.map { _.users }.foreach { users =>
				for (code <- usercodes.asScala) {
					if (!users.knownType.includesUserId(code)) {
						errors.rejectValue("usercodes", "userId.notingroup", Array(code), "")
					}
				}
			}
		}

		// Ensure that the current user can delegate everything that they're trying to revoke permissions for
		if (roleDefinition == null) {
			errors.rejectValue("roleDefinition", "NotEmpty")
		} else {
			if (!roleDefinition.isAssignable) errors.rejectValue("roleDefinition", "permissions.roleDefinition.notAssignable")
			val user = RequestInfo.fromThread.get.user

			val permissionsToRevoke = roleDefinition.allPermissions(Some(scope)).keys
			val deniedPermissions = permissionsToRevoke.filterNot(securityService.canDelegate(user,_,scope))
			if (deniedPermissions.nonEmpty && !user.god) {
				errors.rejectValue("roleDefinition", "permissions.cantRevokeWhatYouDontHave", Array(deniedPermissions.map { _.description }.mkString("\n"), scope),"")
			}
		}
	}
}

trait RevokeRoleCommandState[A <: PermissionsTarget] {
	self: PermissionsServiceComponent =>

	def scope: A
	var roleDefinition: RoleDefinition = _
	var usercodes: JList[String] = JArrayList()

	def grantedRole: Option[GrantedRole[A]]
}

trait RevokeRoleCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: RevokeRoleCommandState[_ <: PermissionsTarget] =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.RolesAndPermissions.Delete, mandatory(scope))
	}
}

trait RevokeRoleCommandDescription[A <: PermissionsTarget] extends Describable[GrantedRole[A]] {
	self: RevokeRoleCommandState[A] =>

	def describe(d: Description): Unit = d.properties(
		"scope" -> (scope.getClass.getSimpleName + "[" + scope.id + "]"),
		"usercodes" -> usercodes.asScala.mkString(","),
		"roleDefinition" -> roleDefinition.getName)
}