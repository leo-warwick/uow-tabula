package uk.ac.warwick.tabula.web.controllers.attendance.view

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.view.{FilterMonitoringPointsCommand, FilterMonitoringPointsCommandResult, RecordMonitoringPointCommand, SetFilterPointsResultOnRecordMonitoringPointCommand}
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.web.controllers.attendance.{AttendanceController, HasMonthNames}
import uk.ac.warwick.tabula.commands.{Appliable, FiltersStudentsBase, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/attendance/view/{department}/{academicYear}/points/{templatePoint}/record"))
class RecordMonitoringPointController extends AttendanceController with HasMonthNames {

	@ModelAttribute("filterCommand")
	def filterCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		FilterMonitoringPointsCommand(mandatory(department), mandatory(academicYear), user)

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable templatePoint: AttendanceMonitoringPoint) =
		RecordMonitoringPointCommand(mandatory(department), mandatory(academicYear), mandatory(templatePoint), user)

	@RequestMapping(method = Array(GET))
	def form(
		@ModelAttribute("filterCommand") filterCommand: Appliable[FilterMonitoringPointsCommandResult] with FiltersStudentsBase,
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringCheckpoint]]
			with PopulateOnForm with SetFilterPointsResultOnRecordMonitoringPointCommand,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable templatePoint: AttendanceMonitoringPoint
	): Mav = {
		val filterResult = filterCommand.apply()
		cmd.setFilteredPoints(filterResult)
		cmd.populate()
		render(filterCommand, department, academicYear, templatePoint)
	}

	private def render(
		filterCommand: FiltersStudentsBase,
		department: Department,
		academicYear: AcademicYear,
		templatePoint: AttendanceMonitoringPoint
	) = {
		Mav("attendance/pointrecord",
			"uploadUrl" -> Routes.View.pointRecordUpload(department, academicYear, templatePoint, filterCommand.serializeFilter),
			"returnTo" -> getReturnTo(Routes.View.points(department, academicYear))
		).crumbs(
			Breadcrumbs.View.HomeForYear(academicYear),
			Breadcrumbs.View.DepartmentForYear(department, academicYear),
			Breadcrumbs.View.Points(department, academicYear)
		)
	}

	@RequestMapping(method = Array(POST))
	def post(
		@ModelAttribute("filterCommand") filterCommand: Appliable[FilterMonitoringPointsCommandResult] with FiltersStudentsBase,
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringCheckpoint]]
			with SetFilterPointsResultOnRecordMonitoringPointCommand with SelfValidating,
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable templatePoint: AttendanceMonitoringPoint
	): Mav = {
		val filterResult = filterCommand.apply()
		cmd.setFilteredPoints(filterResult)
		cmd.validate(errors)
		if (errors.hasErrors) {
			render(filterCommand, department, academicYear, templatePoint)
		} else {
			cmd.apply()
			Redirect(Routes.View.points(department, academicYear))
		}
	}

}