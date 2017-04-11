package uk.ac.warwick.tabula.web.controllers.attendance

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.attendance.{HomeCommand, HomeInformation}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

/**
 * Displays the Attendance home screen.
 * Redirects to the the appropriate page if only one of the following is true:
 * * The user has a profile
 * * The user has view/record permissions on a single department
 * * The user has manage permissions on a single department
 * Otherwise they are shown the home page
 */
abstract class AbstractAttendanceHomeController extends AttendanceController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("command")
	def createCommand(user: CurrentUser) = HomeCommand(user)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[HomeInformation], @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]): Mav = {
		val info = cmd.apply()

		val hasAnyRelationships = info.relationshipTypesMap.exists { case (_, b) => b }

		val academicYear = activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))

		if (info.hasProfile && info.managePermissions.isEmpty && info.viewPermissions.isEmpty && !hasAnyRelationships) {
			Redirect(Routes.Profile.home)
		} else if (!info.hasProfile && info.managePermissions.isEmpty && info.viewPermissions.size == 1 && !hasAnyRelationships) {
			Redirect(Routes.View.departmentForYear(info.viewPermissions.head, academicYear))
		} else if (!info.hasProfile && info.managePermissions.size == 1 && info.viewPermissions.isEmpty && !hasAnyRelationships) {
			Redirect(Routes.Manage.departmentForYear(info.managePermissions.head, academicYear))
		} else {
			Mav("attendance/home",
				"academicYear" -> academicYear,
				"hasProfile" -> info.hasProfile,
				"relationshipTypesMap" -> info.relationshipTypesMap,
				"relationshipTypesMapById" -> info.relationshipTypesMap.map { case (k, v) => (k.id, v) },
				"hasAnyRelationships" -> hasAnyRelationships,
				"viewPermissions" -> info.viewPermissions,
				"managePermissions" -> info.managePermissions
			).secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.homeForYear(year)): _*)
		}
	}
}


@Controller
@RequestMapping(Array("/attendance"))
class AttendanceHomeController extends AbstractAttendanceHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

}

@Controller
@RequestMapping(Array("/attendance/{academicYear}"))
class AttendanceHomeForYearController extends AbstractAttendanceHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

}