package uk.ac.warwick.tabula.web.controllers.groups.admin

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.groups.admin.{ImportSmallGroupSetsFromExternalSystemCommand, ImportSmallGroupSetsFromExternalSystemCommandState, ImportSmallGroupSetsFromExternalSystemPermissionsRestrictedState, LookupEventsFromModuleTimetables}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

@Controller
@RequestMapping(value = Array("/groups/admin/department/{department}/import-groups"))
class ImportSmallGroupSetsFromExternalSystemController extends GroupsController
	with DepartmentScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	validatesSelf[SelfValidating]

	type ImportSmallGroupSetsFromExternalSystemCommand =
		Appliable[Seq[SmallGroupSet]]
			with ImportSmallGroupSetsFromExternalSystemCommandState
			with ImportSmallGroupSetsFromExternalSystemPermissionsRestrictedState
			with LookupEventsFromModuleTimetables
			with PopulateOnForm

	override val departmentPermission: Permission = Permissions.SmallGroups.ImportFromExternalSystem

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("academicYearChoices") def academicYearChoices: Seq[AcademicYear] =
		AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(2, 2)

	@ModelAttribute("command") def command(@PathVariable department: Department, user: CurrentUser): ImportSmallGroupSetsFromExternalSystemCommand =
		ImportSmallGroupSetsFromExternalSystemCommand(mandatory(department), mandatory(user))

	// Handling page to avoid extra long spinny time
	@RequestMapping
	def showForm(@ModelAttribute("command") command: ImportSmallGroupSetsFromExternalSystemCommand, errors: Errors): Mav = {
		Mav("groups/admin/groups/import_loading",
			"academicYear" -> command.academicYear
		).crumbs(Breadcrumbs.Department(command.department, command.academicYear))
	}

	@RequestMapping(method = Array(POST), params = Array("action=populate"))
	def populate(@ModelAttribute("command") command: ImportSmallGroupSetsFromExternalSystemCommand, errors: Errors): Mav = {
		command.populate()
		form(command, errors)
	}

	private def form(command: ImportSmallGroupSetsFromExternalSystemCommand, errors: Errors) =
		Mav("groups/admin/groups/import",
			"academicYear" -> command.academicYear,
			"modules" -> command.modules,
			"timetabledEvents" -> command.timetabledEvents
		).crumbs(Breadcrumbs.Department(command.department, command.academicYear))

	// Change the academic year; restarts from scratch
	@RequestMapping(method = Array(POST), params = Array("action=change-year"))
	def changeYear(@ModelAttribute("command") command: ImportSmallGroupSetsFromExternalSystemCommand, errors: Errors): Mav =
		populate(command, errors) // Run an initial populate() again

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") command: ImportSmallGroupSetsFromExternalSystemCommand, errors: Errors): Mav =
		if (errors.hasErrors) form(command, errors)
		else {
			command.apply()
			Redirect(Routes.groups.admin(command.department, command.academicYear))
		}

}
