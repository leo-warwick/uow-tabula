package uk.ac.warwick.tabula.web.controllers.sysadmin

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.validators.WithinYears
import uk.ac.warwick.tabula.web.Mav

object MaintenanceModeCommand {
	type Command = Appliable[MaintenanceModeMessage] with SelfValidating with PopulateOnForm

	val DefaultMaintenanceMinutes = 30

	def apply(): Command =
		new MaintenanceModeCommandInternal
			with ComposableCommand[MaintenanceModeMessage]
			with MaintenanceModeCommandPopulation
			with MaintenanceModeCommandValidation
			with ManageMaintenanceModePermissions
			with AutowiringMaintenanceModeServiceComponent
			with AutowiringSettingsSyncQueueComponent
			with ReadOnly with Unaudited
}

trait MaintenanceModeCommandRequest {

	var enable: Boolean = _

	@WithinYears(maxFuture = 1, maxPast = 1) @DateTimeFormat(pattern = DateFormats.DateTimePickerPattern)
	var until: DateTime = _

	var message: String = _

}

class MaintenanceModeCommandInternal extends CommandInternal[MaintenanceModeMessage] with MaintenanceModeCommandRequest {
	self: MaintenanceModeServiceComponent with SettingsSyncQueueComponent =>

	override def applyInternal(): MaintenanceModeMessage = {
		if (!enable) {
			message = null
			until = null
		}
		maintenanceModeService.message = Option(message)
		maintenanceModeService.until = Option(until)
		if (enable) maintenanceModeService.enable
		else maintenanceModeService.disable

		val msg = new MaintenanceModeMessage(enable, Option(until), Option(message))

		settingsSyncQueue.send(msg)

		msg
	}

}

trait MaintenanceModeCommandPopulation extends PopulateOnForm {
	self: MaintenanceModeCommandRequest with MaintenanceModeServiceComponent =>

	override def populate(): Unit = {
		enable = maintenanceModeService.enabled

		until = maintenanceModeService.until.getOrElse(DateTime.now.plusMinutes(MaintenanceModeCommand.DefaultMaintenanceMinutes))

		message = maintenanceModeService.message.orNull
	}
}

trait MaintenanceModeCommandValidation extends SelfValidating {

	override def validate(errors: Errors): Unit = {}

}

trait ManageMaintenanceModePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.ManageMaintenanceMode)
	}

}

@Controller
@RequestMapping(Array("/sysadmin/maintenance"))
class MaintenanceModeController extends BaseSysadminController {
	validatesSelf[SelfValidating]

	@ModelAttribute("maintenanceModeCommand") def cmd: MaintenanceModeCommand.Command = MaintenanceModeCommand()

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute("maintenanceModeCommand") form: MaintenanceModeCommand.Command, errors: Errors): Mav =
		Mav("sysadmin/maintenance").noLayoutIf(ajax)

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("maintenanceModeCommand") form: MaintenanceModeCommand.Command, errors: Errors): Mav = {
		if (errors.hasErrors)
			showForm(form, errors)
		else {
			form.apply()
			Redirect("/sysadmin")
		}
	}
}