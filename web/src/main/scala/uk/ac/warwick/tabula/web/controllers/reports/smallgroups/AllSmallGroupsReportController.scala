package uk.ac.warwick.tabula.web.controllers.reports.smallgroups

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.commands.reports.smallgroups._
import uk.ac.warwick.tabula.reports.web.Routes


@Controller
@RequestMapping(Array("/reports/{department}/{academicYear}/groups/all"))
class AllSmallGroupsReportController extends AbstractSmallGroupsReportController {

	@ModelAttribute("command")
	def command(@PathVariable("department") department: Department, @PathVariable("academicYear") academicYear: AcademicYear) =
		AllSmallGroupsReportCommand(mandatory(department), mandatory(academicYear), SmallGroupsReportFilters.identity)

	val pageRenderPath = "allsmallgroups"
	val filePrefix = "all-small-group-attendance"
	def urlGeneratorFactory(department: Department): (AcademicYear) => String = year => Routes.SmallGroups.all(department, year)

}

@Controller
@RequestMapping(Array("/reports/{department}/{academicYear}/groups/unrecorded"))
class UnrecordedSmallGroupsReportController extends AbstractSmallGroupsReportController {

	@ModelAttribute("command")
	def command(@PathVariable("department") department: Department, @PathVariable("academicYear") academicYear: AcademicYear) =
		AllSmallGroupsReportCommand(mandatory(department), mandatory(academicYear), SmallGroupsReportFilters.unrecorded(academicYear))

	val pageRenderPath = "unrecorded"
	val filePrefix = "unrecorded-small-group-attendance"
	def urlGeneratorFactory(department: Department): (AcademicYear) => String = year => Routes.SmallGroups.unrecorded(department, year)

}

@Controller
@RequestMapping(Array("/reports/{department}/{academicYear}/groups/missed"))
class MissedSmallGroupsReportController extends AbstractSmallGroupsReportController {

	@ModelAttribute("command")
	def command(@PathVariable("department") department: Department, @PathVariable("academicYear") academicYear: AcademicYear) =
		AllSmallGroupsReportCommand(mandatory(department), mandatory(academicYear), SmallGroupsReportFilters.missed(academicYear))

	val pageRenderPath = "missed"
	val filePrefix = "missed-small-group-attendance"
	def urlGeneratorFactory(department: Department): (AcademicYear) => String = year => Routes.SmallGroups.missed(department, year)

}