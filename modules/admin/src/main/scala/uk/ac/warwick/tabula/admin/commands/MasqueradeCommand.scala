package uk.ac.warwick.tabula.admin.commands

import uk.ac.warwick.tabula.web.Cookie
import uk.ac.warwick.tabula.commands.{SelfValidating, CommandInternal, Describable, ComposableCommand, ReadOnly, Description}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.helpers.NoUser
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.services._
import org.springframework.validation.Errors

object MasqueradeCommand {
	def apply(user: CurrentUser) =
		new MasqueradeCommandInternal(user)
				with MasqueradeCommandDescription
				with MasqueradeCommandValidation
				with AutowiringUserLookupComponent
				with AutowiringSecurityServiceComponent
				with AutowiringProfileServiceComponent
				with ComposableCommand[Option[Cookie]]
				with ReadOnly
				with PubliclyVisiblePermissions // Public because we always want to be able to remove the cookie, and we validate our own perms
}

class MasqueradeCommandInternal(val user: CurrentUser) extends CommandInternal[Option[Cookie]] with MasqueradeCommandState {
	self: UserLookupComponent =>

	def applyInternal() = {
		if (action == "remove") Some(newCookie(null))
		else userLookup.getUserByUserId(usercode) match {
			case FoundUser(user) => Some(newCookie(usercode))
			case NoUser(user) => None
		}
	}

	private def newCookie(usercode: String) = new Cookie(
		name = CurrentUser.masqueradeCookie,
		value = usercode,
		path = "/")

}

trait MasqueradeCommandState {
	def user: CurrentUser

	var usercode: String = _
	var action: String = _
}

trait MasqueradeCommandValidation extends SelfValidating {
	self: MasqueradeCommandState with UserLookupComponent with ProfileServiceComponent with SecurityServiceComponent =>

	def validate(errors: Errors) {
		if (action != "remove") {
			userLookup.getUserByUserId(usercode) match {
				case FoundUser(u) => {
					val realUser = new CurrentUser(user.realUser, user.realUser)

					if (!securityService.can(realUser, Permissions.Masquerade, PermissionsTarget.Global)) {
						profileService.getMemberByUser(u, true, false) match {
							case Some(profile) if securityService.can(realUser, Permissions.Masquerade, profile) =>
							case _ => errors.rejectValue("usercode", "masquerade.noPermission")
						}
					}
				}
				case NoUser(user) => errors.rejectValue("usercode", "userId.notfound")
			}
		}
	}
}

trait MasqueradeCommandDescription extends Describable[Option[Cookie]] {
	self: MasqueradeCommandState =>

	def describe(d: Description) = d.properties(
		"usercode" -> usercode,
		"action" -> action
	)
}