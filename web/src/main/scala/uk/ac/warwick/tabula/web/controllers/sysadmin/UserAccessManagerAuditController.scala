package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{GetMapping, PostMapping, RequestMapping}
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/sysadmin/uam-audit"))
class UserAccessManagerAuditController extends BaseSysadminController {
	@GetMapping
	def home: Mav = Mav("sysadmin/uam-audit")

	@PostMapping(params = Array("notification=first"))
	def sendFirstNotification(): Mav = {
		home.addObjects("success" -> true)
	}

	@PostMapping(params = Array("notification=second"))
	def sendSecondNotification(): Mav = {
		home.addObjects("success" -> true)
	}
}
