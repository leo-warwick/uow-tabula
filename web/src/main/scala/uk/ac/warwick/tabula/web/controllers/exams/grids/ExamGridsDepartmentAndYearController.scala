package uk.ac.warwick.tabula.web.controllers.exams.grids

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.JavaImports._

@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}"))
class ExamGridsDepartmentAndYearController extends ExamsController
	with DepartmentScopedController with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	override val departmentPermission: Permission = Permissions.Department.ExamGrids

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@RequestMapping
	def home(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @RequestParam(value = "updatedMarks", required = false) updatedMarks: JInteger): Mav = {
		Mav("exams/grids/departmentAndYear",
			"updatedMarks" -> updatedMarks
		).crumbs(Breadcrumbs.Grids.Home)
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.exams.Grids.departmentAcademicYear(department, year)): _*)
	}

}