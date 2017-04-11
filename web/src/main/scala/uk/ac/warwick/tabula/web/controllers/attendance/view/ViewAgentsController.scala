package uk.ac.warwick.tabula.web.controllers.attendance.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.attendance.view.{ViewAgentsCommand, ViewAgentsCommandResult}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController

@Controller
@RequestMapping(Array("/attendance/view/{department}/{academicYear}/agents/{relationshipType}"))
class ViewAgentsController extends AttendanceController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable relationshipType: StudentRelationshipType) =
		ViewAgentsCommand(mandatory(department), mandatory(academicYear), mandatory(relationshipType))

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[Seq[ViewAgentsCommandResult]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable relationshipType: StudentRelationshipType
	): Mav = {
		val agents = cmd.apply()
		Mav("attendance/view/agents",
			"agents" -> agents,
			"agentsEmails" -> agents.flatMap(a => Option(a.agentMember)).flatMap(_.email.maybeText)
		).crumbs(
			Breadcrumbs.View.HomeForYear(academicYear),
			Breadcrumbs.View.DepartmentForYear(department, academicYear)
		).secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.View.agents(department, year, relationshipType)): _*)
	}

}
