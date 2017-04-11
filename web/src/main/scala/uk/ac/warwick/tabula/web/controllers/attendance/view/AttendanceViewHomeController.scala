package uk.ac.warwick.tabula.web.controllers.attendance.view

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.commands.attendance.{HomeCommand, HomeInformation}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController

/**
 * Displays the view home screen, allowing users to choose the department and academic year to view.
 */
abstract class AbstractAttendanceViewHomeController extends AttendanceController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("command")
	def createCommand(user: CurrentUser) = HomeCommand(user)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[HomeInformation], @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]): Mav = {
		val info = cmd.apply()
		val academicYear = activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))

		if (info.viewPermissions.size == 1) {
			Redirect(Routes.View.departmentForYear(info.viewPermissions.head, academicYear))
		} else {
			Mav("attendance/view/home",
				"academicYear" -> academicYear,
				"viewPermissions" -> info.viewPermissions
			).secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.View.homeForYear(year)): _*)
		}
	}

}

@Controller
@RequestMapping(Array("/attendance/view"))
class AttendanceViewHomeController extends AbstractAttendanceViewHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

}

@Controller
@RequestMapping(value = Array("/attendance/view/{academicYear:\\d{4}}"))
class AttendanceViewHomeForYearController extends AbstractAttendanceViewHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

}