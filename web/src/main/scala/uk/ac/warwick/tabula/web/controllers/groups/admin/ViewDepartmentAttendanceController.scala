package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.admin.{ViewDepartmentAttendanceCommand, ViewDepartmentAttendanceCommandState}
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.{GroupsController, GroupsDepartmentsAndModulesWithPermission}
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

abstract class AbstractViewDepartmentAttendanceController extends GroupsController
	with DepartmentScopedController with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent with GroupsDepartmentsAndModulesWithPermission {

	override val departmentPermission: Permission = ViewDepartmentAttendanceCommand.RequiredPermission

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	hideDeletedItems

	@RequestMapping(method=Array(GET, HEAD))
	def adminDepartment(@ModelAttribute("adminCommand") cmd: Appliable[Seq[Module]] with ViewDepartmentAttendanceCommandState): Mav = {
		Mav("groups/attendance/view_department",
			"modules" -> cmd.apply()
		).crumbs(Breadcrumbs.Department(cmd.department, cmd.academicYear))
			.secondCrumbs(academicYearBreadcrumbs(cmd.academicYear)(year => Routes.admin.departmentAttendance(cmd.department, year)):_*)
	}

}

@Controller
@RequestMapping(value=Array("/groups/admin/department/{department}/attendance"))
class ViewDepartmentAttendanceController extends AbstractViewDepartmentAttendanceController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

	@ModelAttribute("adminCommand")
	def command(@PathVariable department: Department, @ModelAttribute("activeAcademicYear") academicYear: Option[AcademicYear], user: CurrentUser) =
		ViewDepartmentAttendanceCommand(mandatory(department), academicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)), user)

}

@Controller
@RequestMapping(value=Array("/groups/admin/department/{department}/{academicYear}/attendance"))
class ViewDepartmentAttendanceInYearController extends AbstractViewDepartmentAttendanceController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@ModelAttribute("adminCommand")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, user: CurrentUser) =
		ViewDepartmentAttendanceCommand(mandatory(department), mandatory(academicYear), user)


}
