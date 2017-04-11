package uk.ac.warwick.tabula.web.controllers.sysadmin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.Mav

object EmergencyMessageCommand {
	type Command = Appliable[EmergencyMessage] with SelfValidating with PopulateOnForm

	def apply(): Command =
		new EmergencyMessageCommandInternal
			with ComposableCommand[EmergencyMessage]
			with EmergencyMessageCommandPopulation
			with EmergencyMessageCommandValidation
			with ManageEmergencyMessagePermissions
			with AutowiringEmergencyMessageServiceComponent
			with AutowiringSettingsSyncQueueComponent
			with ReadOnly with Unaudited
}

trait EmergencyMessageCommandRequest {

	var enable: Boolean = _
	var message: String = _

}

class EmergencyMessageCommandInternal extends CommandInternal[EmergencyMessage] with EmergencyMessageCommandRequest {
	self: EmergencyMessageServiceComponent with SettingsSyncQueueComponent =>

	override def applyInternal(): EmergencyMessage = {
		if (!enable) {
			message = null
		}
		emergencyMessageService.message = Option(message)
		if (enable) emergencyMessageService.enable
		else emergencyMessageService.disable

		val msg = new EmergencyMessage(enable, Option(message))

		settingsSyncQueue.send(msg)

		msg
	}

}

trait EmergencyMessageCommandPopulation extends PopulateOnForm {
	self: EmergencyMessageCommandRequest with EmergencyMessageServiceComponent =>

	override def populate(): Unit = {
		enable = emergencyMessageService.enabled
		message = emergencyMessageService.message.orNull
	}
}

trait EmergencyMessageCommandValidation extends SelfValidating {

	override def validate(errors: Errors): Unit = {}

}

trait ManageEmergencyMessagePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.ManageEmergencyMessage)
	}

}

@Controller
@RequestMapping(Array("/sysadmin/emergencymessage"))
class EmergencyMessageController extends BaseSysadminController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command") def cmd: EmergencyMessageCommand.Command = EmergencyMessageCommand()

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute("command") form: EmergencyMessageCommand.Command, errors: Errors): Mav =
		Mav("sysadmin/emergency-message").noLayoutIf(ajax)

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") form: EmergencyMessageCommand.Command, errors: Errors): Mav = {
		if (errors.hasErrors)
			showForm(form, errors)
		else {
			form.apply()
			Redirect("/sysadmin")
		}
	}
}
