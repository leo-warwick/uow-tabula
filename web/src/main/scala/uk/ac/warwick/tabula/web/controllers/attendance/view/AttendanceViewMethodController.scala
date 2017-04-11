package uk.ac.warwick.tabula.web.controllers.attendance.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}

@Controller
@RequestMapping(Array("/attendance/view/{department}/{academicYear}"))
class AttendanceViewMethodController extends AttendanceController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent
	with DepartmentScopedController with AutowiringModuleAndDepartmentServiceComponent {

	override val departmentPermission: Permission = Permissions.MonitoringPoints.View

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@Autowired var attendanceMonitoringService: AttendanceMonitoringService = _

	@RequestMapping
	def home(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): Mav = {
		Mav("attendance/view/viewmethod",
			"hasSchemes" -> attendanceMonitoringService.listSchemes(mandatory(department), mandatory(academicYear)).nonEmpty
		).crumbs(
			Breadcrumbs.View.HomeForYear(academicYear)
		).secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.View.departmentForYear(department, year)): _*)
	}

}