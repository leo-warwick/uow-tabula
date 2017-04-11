package uk.ac.warwick.tabula.web.controllers.reports.smallgroups

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.commands.reports.smallgroups.{AllSmallGroupsReportCommand, AllSmallGroupsReportCommandResult, SmallGroupsReportFilters}
import uk.ac.warwick.tabula.reports.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.reports.ReportsBreadcrumbs

@Controller
@RequestMapping(Array("/reports/{department}/{academicYear}/groups/missed/bymodule"))
class MissedSmallGroupsByModuleReportController extends AbstractSmallGroupsByModuleReportController {

	@ModelAttribute("filteredAttendanceCommand")
	override def filteredAttendanceCommand(@PathVariable("department") department: Department, @PathVariable("academicYear") academicYear: AcademicYear) =
		AllSmallGroupsReportCommand(department, academicYear, SmallGroupsReportFilters.missed(academicYear))

	override val filePrefix: String = "missed-small-groups-by-module"

	@RequestMapping(method = Array(GET))
	override def page(
		@ModelAttribute("filteredAttendanceCommand") cmd: Appliable[AllSmallGroupsReportCommandResult],
		@PathVariable("department") department: Department,
		@PathVariable("academicYear") academicYear: AcademicYear
	): Mav = {
		Mav("reports/smallgroups/missedByModule")
			.crumbs(
				ReportsBreadcrumbs.SmallGroups.Home(department, academicYear),
				ReportsBreadcrumbs.SmallGroups.Missed(department, academicYear)
			)
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.SmallGroups.missedByModule(department, year)):_*)
	}

 }
