package uk.ac.warwick.tabula.web.controllers.groups.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.groups.admin.{AdminSmallGroupsHomeCommand, DownloadRegistersAsPdfCommand}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.system.RenderableFileView
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}

@Controller
@RequestMapping(value = Array("/groups/admin/department/{department}/{academicYear}"))
class DownloadRegistersAsPdfController extends GroupsController with DepartmentScopedController with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	override val departmentPermission: Permission = AdminSmallGroupsHomeCommand.RequiredPermission

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	hideDeletedItems

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		DownloadRegistersAsPdfCommand(mandatory(department), mandatory(academicYear), "registers.pdf", user)

	@RequestMapping(value = Array("/registers"))
	def form(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): Mav = {
		Mav("groups/admin/groups/print")
			.crumbs(Breadcrumbs.Department(department, academicYear))
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.admin.registers(department, year)):_*)
	}

	@RequestMapping(method = Array(POST), value = Array("/registers.pdf"))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[RenderableFile],
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			form(department, academicYear)
		} else {
			Mav(new RenderableFileView(cmd.apply()))
		}
	}

}

