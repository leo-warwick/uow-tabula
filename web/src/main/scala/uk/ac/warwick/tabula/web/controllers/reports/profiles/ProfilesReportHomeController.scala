package uk.ac.warwick.tabula.web.controllers.reports.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.CurrentSITSAcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.reports.{ReportsBreadcrumbs, ReportsController}

@Controller
@RequestMapping(Array("/reports/{department}/{academicYear}/profiles"))
class ProfilesReportHomeController extends ReportsController with CurrentSITSAcademicYear {

	@RequestMapping
	def home(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): Mav = {
		Mav("reports/profiles/home").crumbs(
			ReportsBreadcrumbs.Home.Department(department),
			ReportsBreadcrumbs.Home.DepartmentForYear(department, academicYear)
		)
	}

}