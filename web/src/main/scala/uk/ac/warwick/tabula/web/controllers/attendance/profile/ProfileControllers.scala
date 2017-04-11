package uk.ac.warwick.tabula.web.controllers.attendance.profile

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.attendance.profile.{AttendanceProfileCommand, AttendanceProfileCommandResult}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.attendance.{AttendanceController, HasMonthNames}

@Controller
@RequestMapping(value = Array("/attendance/profile"))
class ProfileHomeController extends AttendanceController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

	@RequestMapping
	def render(@ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]): Mav = user.profile match {
		case Some(student: StudentMember) =>
			Redirect(Routes.Profile.profileForYear(student, activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))))
		case _ if user.isStaff =>
			Mav("attendance/profile/staff").noLayoutIf(ajax)
		case _ =>
			Mav("attendance/profile/unknown").noLayoutIf(ajax)
	}
}

@Controller
@RequestMapping(value = Array("/attendance/profile/{student}"))
class ProfileYearRedirectController extends AttendanceController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

	@RequestMapping
	def redirect(
		@ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear],
		@PathVariable student: StudentMember
	): Mav = {
		Redirect(Routes.Profile.profileForYear(student, activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))))
	}
}

@Controller
@RequestMapping(Array("/attendance/profile/{student}/{academicYear}"))
class ProfileController extends AttendanceController with HasMonthNames
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@ModelAttribute("command")
	def command(@PathVariable student: StudentMember, @PathVariable academicYear: AcademicYear) =
		AttendanceProfileCommand(mandatory(student), mandatory(academicYear))

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[AttendanceProfileCommandResult],
		@PathVariable student: StudentMember,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		val commandResult = cmd.apply()

		Mav("attendance/profile/profile",
			"groupedPointMap" -> commandResult.groupedPointMap,
			"missedPointCountByTerm" -> commandResult.missedPointCountByTerm,
			"hasAnyMissed" -> commandResult.hasAnyMissedPoints,
			"department" -> currentMember.homeDepartment,
			"isSelf" -> (user.apparentId == student.userId),
			"notes" -> commandResult.notes,
			"noteCheckpoints" -> commandResult.noteCheckpoints,
			"allCheckpointStates" -> AttendanceState.values.sortBy(state => state.description) ,
			"returnTo" -> getReturnTo(Routes.Profile.profileForYear(mandatory(student), mandatory(academicYear)))
		).secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.Profile.profileForYear(student, year)):_*)
	}
}
