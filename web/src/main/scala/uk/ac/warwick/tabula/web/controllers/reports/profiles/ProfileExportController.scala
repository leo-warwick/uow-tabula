package uk.ac.warwick.tabula.web.controllers.reports.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.reports.profiles.ProfileExportCommand
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.reports.{ReportsBreadcrumbs, ReportsController}

@Controller
@RequestMapping(Array("/reports/{department}/{academicYear}/profiles/export"))
class ProfileExportController extends ReportsController {

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		ProfileExportCommand(department, academicYear, user)

	@RequestMapping
	def generateReport(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringStudentData]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		val results = cmd.apply()
		if (ajax) {
			Mav("reports/profiles/_filter", "results" -> results).noLayout()
		} else {
			Mav("reports/profiles/filter", "results" -> results).crumbs(
				ReportsBreadcrumbs.Home.Department(department),
				ReportsBreadcrumbs.Home.DepartmentForYear(department, academicYear),
				ReportsBreadcrumbs.Profiles.Home(department, academicYear)
			)
		}
	}


}