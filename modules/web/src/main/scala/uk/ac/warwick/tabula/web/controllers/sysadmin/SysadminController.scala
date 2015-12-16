package uk.ac.warwick.tabula.web.controllers.sysadmin

import javax.servlet.http.HttpServletResponse
import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.commands.sysadmin.GodModeCommand
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.validators.WithinYears
import uk.ac.warwick.tabula.web.Cookies._
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.userlookup.UserLookupInterface

/**
 * Screens for application sysadmins, i.e. the web development and content teams.
 */

abstract class BaseSysadminController extends BaseController with SysadminBreadcrumbs {
	var moduleService = Wire.auto[ModuleAndDepartmentService]
	var userLookup = Wire.auto[UserLookupInterface]

	def redirectToHome = Redirect("/sysadmin/")
}

/* Just a pojo to bind to for blank form; actually used in scheduling */
class BlankForm {
	@WithinYears(maxPast = 20) @DateTimeFormat(pattern = DateFormats.DateTimePicker)
	var from: DateTime = _
	var deptCode: String = _
}

@Controller
@RequestMapping(Array("/sysadmin"))
class SysadminController extends BaseSysadminController with AutowiringMaintenanceModeServiceComponent with AutowiringEmergencyMessageServiceComponent {

	@ModelAttribute("blankForm") def blankForm = new BlankForm

	@RequestMapping
	def home = Mav("sysadmin/home")
		.addObjects(
			"maintenanceModeEnabled" -> maintenanceModeService.enabled,
			"emergencyMessageEnabled" -> emergencyMessageService.enabled
		)
}

@Controller
@RequestMapping(Array("/sysadmin/god"))
class GodModeController extends BaseSysadminController {

	@RequestMapping
	def submit(@Valid cmd: GodModeCommand, response: HttpServletResponse) = {
		for (cookie <- cmd.apply()) response.addCookie(cookie)
		redirectToHome
	}

}
