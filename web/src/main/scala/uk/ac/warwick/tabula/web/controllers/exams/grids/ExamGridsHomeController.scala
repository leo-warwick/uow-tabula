package uk.ac.warwick.tabula.web.controllers.exams.grids

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.CurrentSITSAcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.permissions.{Permissions, Permission}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent, AutowiringModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController

@Controller
@RequestMapping(Array("/exams/grids"))
class ExamGridsHomeController extends ExamsController
	with DepartmentScopedController with AcademicYearScopedController
	with AutowiringModuleAndDepartmentServiceComponent with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent with CurrentSITSAcademicYear {

	override def departmentPermission: Permission = Permissions.Department.ExamGrids

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
			Redirect(Routes.Grids.departmentAcademicYear(activeDepartment.get, activeAcademicYear.get))
		} else if (activeDepartment.isDefined) {
			Redirect(Routes.Grids.departmentAcademicYear(activeDepartment.get, academicYear))
		} else {
			val featureFilteredDepartments = departmentsWithPermission
			Mav("exams/grids/home",
				"featureFilteredDepartments" -> featureFilteredDepartments,
				"academicYear" -> academicYear
			)
		}
	}

}
