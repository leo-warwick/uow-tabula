package uk.ac.warwick.tabula.commands.permissions

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.commands.{Appliable, CommandInternal, ComposableCommand, Describable, Description, SelfValidating}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, AutowiringUserLookupComponent, SecurityServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.validators.UsercodeListValidator

import scala.collection.JavaConverters._
import scala.reflect._

object GrantRoleCommand {
	def apply[A <: PermissionsTarget : ClassTag](scope: A): Appliable[GrantedRole[A]] with GrantRoleCommandState[A] =
		new GrantRoleCommandInternal(scope)
				with ComposableCommand[GrantedRole[A]]
				with GrantRoleCommandPermissions
				with GrantRoleCommandValidation
				with GrantRoleCommandDescription[A]
				with AutowiringPermissionsServiceComponent
				with AutowiringSecurityServiceComponent
				with AutowiringUserLookupComponent

	def apply[A <: PermissionsTarget : ClassTag](scope: A, defin: RoleDefinition): Appliable[GrantedRole[A]] with GrantRoleCommandState[A] = {
		val command = apply(scope)
		command.roleDefinition = defin
		command
	}
}

class GrantRoleCommandInternal[A <: PermissionsTarget : ClassTag](val scope: A) extends CommandInternal[GrantedRole[A]] with GrantRoleCommandState[A] {
	self: PermissionsServiceComponent with UserLookupComponent =>

	lazy val grantedRole: Option[GrantedRole[A]] = permissionsService.getGrantedRole(scope, roleDefinition)

	def applyInternal(): GrantedRole[A] = transactional() {
		val role = grantedRole.getOrElse(GrantedRole(scope, roleDefinition))

		usercodes.asScala.foreach(role.users.knownType.addUserId)

		permissionsService.saveOrUpdate(role)

		// For each usercode that we've added, clear the cache
		usercodes.asScala.foreach { usercode =>
			permissionsService.clearCachesForUser((usercode, classTag[A]))
		}

		role
	}

}

trait GrantRoleCommandValidation extends SelfValidating {
	self: GrantRoleCommandState[_ <: PermissionsTarget] with SecurityServiceComponent with UserLookupComponent =>

	def validate(errors: Errors) {
		if (usercodes.asScala.forall { _.isEmptyOrWhitespace }) {
			errors.rejectValue("usercodes", "NotEmpty")
		} else {
			val usercodeValidator = new UsercodeListValidator(usercodes, "usercodes") {
				override def alreadyHasCode: Boolean = usercodes.asScala.exists { u => grantedRole.exists(_.users.knownType.includesUserId(u)) }
			}
			usercodeValidator.userLookup = userLookup

			usercodeValidator.validate(errors)
		}

		// Ensure that the current user can delegate everything that they're trying to grant permissions for
		if (roleDefinition == null) {
			errors.rejectValue("roleDefinition", "NotEmpty")
		} else {
			if (!roleDefinition.isAssignable) errors.rejectValue("roleDefinition", "permissions.roleDefinition.notAssignable")
			val user = RequestInfo.fromThread.get.user

			val permissionsToAdd = roleDefinition.allPermissions(Some(scope)).keys
			val deniedPermissions = permissionsToAdd.filterNot(securityService.canDelegate(user,_,scope))
			if (deniedPermissions.nonEmpty && (!user.god)) {
				errors.rejectValue("roleDefinition", "permissions.cantGiveWhatYouDontHave", Array(deniedPermissions.map { _.description }.mkString("\n"), scope),"")
			}
		}
	}
}

trait GrantRoleCommandState[A <: PermissionsTarget] {
	self: PermissionsServiceComponent =>

	def scope: A
	var roleDefinition: RoleDefinition = _
	var usercodes: JList[String] = JArrayList()

	def grantedRole: Option[GrantedRole[A]]
}

trait GrantRoleCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: GrantRoleCommandState[_ <: PermissionsTarget] =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.RolesAndPermissions.Create, mandatory(scope))
	}
}

trait GrantRoleCommandDescription[A <: PermissionsTarget] extends Describable[GrantedRole[A]] {
	self: GrantRoleCommandState[A] =>

	def describe(d: Description): Unit = d.properties(
		"scope" -> (scope.getClass.getSimpleName + "[" + scope.id + "]"),
		"usercodes" -> usercodes.asScala.mkString(","),
		"roleDefinition" -> roleDefinition.getName)
}



