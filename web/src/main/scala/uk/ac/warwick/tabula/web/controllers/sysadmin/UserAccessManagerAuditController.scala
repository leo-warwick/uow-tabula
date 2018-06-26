package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{GetMapping, PostMapping, RequestMapping}
import uk.ac.warwick.tabula.commands.sysadmin.UserAccessManagerAuditCommand
import uk.ac.warwick.tabula.data.model.notifications.{UAMAuditChaserNotification, UAMAuditNotification}
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/sysadmin/uam-audit"))
class UserAccessManagerAuditController extends BaseSysadminController {
	@GetMapping
	def home: Mav = Mav("sysadmin/uam-audit")

	@PostMapping(params = Array("notification=first"))
	def sendFirstNotification(): Mav = {
		UserAccessManagerAuditCommand.apply(new UAMAuditNotification).apply()
		home.addObjects("success" -> true)
	}

	@PostMapping(params = Array("notification=second"))
	def sendSecondNotification(): Mav = {
		UserAccessManagerAuditCommand.apply(new UAMAuditChaserNotification).apply()

		home.addObjects("success" -> true)
	}
}
