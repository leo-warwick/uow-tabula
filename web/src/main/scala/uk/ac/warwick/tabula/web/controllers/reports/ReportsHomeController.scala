package uk.ac.warwick.tabula.web.controllers.reports

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.CurrentSITSAcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.reports.web.Routes
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}

/**
 * Displays the Reports home screen.
 */
@Controller
@RequestMapping(Array("/reports"))
class ReportsHomeController extends ReportsController with CurrentSITSAcademicYear
	with DepartmentScopedController with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	override val departmentPermission: Permission = Permissions.Department.Reports

	@ModelAttribute("activeDepartment")
	override def activeDepartment(department: Department): Option[Department] = retrieveActiveDepartment(None)

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

	@RequestMapping
	def home(
		@ModelAttribute("activeDepartment") activeDepartment: Option[Department],
		@ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]
	): Mav = {
		if (activeDepartment.isDefined && activeAcademicYear.isDefined) {
			Redirect(Routes.departmentAcademicYear(activeDepartment.get, activeAcademicYear.get))
		} else if (activeDepartment.isDefined) {
			Redirect(Routes.departmentHome(activeDepartment.get))
		} else {
			Mav("reports/home", "academicYears" -> Seq(academicYear.previous, academicYear))
		}
	}

}