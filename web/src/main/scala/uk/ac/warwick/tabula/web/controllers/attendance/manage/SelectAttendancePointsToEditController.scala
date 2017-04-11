package uk.ac.warwick.tabula.web.controllers.attendance.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.controllers.attendance.{AttendanceController, HasMonthNames}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceMonitoringPointType, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.attendance.manage.FindPointsCommand
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.attendance.manage.FindPointsResult
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController

@Controller
@RequestMapping(Array("/attendance/manage/{department}/{academicYear}/editpoints"))
class SelectAttendancePointsToEditController extends AttendanceController with HasMonthNames
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	@Autowired var attendanceMonitoringService: AttendanceMonitoringService = _

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@ModelAttribute("findCommand")
	def findCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		FindPointsCommand(mandatory(department), mandatory(academicYear), None)

	@ModelAttribute("allSchemes")
	def allSchemes(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): Seq[AttendanceMonitoringScheme] =
		attendanceMonitoringService.listSchemes(department, academicYear)

	@RequestMapping
	def home(
		@ModelAttribute("findCommand") findCommand: Appliable[FindPointsResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam(required = false) points: JInteger,
		@RequestParam(required = false) actionCompleted: String
	): Mav = {
		val findCommandResult = findCommand.apply()
		Mav("attendance/manage/editpoints",
			"findResult" -> findCommandResult,
			"allTypes" -> AttendanceMonitoringPointType.values,
			"allStyles" -> AttendanceMonitoringPointStyle.values,
			"newPoints" -> Option(points).getOrElse(0),
			"actionCompleted" -> actionCompleted
		).crumbs(
			Breadcrumbs.Manage.HomeForYear(academicYear),
			Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
		).secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.Manage.editPoints(department, year)): _*)

	}

}
