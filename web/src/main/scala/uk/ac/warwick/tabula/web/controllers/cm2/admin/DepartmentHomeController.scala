package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.cm2.assignments.ListEnhancedAssignmentsCommand._
import uk.ac.warwick.tabula.commands.cm2.assignments.{AssignmentInfoFilters, ListEnhancedAssignmentsCommand}
import uk.ac.warwick.tabula.data.model.{Department, UserSettings}
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.data.Transactions._

abstract class AbstractDepartmentHomeController
	extends CourseworkController
		with DepartmentScopedController
		with AcademicYearScopedController
		with AutowiringModuleAndDepartmentServiceComponent
		with AutowiringUserSettingsServiceComponent
		with AutowiringMaintenanceModeServiceComponent {

	hideDeletedItems

	override val departmentPermission: Permission = ListEnhancedAssignmentsCommand.AdminPermission

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] =
		retrieveActiveDepartment(Option(department))

	def showEmptyModulesSetting: Boolean = userSettingsService.getByUserId(user.userId).forall(_.courseworkShowEmptyModules)

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear], user: CurrentUser): DepartmentCommand = {
		val academicYear = activeAcademicYear.getOrElse(AcademicYear.now())

		val command = ListEnhancedAssignmentsCommand.department(department, academicYear, user)
		command.showEmptyModules = showEmptyModulesSetting

		command
	}

	@RequestMapping(params=Array("!ajax"), headers=Array("!X-Requested-With"))
	def home(@ModelAttribute("command") command: DepartmentCommand, errors: Errors, @PathVariable department: Department): Mav =
		Mav("cm2/admin/home/department",
			"academicYear" -> command.academicYear,
			"modules" -> command.allModulesWithPermission,
			"allModuleFilters" -> AssignmentInfoFilters.allModuleFilters(command.allModulesWithPermission.sortBy(_.code)),
			"allWorkflowTypeFilters" -> AssignmentInfoFilters.allWorkflowTypeFilters,
			"allStatusFilters" -> AssignmentInfoFilters.Status.all,
			"academicYear" -> command.academicYear)
			.crumbsList(Breadcrumbs.department(department, Some(command.academicYear), active = true))
			.secondCrumbs(academicYearBreadcrumbs(command.academicYear)(Routes.cm2.admin.department(department, _)): _*)

	@RequestMapping
	def homeAjax(@ModelAttribute("command") command: DepartmentCommand, errors: Errors): Mav = {
		val modules = command.apply()

		if (command.showEmptyModules != showEmptyModulesSetting && !maintenanceModeService.enabled) {
			transactional() {
				val settings = new UserSettings()
				settings.courseworkShowEmptyModules = command.showEmptyModules
				userSettingsService.save(user, settings)
			}
		}

		Mav("cm2/admin/home/moduleList", "modules" -> modules, "academicYear" -> command.academicYear).noLayout()
	}

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}"))
class DepartmentHomeController extends AbstractDepartmentHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] =
		retrieveActiveAcademicYear(None)

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/{academicYear:\\d{4}}"))
class DepartmentHomeForYearController extends AbstractDepartmentHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
		retrieveActiveAcademicYear(Option(academicYear))

}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/assignments.xml"))
class LegacyApiRedirectController extends CourseworkController {

	@RequestMapping def redirect(@PathVariable department: Department) =
		Redirect(Routes.api.department.assignments.xml(mandatory(department)))

}