package uk.ac.warwick.tabula.web.controllers.attendance.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.attendance.view.{FilterMonitoringPointsCommand, FilterMonitoringPointsCommandResult}
import uk.ac.warwick.tabula.web.controllers.attendance.{AttendanceController, HasMonthNames}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}

@Controller
@RequestMapping(Array("/attendance/view/{department}/{academicYear}/points"))
class ViewMonitoringPointsController extends AttendanceController with HasMonthNames
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent
	with DepartmentScopedController with AutowiringModuleAndDepartmentServiceComponent {

	override val departmentPermission: Permission = Permissions.MonitoringPoints.View

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("filterCommand")
	def filterCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		FilterMonitoringPointsCommand(mandatory(department), mandatory(academicYear), user)

	@RequestMapping
	def home(
		@ModelAttribute("filterCommand") filterCommand: Appliable[FilterMonitoringPointsCommandResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		val filterResult = filterCommand.apply().pointMap
		if (ajax) {
			Mav("attendance/view/_points_results",
				"filterResult" -> filterResult
			).noLayout()
		} else {
			Mav("attendance/view/points",
				"filterResult" -> filterResult
			).crumbs(
				Breadcrumbs.View.HomeForYear(academicYear),
				Breadcrumbs.View.DepartmentForYear(department, academicYear)
			).secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.View.points(department, year)): _*)
		}
	}

}