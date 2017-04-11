package uk.ac.warwick.tabula.web.controllers.groups

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.ViewModules
import uk.ac.warwick.tabula.groups.web.views.{GroupsDisplayHelper, GroupsViewModel}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

trait GroupsDepartmentsAndModulesWithPermission {

	self: ModuleAndDepartmentServiceComponent =>

	case class Result(departments: Set[Department], modules: Set[Module])

	def departmentsAndModulesForPermission(user: CurrentUser, permission: Permission): Result = {
		val departments = moduleAndDepartmentService.departmentsWithPermission(user, permission)
		val modules = moduleAndDepartmentService.modulesWithPermission(user, permission)
		Result(departments, modules)
	}

	def allDepartmentsForPermission(user: CurrentUser, permission: Permission): Set[Department] = {
		val result = departmentsAndModulesForPermission(user, permission)
		result.departments ++ result.modules.map(_.adminDepartment)
	}
}

abstract class AbstractGroupsHomeController extends GroupsController with GroupsDepartmentsAndModulesWithPermission
	with AutowiringModuleAndDepartmentServiceComponent with AutowiringSmallGroupServiceComponent
	with AcademicYearScopedController with AutowiringMaintenanceModeServiceComponent with AutowiringUserSettingsServiceComponent {

	import GroupsDisplayHelper._

	@RequestMapping
	def home(@ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]): Mav = {
		val academicYear = activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))

		if (user.loggedIn) {
			val departmentsAndRoutes = departmentsAndModulesForPermission(user, Permissions.Module.ManageSmallGroups)
			val taughtGroups = smallGroupService.findSmallGroupsByTutor(user.apparentUser)

			val memberGroupSets = smallGroupService.findSmallGroupSetsByMember(user.apparentUser).filter(_.academicYear == academicYear)
			val releasedMemberGroupSets = getGroupSetsReleasedToStudents(memberGroupSets)
			val nonEmptyMemberViewModules = getViewModulesForStudent(releasedMemberGroupSets, getGroupsToDisplay(_,user.apparentUser))

			val todaysOccurrences = smallGroupService.findTodaysEventOccurrences(Seq(user.apparentUser), departmentsAndRoutes.modules.toSeq, departmentsAndRoutes.departments.toSeq)

			Mav("groups/home/view",
				"academicYear" -> academicYear,
				"ownedDepartments" -> departmentsAndRoutes.departments,
				"ownedModuleDepartments" -> departmentsAndRoutes.modules.map { _.adminDepartment },
				"taughtGroups" -> taughtGroups,
				"memberGroupsetModules" -> ViewModules(nonEmptyMemberViewModules.sortBy(_.module.code), canManageDepartment = false),
				"todaysModules" -> ViewModules.fromOccurrences(todaysOccurrences, GroupsViewModel.Tutor),
				"showOccurrenceAttendance" -> true
			).secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.homeForYear(year)):_*)
		} else {
			Mav("groups/home/view")
		}
	}

}

@Controller
@RequestMapping(Array("/groups"))
class GroupsHomeController extends AbstractGroupsHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

}

@Controller
@RequestMapping(Array("/groups/{academicYear}"))
class GroupsHomeForYearController extends AbstractGroupsHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

}