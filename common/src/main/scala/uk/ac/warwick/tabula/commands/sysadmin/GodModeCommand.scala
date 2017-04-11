package uk.ac.warwick.tabula.commands.sysadmin

import uk.ac.warwick.tabula.web.Cookie
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.ReadOnly
import org.hibernate.validator.constraints.NotEmpty
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.NoUser
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions._

class GodModeCommand extends Command[Option[Cookie]] with ReadOnly {

	PermissionCheck(Permissions.GodMode)

	var action: String = _

	def applyInternal(): Some[Cookie] = {
		if (action == "remove") Some(newCookie(false))
		else Some(newCookie(true))
	}

	private def newCookie(isGod: Boolean) = new Cookie(
		name = CurrentUser.godModeCookie,
		value = isGod.toString,
		path = "/")

	def describe(d: Description): Unit = d.properties(
		"action" -> action
	)

}