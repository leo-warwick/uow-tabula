package uk.ac.warwick.tabula.web.controllers.sysadmin

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.sysadmin.UserAccessManagerAuditCommand
import uk.ac.warwick.tabula.data.model.notifications.{UAMAuditChaserNotification, UAMAuditNotification}
import uk.ac.warwick.tabula.roles.UserAccessMgrRoleDefinition
import uk.ac.warwick.tabula.services.permissions.{PermissionsService, PermissionsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(Array("/sysadmin/uam-audit"))
class UserAccessManagerAuditController extends BaseSysadminController with PermissionsServiceComponent {

	override def permissionsService: PermissionsService = Wire[PermissionsService]

	@GetMapping
	def home: Mav = Mav("sysadmin/uam-audit").addObjects("uamUsercodes" -> uams.map(_.getUserId))

	def success: Mav = home.addObjects("success" -> true)

	def error: Mav = home.addObjects("error" -> true)

	def sendNotification(notification: UAMAuditNotification): Mav = {
		UserAccessManagerAuditCommand(notification).apply()
		success
	}

	def uams: Seq[User] = {
		permissionsService
			.getAllGrantedRolesForDefinition(UserAccessMgrRoleDefinition)
			.flatMap(_.users.users)
			.distinct
	}

	@PostMapping
	def onSubmit(@RequestParam(value = "notification", required = false) choice: String): Mav = {
		(choice match {
			case "first" => Some(new UAMAuditNotification)
			case "second" => Some(new UAMAuditChaserNotification)
			case _ => None
		}).map(sendNotification).getOrElse(error)
	}

}
