package uk.ac.warwick.tabula.web.controllers.sysadmin.attendancetemplates

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplate
import uk.ac.warwick.tabula.commands.sysadmin.attendancetemplates.EditAttendanceTemplateCommand
import uk.ac.warwick.tabula.web.controllers.sysadmin.{BaseSysadminController, SysadminBreadcrumbs}
import uk.ac.warwick.tabula.sysadmin.web.Routes
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(value = Array("/sysadmin/attendancetemplates/{template}/edit"))
class EditAttendanceTemplateController extends BaseSysadminController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable template: AttendanceMonitoringTemplate) = EditAttendanceTemplateCommand(mandatory(template))

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringTemplate] with PopulateOnForm): Mav = {
		cmd.populate()
		render
	}

	private def render = {
		Mav("sysadmin/attendancetemplates/edit").crumbs(
			SysadminBreadcrumbs.AttendanceTemplates.Home
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringTemplate],
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
