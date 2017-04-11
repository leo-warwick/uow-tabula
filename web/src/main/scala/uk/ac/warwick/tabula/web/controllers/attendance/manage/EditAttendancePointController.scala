package uk.ac.warwick.tabula.web.controllers.attendance.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Department, MeetingFormat}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.commands.{Appliable, ComposableCommand, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.AcademicYear
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.attendance.manage._
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringProfileServiceComponent, AutowiringSmallGroupServiceComponent, AutowiringTermServiceComponent}
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringServiceComponent
import uk.ac.warwick.tabula.web.Mav

import collection.JavaConverters._

@Controller
@RequestMapping(Array("/attendance/manage/{department}/{academicYear}/editpoints/{templatePoint}/edit"))
class EditAttendancePointController extends AttendanceController {

	@ModelAttribute("findCommand")
	def findCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		FindPointsCommand(mandatory(department), mandatory(academicYear), None)

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable templatePoint: AttendanceMonitoringPoint
	): EditAttendancePointCommandInternal with ComposableCommand[Seq[AttendanceMonitoringPoint]] with PopulatesEditAttendancePointCommand with AutowiringAttendanceMonitoringServiceComponent with AutowiringTermServiceComponent with AutowiringSmallGroupServiceComponent with AutowiringModuleAndDepartmentServiceComponent with AutowiringProfileServiceComponent with EditAttendancePointValidation with EditAttendancePointDescription with EditAttendancePointPermissions with EditAttendancePointCommandState with SetsFindPointsResultOnCommandState = {
		EditAttendancePointCommand(department, academicYear, templatePoint)
	}

	private def render(department: Department, academicYear: AcademicYear) = {
		Mav("attendance/manage/editpoint",
			"allMeetingFormats" -> MeetingFormat.members,
			"returnTo" -> getReturnTo("")
		).crumbs(
			Breadcrumbs.Manage.HomeForYear(academicYear),
			Breadcrumbs.Manage.DepartmentForYear(department, academicYear),
			Breadcrumbs.Manage.EditPoints(department, academicYear)
		)
	}

	@RequestMapping(method = Array(GET))
	def form(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]] with PopulateOnForm with SetsFindPointsResultOnCommandState,
		@ModelAttribute("findCommand") findCommand: Appliable[FindPointsResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		val findCommandResult = findCommand.apply()
		cmd.setFindPointsResult(findCommandResult)
		cmd.populate()
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
			doApply(cmd, department, academicYear)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("submitConfirm"))
	def submitSkipOverlap(
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
		if (errors.hasErrors && errors.getAllErrors.asScala.exists(_.getCode != "attendanceMonitoringPoint.overlaps")) {
			render(department, academicYear)
		} else {
			doApply(cmd, department, academicYear)
		}
	}

	private def doApply(
		cmd: Appliable[Seq[AttendanceMonitoringPoint]]
			with SetsFindPointsResultOnCommandState with SelfValidating,
		department: Department,
		academicYear: AcademicYear
	) = {
		val points = cmd.apply()
		Redirect(
			getReturnTo(Routes.Manage.editPoints(department, academicYear)),
			"points" -> points.size.toString,
			"actionCompleted" -> "edited"
		)
	}

}
