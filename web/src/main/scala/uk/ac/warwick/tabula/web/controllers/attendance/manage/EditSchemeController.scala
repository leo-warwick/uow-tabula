package uk.ac.warwick.tabula.web.controllers.attendance.manage

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.attendance.manage.EditSchemeCommand
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

@Controller
@RequestMapping(Array("/attendance/manage/{department}/{academicYear}/{scheme}/edit"))
class EditSchemeController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable scheme: AttendanceMonitoringScheme) =
		EditSchemeCommand(mandatory(scheme), user)

	private def render(scheme: AttendanceMonitoringScheme) = {
		Mav("attendance/manage/edit",
			"ManageSchemeMappingParameters" -> ManageSchemeMappingParameters
		).crumbs(
			Breadcrumbs.Manage.HomeForYear(scheme.academicYear),
			Breadcrumbs.Manage.DepartmentForYear(scheme.department, scheme.academicYear)
		)
	}

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable scheme: AttendanceMonitoringScheme,
		@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme] with PopulateOnForm
	): Mav = {
		cmd.populate()
		render(scheme)
	}

	@RequestMapping(method = Array(POST))
	def save(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@PathVariable scheme: AttendanceMonitoringScheme
	): Mav = {
		if (errors.hasErrors) {
			render(scheme)
		} else {
			val scheme = cmd.apply()
			Redirect(Routes.Manage.departmentForYear(scheme.department, scheme.academicYear))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.createAndAddStudents))
	def saveAndEditStudents(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@PathVariable scheme: AttendanceMonitoringScheme
	): Mav = {
		if (errors.hasErrors) {
			render(scheme)
		} else {
			val scheme = cmd.apply()
			Redirect(Routes.Manage.editSchemeStudents(scheme))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.createAndAddPoints))
	def saveAndEditPoints(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@PathVariable scheme: AttendanceMonitoringScheme
	): Mav = {
		if (errors.hasErrors) {
			render(scheme)
		} else {
			val scheme = cmd.apply()
			Redirect(Routes.Manage.editSchemePoints(scheme))
		}
	}
}
