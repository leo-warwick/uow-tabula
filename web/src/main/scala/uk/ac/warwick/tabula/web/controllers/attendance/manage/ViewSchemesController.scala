package uk.ac.warwick.tabula.web.controllers.attendance.manage

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.attendance.manage.ViewSchemesCommand
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}

@Controller
@RequestMapping(Array("/attendance/manage/{department}/{academicYear}"))
class ViewSchemesController extends AttendanceController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent
	with DepartmentScopedController with AutowiringModuleAndDepartmentServiceComponent {

	override val departmentPermission: Permission = Permissions.MonitoringPoints.Manage

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear)
		= ViewSchemesCommand(mandatory(department), mandatory(academicYear), user)

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringScheme]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		val schemes = cmd.apply()

		Mav("attendance/manage/list",
			"schemes" -> schemes,
			"havePoints" -> (schemes.map{_.points.size}.sum > 0)
		).crumbs(
			Breadcrumbs.Manage.HomeForYear(academicYear),
			Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
		).secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.Manage.departmentForYear(department, year)): _*)
	}

}
