package uk.ac.warwick.tabula.web.controllers.attendance.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.commands.{Appliable, ComposableCommand, SelfValidating}
import uk.ac.warwick.tabula.AcademicYear
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.attendance.manage._
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringServiceComponent
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/attendance/manage/{department}/{academicYear}/editpoints/{templatePoint}/delete"))
class DeleteAttendancePointController extends AttendanceController {

	@ModelAttribute("findCommand")
	def findCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		FindPointsCommand(department, academicYear, None)

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable templatePoint: AttendanceMonitoringPoint
	): DeleteAttendancePointCommandInternal with ComposableCommand[Seq[AttendanceMonitoringPoint]] with AutowiringAttendanceMonitoringServiceComponent with AutowiringProfileServiceComponent with DeleteAttendancePointValidation with DeleteAttendancePointDescription with DeleteAttendancePointPermissions with DeleteAttendancePointCommandState with SetsFindPointsResultOnCommandState = {
		DeleteAttendancePointCommand(mandatory(department), mandatory(templatePoint))
	}

	private def render(department: Department, academicYear: AcademicYear) = {
		Mav("attendance/manage/deletepoint",
			"returnTo" -> getReturnTo("")
		).crumbs(
			Breadcrumbs.Manage.HomeForYear(academicYear),
			Breadcrumbs.Manage.DepartmentForYear(department, academicYear),
			Breadcrumbs.Manage.EditPoints(department, academicYear)
		)
	}

	@RequestMapping(method = Array(GET))
	def form(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]] with SetsFindPointsResultOnCommandState,
		@ModelAttribute("findCommand") findCommand: Appliable[FindPointsResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		val findCommandResult = findCommand.apply()
		cmd.setFindPointsResult(findCommandResult)
		render(department, academicYear)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]]
			with SetsFindPointsResultOnCommandState with SelfValidating,
		errors: Errors,
		@ModelAttribute("findCommand") findCommand: Appliable[FindPointsResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		val findCommandResult = findCommand.apply()
		cmd.setFindPointsResult(findCommandResult)
		cmd.validate(errors)
		if (errors.hasErrors) {
			render(department, academicYear)
		} else {
			val points = cmd.apply()
			Redirect(
				getReturnTo(Routes.Manage.editPoints(department, academicYear)),
				"points" -> points.size.toString,
				"actionCompleted" -> "deleted"
			)
		}
	}

}
