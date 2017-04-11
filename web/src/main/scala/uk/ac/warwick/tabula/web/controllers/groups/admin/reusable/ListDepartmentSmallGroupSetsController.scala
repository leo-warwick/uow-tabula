package uk.ac.warwick.tabula.web.controllers.groups.admin.reusable

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.commands.groups.admin.reusable.{ListDepartmentSmallGroupSetsCommand, ListDepartmentSmallGroupSetsCommandState}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

@Controller
@RequestMapping(value=Array("/groups/admin/department/{department}/{academicYear}/groups/reusable"))
class ListDepartmentSmallGroupSetsController extends GroupsController
	with DepartmentScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AcademicYearScopedController
	with AutowiringMaintenanceModeServiceComponent {

	hideDeletedItems

	type ListDepartmentSmallGroupSetsCommand = Appliable[Seq[DepartmentSmallGroupSet]] with ListDepartmentSmallGroupSetsCommandState

	override val departmentPermission: Permission = Permissions.SmallGroups.Create

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		ListDepartmentSmallGroupSetsCommand(department, academicYear)

	@RequestMapping
	def list(@ModelAttribute("command") command: ListDepartmentSmallGroupSetsCommand, @PathVariable department: Department): Mav = {
		Mav("groups/admin/groups/reusable/list",
			"sets" -> command.apply()
		).crumbs(Breadcrumbs.Department(command.department, command.academicYear))
			.secondCrumbs(academicYearBreadcrumbs(command.academicYear)(year => Routes.admin.reusable(department, year)):_*)
	}

}
