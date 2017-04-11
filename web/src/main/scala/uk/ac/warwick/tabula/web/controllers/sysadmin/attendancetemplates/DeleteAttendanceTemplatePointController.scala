package uk.ac.warwick.tabula.web.controllers.sysadmin.attendancetemplates

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringTemplate, AttendanceMonitoringTemplatePoint}
import uk.ac.warwick.tabula.commands.sysadmin.attendancetemplates.DeleteAttendanceTemplatePointCommand
import uk.ac.warwick.tabula.web.controllers.sysadmin.{BaseSysadminController, SysadminBreadcrumbs}
import uk.ac.warwick.tabula.sysadmin.web.Routes
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(value = Array("/sysadmin/attendancetemplates/{template}/points/{point}/delete"))
class DeleteAttendanceTemplatePointController extends BaseSysadminController {

	@ModelAttribute("command")
	def command(@PathVariable point: AttendanceMonitoringTemplatePoint) = DeleteAttendanceTemplatePointCommand(mandatory(point))

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: Appliable[Unit], @PathVariable template: AttendanceMonitoringTemplate): Mav = {
		render(template)
	}

	private def render(template: AttendanceMonitoringTemplate) = {
		Mav("sysadmin/attendancetemplates/deletepoint").crumbs(
			SysadminBreadcrumbs.AttendanceTemplates.Home,
			SysadminBreadcrumbs.AttendanceTemplates.Edit(template)
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@ModelAttribute("command") cmd: Appliable[Unit],
		@PathVariable template: AttendanceMonitoringTemplate
	): Mav = {
		cmd.apply()
		Redirect(Routes.AttendanceTemplates.edit(template))
	}

}
