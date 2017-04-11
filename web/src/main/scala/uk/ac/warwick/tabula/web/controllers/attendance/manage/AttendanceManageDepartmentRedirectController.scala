package uk.ac.warwick.tabula.web.controllers.attendance.manage

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

@Controller
@RequestMapping(Array("/attendance/manage/{department:[^\\d].*}"))
class AttendanceManageDepartmentRedirectController extends AttendanceController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

	@RequestMapping
	def home(@PathVariable department: Department, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]): Mav = {
		Redirect(Routes.Manage.departmentForYear(mandatory(department), activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))))
	}

}