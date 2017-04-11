package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

trait CourseworkDepartmentAndYearController extends DepartmentScopedController with AcademicYearScopedController
	with AutowiringModuleAndDepartmentServiceComponent with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	self : CourseworkController =>

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] =
		retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
		retrieveActiveAcademicYear(Option(academicYear))

}
