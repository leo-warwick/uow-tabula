package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.admin.{AdminSmallGroupsHomeCommand, AdminSmallGroupsHomeCommandState, AdminSmallGroupsHomeInformation}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupSet, SmallGroupSetFilters}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.permissions.{Permissions, Permission}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringTermServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.groups.{GroupsController, GroupsDepartmentsAndModulesWithPermission}
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.JavaConverters._

abstract class AbstractGroupsAdminDepartmentHomeController extends GroupsController with AutowiringTermServiceComponent
	with DepartmentScopedController with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent with GroupsDepartmentsAndModulesWithPermission
	with AutowiringMaintenanceModeServiceComponent {

	type AdminSmallGroupsHomeCommand = Appliable[AdminSmallGroupsHomeInformation] with AdminSmallGroupsHomeCommandState

	override val departmentPermission: Permission = null

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department) = retrieveActiveDepartment(Option(department))

	@ModelAttribute("departmentsWithPermission")
	override def departmentsWithPermission: Seq[Department] = {
		def withSubDepartments(d: Department) = Seq(d) ++ d.children.asScala.toSeq.filter(_.routes.asScala.nonEmpty).sortBy(_.fullName)

		allDepartmentsForPermission(user, Permissions.Module.ManageSmallGroups)
			.toSeq.sortBy(_.fullName).flatMap(withSubDepartments).distinct
	}

	hideDeletedItems

	@ModelAttribute("allocated") def allocatedSet(@RequestParam(value="allocated", required=false) set: SmallGroupSet) = set

	private def process(cmd: AdminSmallGroupsHomeCommand, department: Department, view: String) = {
		val info = cmd.apply()

		val hasModules = info.modulesWithPermission.nonEmpty
		val hasGroups = info.setsWithPermission.nonEmpty
		val hasGroupAttendance = info.setsWithPermission.exists { _.set.showAttendanceReports }
		val isFiltered = !(cmd.moduleFilters.isEmpty && cmd.statusFilters.isEmpty && cmd.allocationMethodFilters.isEmpty && cmd.termFilters.isEmpty)

		val model = Map(
			"viewedAcademicYear" -> cmd.academicYear,
			"department" -> department,
			"canAdminDepartment" -> info.canAdminDepartment,
			"modules" -> info.modulesWithPermission,
			"sets" -> info.setsWithPermission,
			"hasUnreleasedGroupsets" -> info.setsWithPermission.exists { !_.set.fullyReleased },
			"hasOpenableGroupsets" -> info.setsWithPermission.exists { sv => (!sv.set.openForSignups) && sv.set.allocationMethod == SmallGroupAllocationMethod.StudentSignUp },
			"hasCloseableGroupsets" -> info.setsWithPermission.exists { sv => sv.set.openForSignups && sv.set.allocationMethod == SmallGroupAllocationMethod.StudentSignUp },
			"hasModules" -> hasModules,
			"hasGroups" -> hasGroups,
			"hasGroupAttendance" -> hasGroupAttendance,
			"allStatusFilters" -> SmallGroupSetFilters.Status.all,
			"allModuleFilters" -> SmallGroupSetFilters.allModuleFilters(info.modulesWithPermission),
			"allAllocationFilters" -> SmallGroupSetFilters.AllocationMethod.all(info.departmentSmallGroupSets),
			"allTermFilters" -> SmallGroupSetFilters.allTermFilters(cmd.academicYear, termService),
			"isFiltered" -> isFiltered
		)

		Mav(view, model)
			.secondCrumbs(academicYearBreadcrumbs(cmd.academicYear)(year => Routes.admin(department, year)):_*)
	}

	@RequestMapping(params=Array("!ajax"), headers=Array("!X-Requested-With"))
	def adminDepartment(@ModelAttribute("adminCommand") cmd: AdminSmallGroupsHomeCommand, @PathVariable("department") department: Department, user: CurrentUser) =
		process(cmd, department, "groups/admin/department")

	@RequestMapping
	def loadSets(@ModelAttribute("adminCommand") cmd: AdminSmallGroupsHomeCommand, @PathVariable("department") department: Department, user: CurrentUser) =
		process(cmd, department, "groups/admin/department-noLayout").noLayout()
}

@Controller
@RequestMapping(value=Array("/groups/admin/department/{department}"))
class GroupsAdminDepartmentController extends AbstractGroupsAdminDepartmentHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

	@ModelAttribute("adminCommand")
	def command(@PathVariable("department") dept: Department, @ModelAttribute("activeAcademicYear") academicYear: Option[AcademicYear], user: CurrentUser): AdminSmallGroupsHomeCommand =
		AdminSmallGroupsHomeCommand(mandatory(dept), academicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)), user, calculateProgress = ajax)

}

@Controller
@RequestMapping(value=Array("/groups/admin/department/{department}/{academicYear}"))
class GroupsAdminDepartmentForYearController extends AbstractGroupsAdminDepartmentHomeController
	with DepartmentScopedController with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@ModelAttribute("adminCommand")
	def command(@PathVariable("department") dept: Department, @PathVariable("academicYear") academicYear: AcademicYear, user: CurrentUser): AdminSmallGroupsHomeCommand =
		AdminSmallGroupsHomeCommand(mandatory(dept), mandatory(academicYear), user, calculateProgress = ajax)

}