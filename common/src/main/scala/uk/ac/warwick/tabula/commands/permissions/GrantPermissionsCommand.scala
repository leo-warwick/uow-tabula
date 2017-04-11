package uk.ac.warwick.tabula.commands.permissions

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.commands.{Appliable, ComposableCommand, CommandInternal, Describable}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent, AutowiringSecurityServiceComponent, SecurityServiceComponent}
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.validators.UsercodeListValidator
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.RequestInfo
import scala.reflect._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object GrantPermissionsCommand {
	def apply[A <: PermissionsTarget : ClassTag](scope: A): Appliable[GrantedPermission[A]] with GrantPermissionsCommandState[A] =
		new GrantPermissionsCommandInternal(scope)
			with ComposableCommand[GrantedPermission[A]]
			with GrantPermissionsCommandPermissions
			with GrantPermissionsCommandValidation
			with GrantPermissionsCommandDescription[A]
			with AutowiringPermissionsServiceComponent
			with AutowiringSecurityServiceComponent
			with AutowiringUserLookupComponent
}

class GrantPermissionsCommandInternal[A <: PermissionsTarget : ClassTag](val scope: A)
	extends CommandInternal[GrantedPermission[A]] with GrantPermissionsCommandState[A] {

	self: PermissionsServiceComponent with UserLookupComponent =>

	lazy val grantedPermission: Option[GrantedPermission[A]] = permissionsService.getGrantedPermission(scope, permission, overrideType)

	def applyInternal(): GrantedPermission[A] = transactional() {
		val granted = grantedPermission.getOrElse(GrantedPermission(scope, permission, overrideType))

		usercodes.asScala.foreach(granted.users.knownType.addUserId)

		permissionsService.saveOrUpdate(granted)

		// For each usercode that we've added, clear the cache
		usercodes.asScala.foreach { usercode =>
			permissionsService.clearCachesForUser((usercode, classTag[A]))
		}

		granted
	}

}

trait GrantPermissionsCommandValidation extends SelfValidating {
	self: GrantPermissionsCommandState[_ <: PermissionsTarget] with SecurityServiceComponent =>

	def validate(errors: Errors) {
		if (usercodes.asScala.forall { _.isEmptyOrWhitespace }) {
			errors.rejectValue("usercodes", "NotEmpty")
		} else {
			grantedPermission.map { _.users }.foreach { users =>
				val usercodeValidator = new UsercodeListValidator(usercodes, "usercodes") {
					override def alreadyHasCode: Boolean = usercodes.asScala.exists { users.knownType.includesUserId }
				}

				usercodeValidator.validate(errors)
			}
		}

		// Ensure that the current user can do everything that they're trying to grant permissions for
		val user = RequestInfo.fromThread.get.user

		if (permission == null) errors.rejectValue("permission", "NotEmpty")
		else if (!user.sysadmin && !securityService.canDelegate(user, permission, scope)) {
			errors.rejectValue("permission", "permissions.cantGiveWhatYouDontHave", Array(permission.description, scope), "")
		}
	}
}

trait GrantPermissionsCommandState[A <: PermissionsTarget] {
	self: PermissionsServiceComponent =>

	def scope: A
	var permission: Permission = _
	var usercodes: JList[String] = JArrayList()
	var overrideType: Boolean = _

	def grantedPermission: Option[GrantedPermission[A]]
}

trait GrantPermissionsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: GrantPermissionsCommandState[_ <: PermissionsTarget] =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.RolesAndPermissions.Create, mandatory(scope))
	}
}

trait GrantPermissionsCommandDescription[A <: PermissionsTarget] extends Describable[GrantedPermission[A]] {
	self: GrantPermissionsCommandState[A] =>

	def describe(d: Description): Unit = d.properties(
		"scope" -> (scope.getClass.getSimpleName + "[" + scope.id + "]"),
		"usercodes" -> usercodes.asScala.mkString(","),
		"permission" -> permission.getName,
		"overrideType" -> overrideType)
}


