package uk.ac.warwick.tabula.web.controllers.attendance.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.commands.attendance.manage.{DeleteSchemeCommand, DeleteSchemeCommandState}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import javax.validation.Valid

import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/attendance/manage/{department}/{academicYear}/{scheme}/delete"))
class DeleteSchemeController extends AttendanceController {

	validatesSelf[SelfValidating]
	type DeleteSchemeCommand = Appliable[AttendanceMonitoringScheme] with DeleteSchemeCommandState

	@ModelAttribute("command")
	def command(@PathVariable scheme: AttendanceMonitoringScheme): DeleteSchemeCommand =
		DeleteSchemeCommand(mandatory(scheme))

	@RequestMapping(method = Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: DeleteSchemeCommand): Mav = {
		Mav("attendance/manage/delete")
			.crumbs(
				Breadcrumbs.Manage.HomeForYear(cmd.scheme.academicYear),
				Breadcrumbs.Manage.DepartmentForYear(cmd.scheme.department, cmd.scheme.academicYear)
			)
	}

	@RequestMapping(method = Array(POST), params = Array("submit"))
	def submit(
		@Valid @ModelAttribute("command") cmd: DeleteSchemeCommand,
		errors: Errors
		): Mav = {
			if (errors.hasErrors) {
				form(cmd)
			} else {
				val scheme = cmd.apply()
				Redirect(Routes.Manage.departmentForYear(scheme.department, scheme.academicYear))
			}
	}

	@RequestMapping(method = Array(POST), params = Array("cancel"))
	def cancel(@PathVariable scheme: AttendanceMonitoringScheme): Mav = {
		Redirect(Routes.Manage.departmentForYear(scheme.department, scheme.academicYear))
	}

}
