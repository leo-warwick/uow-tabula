package uk.ac.warwick.tabula.web.controllers.admin.department

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.admin.department.NotificationSettingsCommand
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController
import uk.ac.warwick.tabula.web.controllers.admin.AdminController
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department

@Controller
@RequestMapping(Array("/admin/department/{department}/settings/notification"))
class NotificationSettingsController extends AdminController
	with DepartmentScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent {
	
	type NotificationSettingsCommand = Appliable[Department]

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department): NotificationSettingsCommand = NotificationSettingsCommand(mandatory(department))

	override val departmentPermission: Permission = Permissions.Department.ManageDisplaySettings

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@RequestMapping(method = Array(GET, HEAD))
	def form(@PathVariable department: Department, @ModelAttribute("command") cmd: NotificationSettingsCommand): Mav = {
		Mav("admin/notification-settings", "returnTo" -> getReturnTo(Routes.admin.department(department))).crumbs(
			Breadcrumbs.Department(department)
		)
	}

	@RequestMapping(method = Array(POST))
	def saveSettings(
		@Valid @ModelAttribute("command") cmd: NotificationSettingsCommand,
		errors: Errors,
		@PathVariable department: Department
	): Mav = {
		if (errors.hasErrors) {
			form(department, cmd)
		} else {
			cmd.apply()
			Redirect(Routes.admin.department(department))
		}
	}
}