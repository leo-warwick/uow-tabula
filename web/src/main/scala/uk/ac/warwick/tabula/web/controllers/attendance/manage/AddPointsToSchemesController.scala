package uk.ac.warwick.tabula.web.controllers.attendance.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.commands.attendance.manage.{AddPointsToSchemesCommand, AddPointsToSchemesCommandResult}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController

@Controller
@RequestMapping(Array("/attendance/manage/{department}/{academicYear}/addpoints"))
class AddPointsToSchemesController extends AttendanceController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		AddPointsToSchemesCommand(mandatory(department), mandatory(academicYear))

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[AddPointsToSchemesCommandResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam(required = false) points: JInteger
	): Mav = {
		val result = cmd.apply()
		Mav("attendance/manage/addpoints",
			"schemeMaps" -> result,
			"changedSchemes" -> (result.weekSchemes.count(_._2) + result.dateSchemes.count(_._2)),
			"schemesParam" -> (result.weekSchemes.filter(_._2).keys ++ result.dateSchemes.filter(_._2).keys)
				.map(s => s"findSchemes=${s.id}").mkString("&"),
			"newPoints" -> Option(points).getOrElse(0)
		).crumbs(
			Breadcrumbs.Manage.HomeForYear(academicYear),
			Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
		).secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.Manage.addPointsToExistingSchemes(department, year)): _*)
	}

}
