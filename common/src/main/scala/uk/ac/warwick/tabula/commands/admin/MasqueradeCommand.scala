package uk.ac.warwick.tabula.commands.admin

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description, ReadOnly, SelfValidating}
import uk.ac.warwick.tabula.helpers.{FoundUser, NoUser}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.web.Cookie

object MasqueradeCommand {
	def apply(user: CurrentUser) =
		new MasqueradeCommandInternal(user)
				with MasqueradeCommandDescription
				with MasqueradeCommandValidation
				with AutowiringUserLookupComponent
				with AutowiringSecurityServiceComponent
				with AutowiringProfileServiceComponent
				with AutowiringModuleAndDepartmentServiceComponent
				with ComposableCommand[Option[Cookie]]
				with ReadOnly
				with PubliclyVisiblePermissions // Public because we always want to be able to remove the cookie, and we validate our own perms
}

class MasqueradeCommandInternal(val user: CurrentUser) extends CommandInternal[Option[Cookie]] with MasqueradeCommandState {
	self: UserLookupComponent =>

	def applyInternal(): Option[Cookie] = {
		if (action == "remove") Some(newCookie(null))
		else userLookup.getUserByUserId(usercode) match {
			case FoundUser(u) => Some(newCookie(usercode))
			case NoUser(u) => None
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
	self: MasqueradeCommandState with UserLookupComponent with ProfileServiceComponent with SecurityServiceComponent with ModuleAndDepartmentServiceComponent =>

	def validate(errors: Errors) {
		if (action != "remove") {
			userLookup.getUserByUserId(usercode) match {
				case FoundUser(u) =>
					val realUser = if (user.masquerading) new CurrentUser(user.realUser, user.realUser) else user
					val masqueradeDepartments = moduleAndDepartmentService.departmentsWithPermission(realUser, Permissions.Masquerade)
					val masqueradeDepartmentsAndRoots = masqueradeDepartments.flatMap { dept =>
						if (dept.hasParent) Seq(dept, dept.rootDepartment)
						else Seq(dept)
					}

					if (!securityService.can(realUser, Permissions.Masquerade, PermissionsTarget.Global)) {
						profileService.getMemberByUser(u, disableFilter = true, eagerLoad = false) match {
							case Some(profile)
								if securityService.can(realUser, Permissions.Masquerade, profile) ||
									 masqueradeDepartmentsAndRoots.exists(profile.affiliatedDepartments.contains) =>
							case _ => errors.rejectValue("usercode", "masquerade.noPermission")
						}
					}
				case NoUser(user) => errors.rejectValue("usercode", "userId.notfound")
			}
		}
	}
}

trait MasqueradeCommandDescription extends Describable[Option[Cookie]] {
	self: MasqueradeCommandState =>

	def describe(d: Description): Unit = d.properties(
		"usercode" -> usercode,
		"action" -> action
	)
}