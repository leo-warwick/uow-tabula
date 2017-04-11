package uk.ac.warwick.tabula.web.controllers.attendance.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.web.controllers.attendance.{AttendanceController, HasMonthNames}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.attendance.view.{FilterStudentsAttendanceCommand, FilteredStudentsAttendanceResult}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}

@Controller
@RequestMapping(Array("/attendance/view/{department}/{academicYear}/students"))
class ViewStudentsAttendanceController extends AttendanceController with HasMonthNames
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
		FilterStudentsAttendanceCommand(mandatory(department), mandatory(academicYear))

	@RequestMapping
	def home(
		@ModelAttribute("filterCommand") filterCommand: Appliable[FilteredStudentsAttendanceResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam(value="reports", required = false) reports: JInteger,
		@RequestParam(value="monitoringPeriod", required = false) monitoringPeriod: String
	): Mav = {
		val filterResult = filterCommand.apply()
		val modelMap = Map(
			"filterResult" -> filterResult,
			"visiblePeriods" -> filterResult.results.flatMap(_.groupedPointCheckpointPairs.keys).distinct,
			"reports" -> reports,
			"monitoringPeriod" -> monitoringPeriod
		)
		if (ajax) {
			Mav("attendance/view/_students_results", modelMap).noLayout()
		} else {
			Mav("attendance/view/students", modelMap).crumbs(
				Breadcrumbs.View.HomeForYear(academicYear),
				Breadcrumbs.View.DepartmentForYear(department, academicYear)
			).secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.View.students(department, year)): _*)
		}
	}

}