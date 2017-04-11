package uk.ac.warwick.tabula.web.controllers.attendance.agent

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.commands.attendance.{HomeCommand, HomeInformation}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController

/**
 * Displays the agent home screen, allowing users to choose the relationship type.
 */
@Controller
@RequestMapping(Array("/attendance/agent"))
class AgentHomeController extends AttendanceController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

	@ModelAttribute("command")
	def createCommand(user: CurrentUser) = HomeCommand(user)

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[HomeInformation],
		@ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]
	): Mav = {
		val info = cmd.apply()
		if (info.relationshipTypesMap.keySet.size == 1) {
			Redirect(Routes.Agent.relationshipForYear(info.relationshipTypesMap.keys.head, activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))))
		} else {
			Mav("attendance/agent/home", "relationshipTypesMap" -> info.relationshipTypesMap)
		}
	}

}