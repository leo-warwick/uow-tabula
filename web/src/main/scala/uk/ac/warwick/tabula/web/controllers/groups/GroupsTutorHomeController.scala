package uk.ac.warwick.tabula.web.controllers.groups

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.TutorHomeCommand
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEventOccurrence, SmallGroupSet}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController

/**
 * Displays the group sets that the current user is a tutor of.
 */
abstract class AbstractGroupsTutorHomeController extends GroupsController
	with AcademicYearScopedController with AutowiringMaintenanceModeServiceComponent with AutowiringUserSettingsServiceComponent {

	@ModelAttribute("command") def command(@ModelAttribute("activeAcademicYear") academicYear: Option[AcademicYear]) =
		TutorHomeCommand(user, academicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)))

	@RequestMapping
	def listModules(
		@ModelAttribute("command") command: Appliable[Map[Module, Map[SmallGroupSet, Seq[SmallGroup]]]],
		@ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear],
		@RequestParam(value="updatedOccurrence", required=false) occurrence: SmallGroupEventOccurrence
	): Mav = {
		val mapping = command.apply()
		val academicYear = activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))

		val data = GroupsViewModel.ViewModules.generate(mapping, GroupsViewModel.Tutor)

		Mav("groups/tutor_home",
			"academicYear" -> academicYear,
			"data" -> data,
			"updatedOccurrence" -> occurrence,
			"isSelf" -> true
		).secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.tutor.mygroupsForYear(year)):_*)
	}

}

@Controller
@RequestMapping(Array("/groups/tutor"))
class GroupsTutorHomeController extends AbstractGroupsTutorHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

}

@Controller
@RequestMapping(Array("/groups/tutor/{academicYear}"))
class GroupsTutorHomeForYearController extends AbstractGroupsTutorHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

}
