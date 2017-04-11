package uk.ac.warwick.tabula.web.controllers.reports.attendancemonitoring

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.reports.ReportsController
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}

@Controller
@RequestMapping(Array("/reports/{department}/{academicYear}/attendance"))
class AttendanceReportsHomeController extends ReportsController
	with DepartmentScopedController with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	override val departmentPermission: Permission = Permissions.Department.Reports

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@RequestMapping
	def home(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): Mav = {
		Mav("reports/attendancemonitoring/home")
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.reports.Attendance.home(department, year)): _*)
	}

}