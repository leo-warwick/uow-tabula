package uk.ac.warwick.tabula.web.controllers.sysadmin.attendancetemplates

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplate
import uk.ac.warwick.tabula.commands.sysadmin.attendancetemplates.DeleteAttendanceTemplateCommand
import uk.ac.warwick.tabula.web.controllers.sysadmin.{BaseSysadminController, SysadminBreadcrumbs}
import uk.ac.warwick.tabula.sysadmin.web.Routes
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(value = Array("/sysadmin/attendancetemplates/{template}/delete"))
class DeleteAttendanceTemplateController extends BaseSysadminController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable template: AttendanceMonitoringTemplate) = DeleteAttendanceTemplateCommand(mandatory(template))

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: Appliable[Unit]): Mav = {
		render
	}

	private def render = {
		Mav("sysadmin/attendancetemplates/delete").crumbs(
			SysadminBreadcrumbs.AttendanceTemplates.Home
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Unit],
		errors: Errors
	): Mav = {
		if (errors.hasErrors) {
			render
		} else {
			cmd.apply()
			Redirect(Routes.AttendanceTemplates.home)
		}
	}

}
