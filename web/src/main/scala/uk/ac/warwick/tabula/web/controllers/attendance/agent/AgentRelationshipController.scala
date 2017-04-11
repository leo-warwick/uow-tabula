package uk.ac.warwick.tabula.web.controllers.attendance.agent

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

/**
 * Displays the agent relationship screen, allowing users to choose academic year to view.
 */
@Controller
@RequestMapping(Array("/attendance/agent/{relationshipType}"))
class AgentRelationshipController extends AttendanceController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

	@RequestMapping
	def home(
		@PathVariable relationshipType: StudentRelationshipType,
		@ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]
	): Mav = {
		Redirect(Routes.Agent.relationshipForYear(relationshipType, activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))))
	}

}