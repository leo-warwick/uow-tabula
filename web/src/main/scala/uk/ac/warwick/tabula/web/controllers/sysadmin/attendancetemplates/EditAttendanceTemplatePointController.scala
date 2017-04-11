package uk.ac.warwick.tabula.web.controllers.sysadmin.attendancetemplates

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringTemplate, AttendanceMonitoringTemplatePoint}
import uk.ac.warwick.tabula.commands.sysadmin.attendancetemplates.EditAttendanceTemplatePointCommand
import uk.ac.warwick.tabula.web.controllers.sysadmin.{BaseSysadminController, SysadminBreadcrumbs}
import uk.ac.warwick.tabula.sysadmin.web.Routes
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(value = Array("/sysadmin/attendancetemplates/{template}/points/{point}/edit"))
class EditAttendanceTemplatePointController extends BaseSysadminController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable point: AttendanceMonitoringTemplatePoint) = EditAttendanceTemplatePointCommand(mandatory(point))

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringTemplatePoint] with PopulateOnForm, @PathVariable template: AttendanceMonitoringTemplate): Mav = {
		cmd.populate()
		render(template)
	}

	private def render(template: AttendanceMonitoringTemplate) = {
		Mav("sysadmin/attendancetemplates/editpoint").crumbs(
			SysadminBreadcrumbs.AttendanceTemplates.Home,
			SysadminBreadcrumbs.AttendanceTemplates.Edit(template)
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringTemplatePoint],
		errors: Errors,
		@PathVariable template: AttendanceMonitoringTemplate
	): Mav = {
		if (errors.hasErrors) {
			render(template)
		} else {
			cmd.apply()
			Redirect(Routes.AttendanceTemplates.edit(template))
		}
	}

	@RequestMapping(method = Array(POST), params = Array("cancel"))
	def cancel(@PathVariable template: AttendanceMonitoringTemplate): Mav = {
		Redirect(Routes.AttendanceTemplates.edit(template))
	}

}
