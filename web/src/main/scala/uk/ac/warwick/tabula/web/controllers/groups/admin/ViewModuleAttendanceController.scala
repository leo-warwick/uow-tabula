package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.groups.ViewSmallGroupAttendanceCommand
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, PathVariable}
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController
import scala.collection.immutable.SortedMap
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.commands.groups.admin.{ViewModuleAttendanceState, ViewModuleAttendanceCommand}

abstract class AbstractViewModuleAttendanceController extends GroupsController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent {

	type ViewModuleAttendanceCommand = Appliable[SortedMap[SmallGroupSet, SortedMap[SmallGroup, ViewSmallGroupAttendanceCommand.SmallGroupAttendanceInformation]]]
		with ViewModuleAttendanceState

	@RequestMapping
	def show(
		@ModelAttribute("command") command: ViewModuleAttendanceCommand,
		@PathVariable module: Module
	): Mav = {
		val sets = command.apply()

		if (ajax)
			Mav("groups/attendance/view_module_partial",
				"sets" -> sets
			).noLayout()
		else
			Mav("groups/attendance/view_module",
				"sets" -> sets
			).crumbs(Breadcrumbs.Department(module.adminDepartment, command.academicYear), Breadcrumbs.Module(module))
				.secondCrumbs(academicYearBreadcrumbs(command.academicYear)(year => Routes.admin.moduleAttendance(command.module, year)):_*)
	}

}

@RequestMapping(Array("/groups/admin/module/{module}/attendance"))
@Controller
class ViewModuleAttendanceController extends AbstractViewModuleAttendanceController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @ModelAttribute("activeAcademicYear") academicYear: Option[AcademicYear]) =
		ViewModuleAttendanceCommand(mandatory(module), academicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)))

}

@RequestMapping(Array("/groups/admin/module/{module}/{academicYear}/attendance"))
@Controller
class ViewModuleAttendanceInYearController extends AbstractViewModuleAttendanceController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable academicYear: AcademicYear) =
		ViewModuleAttendanceCommand(mandatory(module), mandatory(academicYear))

}
