package uk.ac.warwick.tabula.web.controllers.attendance.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.manage.CreateMonitoringSchemeCommand
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import javax.validation.Valid

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/attendance/manage/{department}/{academicYear}/new"))
class CreateMonitoringSchemeController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		CreateMonitoringSchemeCommand(mandatory(department), mandatory(academicYear))

	@RequestMapping(method = Array(GET, HEAD))
	def form(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): Mav = {
		Mav("attendance/manage/new", "ManageSchemeMappingParameters" -> ManageSchemeMappingParameters)
			.crumbs(
				Breadcrumbs.Manage.HomeForYear(academicYear),
				Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
			)
	}

	@RequestMapping(method = Array(POST))
	def post(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			form(department, academicYear)
		} else {
			cmd.apply()
			Redirect(Routes.Manage.departmentForYear(department, academicYear))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.createAndAddStudents))
	def postAndAddStudents(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			form(department, academicYear)
		} else {
			val scheme = cmd.apply()
			Redirect(Routes.Manage.addStudentsToScheme(scheme))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.createAndAddPoints))
	def postAndAddPoints(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			form(department, academicYear)
		} else {
			val scheme = cmd.apply()
			Redirect(Routes.Manage.addPointsToNewScheme(scheme))
		}
	}

}
