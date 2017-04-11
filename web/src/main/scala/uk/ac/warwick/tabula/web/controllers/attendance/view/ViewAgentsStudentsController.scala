package uk.ac.warwick.tabula.web.controllers.attendance.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.attendance.view.{FilteredStudentsAttendanceResult, ViewAgentsStudentsCommand}
import uk.ac.warwick.tabula.web.controllers.attendance.{AttendanceController, HasMonthNames}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.{Department, Member, StudentRelationshipType}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController

@Controller
@RequestMapping(Array("/attendance/view/{department}/{academicYear}/agents/{relationshipType}/{agent}"))
class ViewAgentsStudentsController extends AttendanceController with HasMonthNames
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable agent: Member
	) =
		ViewAgentsStudentsCommand(mandatory(department), mandatory(academicYear), mandatory(relationshipType), mandatory(agent))

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[FilteredStudentsAttendanceResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable agent: Member
	): Mav = {
		val result = cmd.apply()
		Mav("attendance/view/agent",
			"result" -> result,
			"visiblePeriods" -> result.results.flatMap(_.groupedPointCheckpointPairs.keys).distinct
		).crumbs(
			Breadcrumbs.View.HomeForYear(academicYear),
			Breadcrumbs.View.DepartmentForYear(department, academicYear),
			Breadcrumbs.View.Agents(department, academicYear, relationshipType)
		).secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.View.agent(department, year, relationshipType, agent)): _*)
	}

}
