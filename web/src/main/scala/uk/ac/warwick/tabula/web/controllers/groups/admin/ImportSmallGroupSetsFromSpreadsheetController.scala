package uk.ac.warwick.tabula.web.controllers.groups.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.groups.admin.{ImportSmallGroupSetsFromSpreadsheetCommand, SmallGroupSetsSpreadsheetTemplateCommand}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController
import uk.ac.warwick.tabula.web.views.ExcelView

@Controller
@RequestMapping(Array("/groups/admin/department/{department}/{academicYear}/import-spreadsheet"))
class ImportSmallGroupSetsFromSpreadsheetController extends GroupsController
	with DepartmentScopedController with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	override val departmentPermission: Permission = ImportSmallGroupSetsFromSpreadsheetCommand.RequiredPermission

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	validatesSelf[SelfValidating]

	type CommandType = ImportSmallGroupSetsFromSpreadsheetCommand.CommandType

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): CommandType =
		ImportSmallGroupSetsFromSpreadsheetCommand(mandatory(department), mandatory(academicYear))

	@RequestMapping
	def form(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): Mav = {
		Mav("groups/admin/groups/import-spreadsheet/form")
			.crumbs(Breadcrumbs.Department(department, academicYear))
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.groups.admin.importSpreadsheet(department, year)): _*)
	}

	@RequestMapping(method = Array(POST))
	def processSpreadsheet(
		@Valid @ModelAttribute("command") cmd: CommandType,
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = Mav("groups/admin/groups/import-spreadsheet/preview", "errors" -> errors)
		.crumbs(Breadcrumbs.Department(department, academicYear))
		.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.groups.admin.importSpreadsheet(department, year)): _*)

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def submit(
		@Valid @ModelAttribute("command") cmd: CommandType,
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			processSpreadsheet(cmd, errors, department, academicYear)
		} else {
			cmd.apply()
			Redirect(Routes.groups.admin(department, academicYear))
		}
	}

}

@Controller
@RequestMapping(Array("/groups/admin/department/{department}/{academicYear}/import-spreadsheet/template"))
class SmallGroupSetsSpreadsheetTemplateController extends GroupsController {

	type CommandType = Appliable[ExcelView]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): CommandType =
		SmallGroupSetsSpreadsheetTemplateCommand(mandatory(department), mandatory(academicYear))

	@RequestMapping
	def getTemplate(@Valid @ModelAttribute("command") cmd: CommandType): ExcelView = {
		cmd.apply()
	}

}